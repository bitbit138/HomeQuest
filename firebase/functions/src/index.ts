import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();

// ── Constants (must match Android Constants.kt exactly) ────────────────────

const USERS_COLLECTION = "users";
const HOUSEHOLDS_COLLECTION = "households";
const TASKS_SUB = "tasks";
const COUPONS_SUB = "coupons";
const FEED_SUB = "activity_feed";

const TASK_STATUS = {
  OPEN: "open",
  CLAIMED: "claimed",
  PENDING_VERIFICATION: "pending_verification",
  COMPLETED: "completed",
};

const FEED_TYPE = {
  TASK_COMPLETED: "task_completed",
  LEVEL_UP: "level_up",
  COUPON_PURCHASED: "coupon_purchased",
};

// XP thresholds — index = level-1, value = total XP required (Section 7.3)
const XP_THRESHOLDS = [0, 200, 500, 1000, 2000, 3500, 6000, 9000, 13000, 18000];
const MAX_LEVEL = 10;

// Feed constants
const FEED_MAX_ENTRIES = 500;
const FEED_RETENTION_DAYS = 30;

// ── 1. Task Completion Trigger ─────────────────────────────────────────────
//
// Triggered when a task document is written.
// When status transitions pending_verification → completed:
//   - Awards XP + coins to the claimer via Admin SDK batch (bypasses security rules)
//   - Handles level-up detection
//   - Writes feed entry for task_completed (and level_up if applicable)
//   - Sets completedAt timestamp
//   - If isRecurring, re-creates the task as open (V2)

