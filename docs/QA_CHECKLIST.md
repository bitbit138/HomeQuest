# HomeQuest — Pre-Launch QA Checklist
**Version 1.0 | Based on Architecture Section 11**

---

## 11.1 Coding Standards (Professor's Conventions)

- [ ] All XML view IDs follow `{screen}_{WIDGET}_{descriptor}` convention
- [ ] Every Activity has `findViews()` and `initViews()` called from `onCreate()`
- [ ] ViewBinding enabled and used in all Activities and Adapters
- [ ] All utility singletons use thread-safe double-check locking with `@Volatile`
- [ ] All singletons initialized exclusively in `App.kt`, not in Activities
- [ ] `WeakReference<Context>` used in all singleton classes — no direct Context storage
- [ ] All data model classes use `private constructor` + inner `Builder` pattern
- [ ] All inter-component callbacks use named interfaces in `interfaces/` package
- [ ] `Constants.kt` uses nested object groups — no hardcoded strings in Kotlin files
- [ ] All Toast calls route through `SignalManager.getInstance().toast()`
- [ ] All image loading routes through `ImageLoader.getInstance().loadImage()`
- [ ] `SplashScreenActivity` implemented with Lottie animation before `MainActivity`
- [ ] Firestore listener stored as `ListenerRegistration`; removed in `onPause()`

---

## 11.2 Data Layer

- [ ] User document ID matches Firebase Auth UID — verified at sign-up
- [ ] `coinBalance`, `currentXp`, `level` never written directly by client (Cloud Function only)
- [ ] Task status transitions only forward (open → claimed → pending_verification → completed)
- [ ] `proofImageUrl` non-null before task advances to `completed`
- [ ] Activity feed entry written atomically with every reward event
- [ ] FCM token refreshed in `App.onCreate()`
- [ ] Invite code uniqueness checked before household creation (with retry on collision)

---

## 11.3 Storage & Security

- [ ] JPEG compression applied: max 1280px, 80% quality, < 200 KB
- [ ] Fallback compression at 60% if primary exceeds 200 KB
- [ ] Storage path convention: `proofs/{householdId}/{taskId}.jpg`
- [ ] Firestore security rules deployed and tested with Firebase Emulator Suite
- [ ] Attempt direct write to `coinBalance` from client → must be REJECTED
- [ ] Attempt to read another household's data → must be REJECTED
- [ ] Attempt to delete a task from client → must be REJECTED
- [ ] Attempt to update/delete a feed entry from client → must be REJECTED

---

## 11.4 Performance & Offline

- [ ] Composite indexes deployed (`firestore.indexes.json` applied to project)
- [ ] Firestore offline persistence enabled in `App` class
- [ ] Feed queries paginated: `limit(20)` with `startAfterDocument()`
- [ ] Feed pruning Cloud Function scheduled (daily at 03:00 UTC)
- [ ] Leaderboard cached for 60 seconds (no re-fetch within cache window)

---

## Manual QA Test Scenarios

### Auth Flow
- [ ] New user sign-up → creates household → lands on MainActivity
- [ ] New user sign-up → joins household via invite code → lands on MainActivity
- [ ] Existing user signs in → lands on MainActivity
- [ ] Unauthenticated app launch → routes to LoginActivity (not MainActivity)
- [ ] Sign out → clears SharedPreferences → routes to LoginActivity

### Quest Flow
- [ ] Quest list shows only `open` tasks ordered by deadline
- [ ] User can claim an open quest → status becomes `claimed`
- [ ] User cannot claim a quest already claimed by someone else
- [ ] Claimed quest shows upload proof button
- [ ] Photo is compressed: upload 5MB image → verify result < 200KB in Storage bucket
- [ ] Submit proof → status becomes `pending_verification`, proofImageUrl is set
- [ ] Cloud Function: approve → status becomes `completed`, XP/coins awarded, feed entry written
- [ ] Level-up: accumulate enough XP → level field increments, `level_up` feed entry written

### Rewards Flow
- [ ] Available coupons displayed (buyerId == null)
- [ ] Purchase coupon: coin balance debits, coupon shows as owned
- [ ] Simultaneous purchase attempt (two devices) → only one succeeds; other gets "already purchased"
- [ ] Seller can mark owned coupon as redeemed
- [ ] Insufficient coins → purchase rejected with appropriate toast

### Feed & Dashboard
- [ ] Real-time feed updates when teammate completes quest (no manual refresh)
- [ ] App backgrounded → feed listener detaches (check Logcat / Firebase console reads drop)
- [ ] App foregrounded → feed listener re-attaches
- [ ] Feed limited to 20 items initially

### Offline
- [ ] Disable network → app still loads previously cached data
- [ ] Re-enable network → data syncs without crash

### Lifecycle
- [ ] Rotate screen on MainActivity → no duplicate listeners registered
- [ ] Background app mid-upload → upload continues, result handled on return

---

## Security Audit Checklist

| Test | Expected Result | Pass/Fail |
|------|----------------|-----------|
| Client writes `coinBalance` | Firestore rejects (security rule) | |
| Client writes `currentXp` | Firestore rejects | |
| Client writes `level` | Firestore rejects | |
| User reads another household's tasks | Firestore rejects | |
| User deletes a task | Firestore rejects | |
| User updates a feed entry | Firestore rejects | |
| Upload image > 200KB | Storage rejects | |
| Upload non-JPEG to proofs/ | Storage rejects | |

---

## Production Deploy Checklist

- [ ] `google-services.json` replaced with production project config
- [ ] Firebase security rules deployed to production project
- [ ] Firestore composite indexes deployed to production project
- [ ] Cloud Functions deployed to production project
- [ ] Feed pruning Cloud Function verified active in Firebase Console
- [ ] Firebase Crashlytics enabled
- [ ] Signed release APK/AAB generated with production keystore
- [ ] ProGuard/R8 rules verified (Glide, Firebase, Gson, Lottie rules present)
- [ ] Version code and version name set correctly in `build.gradle.kts`
- [ ] Internal testing track on Google Play Console published
- [ ] Staged rollout plan: 10% → 50% → 100%
