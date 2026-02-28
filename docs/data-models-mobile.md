# Data Models - Android Mobile App

**Project:** HomeQuest  
**Part:** Mobile (Android)  
**Generated:** 2026-02-24  
**Scan Level:** Quick (pattern-based)

---

## Overview

HomeQuest uses Firebase Firestore as its primary database with a hierarchical document structure. The mobile app defines Kotlin data classes that mirror the Firestore schema.

---

## Core Data Models

### 1. User

**Firestore Path:** `/users/{userId}`  
**Model File:** `app/src/main/java/dev/tombit/homequest/model/User.kt`

**Purpose:** Represents a user profile with gamification metrics

**Key Fields:**
- `userId` (String) - Matches Firebase Auth UID
- `displayName` (String) - User's display name
- `email` (String) - User email
- `householdId` (String) - Reference to household
- `coinBalance` (Int) - Virtual currency balance
- `currentXp` (Int) - Experience points
- `level` (Int) - User level (derived from XP)
- `avatarUrl` (String?) - Profile picture URL
- `fcmToken` (String?) - Firebase Cloud Messaging token

**Security Constraints:**
- `coinBalance`, `currentXp`, `level` are **read-only from client** (Cloud Function only)
- Users can only update their own profile
- Cannot delete user documents

---

### 2. Household

**Firestore Path:** `/households/{householdId}`  
**Model File:** `app/src/main/java/dev/tombit/homequest/model/Household.kt`

**Purpose:** Represents a family/group that shares quests and rewards

**Key Fields:**
- `householdId` (String) - Document ID
- `name` (String) - Household name
- `inviteCode` (String) - 6-character unique code for joining
- `members` (List<String>) - Array of user IDs
- `createdAt` (Timestamp) - Creation timestamp
- `createdBy` (String) - Creator user ID

**Security Constraints:**
- Any authenticated user can read (needed for invite code lookup)
- Members can update household
- Non-members can add themselves via invite code
- Cannot delete households

---

### 3. Task (Quest)

**Firestore Path:** `/households/{householdId}/tasks/{taskId}`  
**Model File:** `app/src/main/java/dev/tombit/homequest/model/Task.kt`

**Purpose:** Represents a household chore/quest with rewards

**Key Fields:**
- `taskId` (String) - Document ID
- `title` (String) - Quest title (max 80 chars)
- `description` (String?) - Optional description
- `xpReward` (Int) - XP reward (10-500)
- `coinReward` (Int) - Coin reward (5-200)
- `status` (String) - Lifecycle state
- `createdBy` (String) - Creator user ID
- `claimedBy` (String?) - User who claimed the quest
- `deadline` (Timestamp?) - Optional deadline
- `proofImageUrl` (String?) - Photo proof URL
- `isRecurring` (Boolean) - Repeating quest flag
- `createdAt` (Timestamp)

**Status Lifecycle:**
1. `open` - Available to claim
2. `claimed` - Claimed by a user
3. `pending_verification` - Proof submitted, awaiting approval
4. `completed` - Approved and rewards granted

**Security Constraints:**
- Household members can read, create, update
- Cannot delete tasks
- Status transitions are forward-only
- `proofImageUrl` required before completion

**Firestore Indexes:**
- Composite: `status` + `deadline` (ASC)
- Composite: `status` + `createdAt` (DESC)
- Composite: `claimedBy` + `status` (ASC)

---

### 4. Coupon (Reward)

**Firestore Path:** `/households/{householdId}/coupons/{couponId}`  
**Model File:** `app/src/main/java/dev/tombit/homequest/model/Coupon.kt`

**Purpose:** Represents a purchasable reward in the household marketplace

**Key Fields:**
- `couponId` (String) - Document ID
- `title` (String) - Reward title (max 60 chars)
- `cost` (Int) - Coin cost
- `sellerId` (String) - User who created the reward
- `buyerId` (String?) - User who purchased (null = available)
- `isRedeemed` (Boolean) - Redemption status
- `createdAt` (Timestamp)

**Purchase Flow:**
1. Available: `buyerId == null`
2. Purchased: `buyerId` set, coins debited
3. Redeemed: `isRedeemed = true` (seller marks)

**Security Constraints:**
- Household members can read, create, update
- Cannot delete coupons
- Concurrent purchase protection via Cloud Function transaction

**Firestore Indexes:**
- Composite: `buyerId` + `createdAt` (DESC)
- Composite: `sellerId` + `isRedeemed` (ASC)

---

### 5. FeedItem (Activity Feed)

**Firestore Path:** `/households/{householdId}/activity_feed/{entryId}`  
**Model File:** `app/src/main/java/dev/tombit/homequest/model/FeedItem.kt`

**Purpose:** Real-time activity log for household events

**Key Fields:**
- `entryId` (String) - Document ID
- `type` (String) - Event type (quest_completed, level_up, coupon_purchased, etc.)
- `userId` (String) - User who triggered the event
- `userName` (String) - Display name (denormalized)
- `message` (String) - Human-readable event description
- `timestamp` (Timestamp)
- `metadata` (Map?) - Additional event-specific data

**Event Types:**
- `quest_completed` - User completed a quest
- `level_up` - User leveled up
- `coupon_purchased` - Reward purchased
- `coupon_redeemed` - Reward redeemed

**Security Constraints:**
- Household members can read and create
- **Cannot update or delete** (immutable feed)
- Written atomically by Cloud Functions with reward events

**Query Pattern:**
- Paginated: `limit(20)` with `startAfterDocument()` for infinite scroll
- Ordered by `timestamp` DESC

---

## Firestore Security Rules

**Location:** `firebase/firestore/firestore.rules`

**Key Security Principles:**

1. **Protected Fields:** `coinBalance`, `currentXp`, `level` cannot be modified by clients
2. **Household Isolation:** Users can only access data from their household
3. **No Deletions:** All collections prevent client-side deletions
4. **Immutable Feed:** Activity feed entries cannot be updated or deleted
5. **Proof Validation:** Tasks require `proofImageUrl` before completion
6. **Invite System:** Non-members can read households to look up invite codes

---

## Storage Structure

**Location:** Firebase Storage  
**Rules:** `firebase/storage/storage.rules`

### Proof Images
- **Path:** `proofs/{householdId}/{taskId}.jpg`
- **Format:** JPEG only
- **Max Size:** 200 KB (enforced by storage rules)
- **Compression:** Applied client-side by `ImageCompressor` utility

### Avatar Images
- **Path:** `avatars/{userId}.jpg`
- **Format:** JPEG only
- **Max Size:** 200 KB
- **Access:** User can only write their own avatar

---

## Data Architecture Notes

- **Builder Pattern:** All model classes use private constructor + inner `Builder` class (per L08 coding standards)
- **Offline Support:** Firestore offline persistence enabled in `App.kt`
- **Real-time Listeners:** Used for feed updates, stored as `ListenerRegistration`, removed in `onPause()`
- **Denormalization:** User names stored in feed items for performance
- **No Client-Side Deletions:** All data is append-only from client perspective
- **Cloud Function Authority:** Sensitive operations (rewards, balance updates) handled server-side

---

## Related Documentation

- [Architecture - Mobile](./architecture-mobile.md) _(To be generated)_
- [Architecture - Backend](./architecture-backend.md) _(To be generated)_
- [Integration Architecture](./integration-architecture.md) _(To be generated)_
- [QA Checklist](./QA_CHECKLIST.md) - Existing validation checklist