export const onTaskWrite = functions.firestore
  .document(`${HOUSEHOLDS_COLLECTION}/{householdId}/${TASKS_SUB}/{taskId}`)
  .onWrite(async (change, context) => {
    const { householdId, taskId } = context.params;

    // Ignore deletes (soft-delete only — should never happen per security rules)
    if (!change.after.exists) return null;

    const newData = change.after.data()!;
    const oldData = change.before.data();

    const newStatus = newData.status as string;
    const oldStatus = oldData?.status as string | undefined;

    // Only act if the task is now COMPLETED and wasn't before
    if (newStatus !== TASK_STATUS.COMPLETED || oldStatus === TASK_STATUS.COMPLETED) {
      return null;
    }

    const claimedBy = newData.claimedBy as string | null;
    if (!claimedBy) {
      functions.logger.error(`Task ${taskId} completed but has no claimedBy uid.`);
      return null;
    }

    const xpReward = (newData.xpReward as number) ?? 0;
    const coinReward = (newData.coinReward as number) ?? 0;

    // Fetch the user to determine current level + XP for level-up detection
    const userRef = db.collection(USERS_COLLECTION).doc(claimedBy);
    const userSnap = await userRef.get();
    if (!userSnap.exists) {
      functions.logger.error(`User ${claimedBy} not found for task completion.`);
      return null;
    }

    const userData = userSnap.data()!;
    const currentLevel = (userData.level as number) ?? 1;
    const currentXp = (userData.currentXp as number) ?? 0;
    const displayName = (userData.displayName as string) ?? "A member";
    const newXp = currentXp + xpReward;

    // Level-up detection
    const newLevel = computeLevel(newXp);
    const didLevelUp = newLevel > currentLevel && newLevel <= MAX_LEVEL;

    // Build batch
    const batch = db.batch();

    // Increment XP + coins on user (Admin SDK — bypasses security rule block)
    const userUpdates: Record<string, admin.firestore.FieldValue | number> = {
      currentXp: admin.firestore.FieldValue.increment(xpReward),
      coinBalance: admin.firestore.FieldValue.increment(coinReward),
    };
    if (didLevelUp) {
      userUpdates.level = newLevel;
    }
    batch.update(userRef, userUpdates);

    // Set completedAt on the task
    const taskRef = db
      .collection(HOUSEHOLDS_COLLECTION)
      .doc(householdId)
      .collection(TASKS_SUB)
      .doc(taskId);
    batch.update(taskRef, {
      completedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Write task_completed feed entry
    const feedRef = db
      .collection(HOUSEHOLDS_COLLECTION)
      .doc(householdId)
      .collection(FEED_SUB)
      .doc();
    batch.set(feedRef, {
      entryId: feedRef.id,
      type: FEED_TYPE.TASK_COMPLETED,
      actorId: claimedBy,
      actorName: displayName,
      message: `${displayName} completed "${newData.title}" and earned +${xpReward} XP! ✅`,
      relatedEntityId: taskId,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Write level_up feed entry if applicable
    if (didLevelUp) {
      const levelFeedRef = db
        .collection(HOUSEHOLDS_COLLECTION)
        .doc(householdId)
        .collection(FEED_SUB)
        .doc();
      batch.set(levelFeedRef, {
        entryId: levelFeedRef.id,
        type: FEED_TYPE.LEVEL_UP,
        actorId: claimedBy,
        actorName: displayName,
        message: `🆙 ${displayName} leveled up to Level ${newLevel}!`,
        relatedEntityId: claimedBy,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
      });
    }

    // Re-create recurring task (V2 feature — isRecurring == true)
    if (newData.isRecurring === true) {
      const recurringRef = db
        .collection(HOUSEHOLDS_COLLECTION)
        .doc(householdId)
        .collection(TASKS_SUB)
        .doc();
      batch.set(recurringRef, {
        ...newData,
        taskId: recurringRef.id,
        status: TASK_STATUS.OPEN,
        claimedBy: null,
        proofImageUrl: null,
        completedAt: null,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    }

    await batch.commit();
    functions.logger.info(
      `Task ${taskId} completed by ${claimedBy}. ` +
      `XP: +${xpReward}, Coins: +${coinReward}, LevelUp: ${didLevelUp}`
    );

    // Send FCM push to the household (best-effort, non-blocking)
    await sendTaskCompletionNotification(householdId, displayName, newData.title as string, claimedBy);

    return null;
  });

// ── 2. Coupon Purchase (Callable) ───────────────────────────────────────────
//
// Atomically: validate balance >= cost, debit coins, set buyerId, write feed.
// Required because Firestore rules block client writes to coinBalance.

export const purchaseCoupon = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be signed in.");
  }
  const uid = context.auth.uid;
  const { householdId, couponId } = data as { householdId?: string; couponId?: string };
  if (!householdId || !couponId) {
    throw new functions.https.HttpsError("invalid-argument", "householdId and couponId required.");
  }

  const buyerRef = db.collection(USERS_COLLECTION).doc(uid);
  const buyerSnap = await buyerRef.get();
  if (!buyerSnap.exists) {
    throw new functions.https.HttpsError("not-found", "User not found.");
  }
  const userHouseholdId = buyerSnap.data()?.householdId as string | undefined;
  if (userHouseholdId !== householdId) {
    throw new functions.https.HttpsError("permission-denied", "User is not a member of this household.");
  }
  const couponRef = db
    .collection(HOUSEHOLDS_COLLECTION)
    .doc(householdId)
    .collection(COUPONS_SUB)
    .doc(couponId);
  const feedRef = db
    .collection(HOUSEHOLDS_COLLECTION)
    .doc(householdId)
    .collection(FEED_SUB)
    .doc();

  const transaction = db.runTransaction(async (tx) => {
    const userSnap = await tx.get(buyerRef);
    const couponSnap = await tx.get(couponRef);

    if (!couponSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Coupon not found.");
    }

    const userData = userSnap.data()!;
    const couponData = couponSnap.data()!;
    const balance = (userData.coinBalance as number) ?? 0;
    const cost = (couponData.cost as number) ?? 0;
    const buyerId = couponData.buyerId as string | null;
    const sellerId = couponData.sellerId as string | null;
    const displayName = (userData.displayName as string) ?? "A member";

    if (sellerId === uid) {
      throw new functions.https.HttpsError("failed-precondition", "You cannot purchase your own reward.");
    }
    if (buyerId != null) {
      throw new functions.https.HttpsError("failed-precondition", "This coupon has already been purchased.");
    }
    if (balance < cost) {
      throw new functions.https.HttpsError("failed-precondition", `Insufficient coins. You need ${cost} but have ${balance}.`);
    }

    tx.update(buyerRef, { coinBalance: balance - cost });
    tx.update(couponRef, {
      buyerId: uid,
      purchasedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    tx.set(feedRef, {
      entryId: feedRef.id,
      type: FEED_TYPE.COUPON_PURCHASED,
      actorId: uid,
      actorName: displayName,
      message: `${displayName} purchased "${couponData.title}" 🎟️`,
      relatedEntityId: couponId,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
    });
  });

  await transaction;
  return { success: true };
});

// ── 3. Feed Pruning Scheduler ──────────────────────────────────────────────
//
// Runs daily at 03:00 UTC.
// Deletes feed entries older than FEED_RETENTION_DAYS.
// Caps each household feed at FEED_MAX_ENTRIES total.
// (Risk Register: LOW — feed storage growth mitigation, Section 11)

export const pruneFeedDaily = functions.pubsub
  .schedule("0 3 * * *")
  .timeZone("UTC")
  .onRun(async () => {
    const cutoff = admin.firestore.Timestamp.fromDate(
      new Date(Date.now() - FEED_RETENTION_DAYS * 24 * 60 * 60 * 1000)
    );

    const householdsSnap = await db.collection(HOUSEHOLDS_COLLECTION).get();
    const prunePromises = householdsSnap.docs.map(async (householdDoc) => {
      const feedRef = householdDoc.ref.collection(FEED_SUB);

      // Delete entries older than retention window
      const oldEntries = await feedRef.where("timestamp", "<", cutoff).get();
      for (const doc of oldEntries.docs) {
        await doc.ref.delete();
      }

      // Cap at MAX_ENTRIES — delete oldest beyond the cap
      const allEntries = await feedRef.orderBy("timestamp", "desc").get();
      if (allEntries.size > FEED_MAX_ENTRIES) {
        const toDelete = allEntries.docs.slice(FEED_MAX_ENTRIES);
        for (const doc of toDelete) {
          await doc.ref.delete();
        }
      }

      functions.logger.info(
        `Pruned ${oldEntries.size} old feed entries from household ${householdDoc.id}`
      );
    });

    await Promise.all(prunePromises);
    return null;
  });

// ── 4. FCM: Task Completion Notification ──────────────────────────────────
//
// Notifies all household members when a task is completed.
// Skips the completing user themselves. Stale tokens are silently ignored.
// (Risk Register: MED — FCM token staleness handled at client on app open)

async function sendTaskCompletionNotification(
  householdId: string,
  actorName: string,
  taskTitle: string,
  actorUid: string
): Promise<void> {
  try {
    const householdSnap = await db
      .collection(HOUSEHOLDS_COLLECTION)
      .doc(householdId)
      .get();

    if (!householdSnap.exists) return;
    const memberUids = (householdSnap.data()!.members as string[]) ?? [];

    // Fan out to get all FCM tokens, excluding the actor
    const tokenFetches = memberUids
      .filter((uid) => uid !== actorUid)
      .map((uid) =>
        db.collection(USERS_COLLECTION).doc(uid).get().then((snap: admin.firestore.DocumentSnapshot) => {
          return snap.data()?.fcmToken as string | undefined;
        })
      );

    const tokens = (await Promise.all(tokenFetches)).filter(Boolean) as string[];
    if (tokens.length === 0) return;

    const message: admin.messaging.MulticastMessage = {
      tokens,
      notification: {
        title: "Quest Complete! 🎉",
        body: `${actorName} just completed "${taskTitle}"`,
      },
      android: {
        priority: "normal",
      },
    };

    const response = await admin.messaging().sendEachForMulticast(message);
    functions.logger.info(
      `FCM sent: ${response.successCount} success, ${response.failureCount} failures`
    );
  } catch (err) {
    // Non-fatal — push failure should not break the completion flow
    functions.logger.warn("FCM notification failed (non-fatal):", err);
  }
}

// ── Utility: Level computation ─────────────────────────────────────────────

function computeLevel(totalXp: number): number {
  let level = 1;
  for (let i = XP_THRESHOLDS.length - 1; i >= 0; i--) {
    if (totalXp >= XP_THRESHOLDS[i]) {
      level = i + 1;
      break;
    }
  }
  return Math.min(level, MAX_LEVEL);
}
