# Architecture - Firebase Backend

**Project:** HomeQuest  
**Part:** Backend (Firebase)  
**Generated:** 2026-02-24  
**Version:** 1.0.0

---

## Executive Summary

HomeQuest's backend is a serverless architecture built entirely on Firebase services. Cloud Functions (TypeScript/Node.js 20) handle server-side business logic triggered by Firestore document changes and scheduled events. The backend enforces critical security constraints (protected fields, household isolation) and manages sensitive operations like reward distribution, XP/coin balance updates, and activity feed generation.

**Key Characteristics:**
- **Architecture Pattern:** Serverless event-driven (Cloud Functions)
- **Language:** TypeScript 5.0.4
- **Runtime:** Node.js 20
- **Database:** Firestore (NoSQL document database)
- **Storage:** Firebase Storage (object storage)
- **Auth:** Firebase Authentication
- **Messaging:** Firebase Cloud Messaging (FCM)

---

## Technology Stack

| Category | Technology | Version | Purpose |
|----------|-----------|---------|---------|
| **Platform** | Firebase Cloud Functions | 5.0.0 | Serverless compute |
| **Runtime** | Node.js | 20 | JavaScript runtime |
| **Language** | TypeScript | 5.0.4 | Type-safe JavaScript |
| **Target** | ES2017 | N/A | Modern JavaScript features |
| **Admin SDK** | firebase-admin | 12.0.0 | Server-side Firebase operations |
| **Functions SDK** | firebase-functions | 5.0.0 | Cloud Functions framework |
| **Database** | Firestore | Via Admin SDK | NoSQL document database |
| **Storage** | Firebase Storage | Via Admin SDK | Object storage |
| **Auth** | Firebase Auth | Via Admin SDK | User authentication |
| **Messaging** | FCM | Via Admin SDK | Push notifications |

---

## Architecture Pattern

### Serverless Event-Driven

**Trigger Types:**

1. **Firestore Triggers**
   - Document onCreate, onUpdate, onDelete
   - Example: Task status change → Reward approval function

2. **Scheduled Functions**
   - Cron-based execution
   - Example: Daily feed pruning at 03:00 UTC

3. **HTTPS Callable** (if implemented)
   - Direct invocation from mobile app
   - Example: Coupon purchase transaction

4. **Auth Triggers** (potential)
   - User creation, deletion
   - Example: Initialize user document on sign-up

---

## Cloud Functions

**Entry Point:** `firebase/functions/src/index.ts`

### Expected Functions (Based on Architecture)

#### 1. Reward Approval Function
```typescript
export const approveTaskCompletion = functions.firestore
  .document('households/{householdId}/tasks/{taskId}')
  .onUpdate(async (change, context) => {
    // Triggered when task status changes
    // If status == "pending_verification":
    //   - Validate proofImageUrl exists
    //   - Award XP and coins to claimedBy user
    //   - Check for level-up
    //   - Update task status to "completed"
    //   - Write activity_feed entry
  });
```

**Responsibilities:**
- Validate proof image exists
- Award XP: Update `User.currentXp`
- Award coins: Update `User.coinBalance`
- Level-up logic: Update `User.level` if XP threshold crossed
- Update task status to `completed`
- Write activity feed entries (quest_completed, level_up)

---

#### 2. Feed Pruning Function
```typescript
export const pruneFeed = functions.pubsub
  .schedule('0 3 * * *')  // Daily at 03:00 UTC
  .onRun(async (context) => {
    // Delete feed entries older than 30 days
    // Batch delete in chunks of 500
  });
```

**Responsibilities:**
- Query old feed entries (> 30 days)
- Batch delete to maintain performance
- Run daily to prevent unbounded growth

---

#### 3. Coupon Purchase Transaction (Potential)
```typescript
export const purchaseCoupon = functions.https
  .onCall(async (data, context) => {
    // Atomic transaction:
    //   - Check user has sufficient coins
    //   - Debit coinBalance
    //   - Set coupon.buyerId
    //   - Write activity_feed entry
  });
```

**Responsibilities:**
- Verify sufficient coin balance
- Atomic transaction to prevent race conditions
- Debit coins and assign coupon
- Write activity feed entry

---

## Data Architecture

### Firestore Collections

**Top-Level:**
- `/users/{userId}` - User profiles
- `/households/{householdId}` - Household groups

**Subcollections:**
- `/households/{householdId}/tasks/{taskId}` - Quests
- `/households/{householdId}/coupons/{couponId}` - Rewards
- `/households/{householdId}/activity_feed/{entryId}` - Activity log

### Server-Controlled Fields

**User Document:**
- `coinBalance` - Only Cloud Functions can write
- `currentXp` - Only Cloud Functions can write
- `level` - Only Cloud Functions can write

**Enforcement:** Firestore security rules reject client writes to these fields.

### Composite Indexes

**File:** `firebase/firestore/firestore.indexes.json`

**Indexes:**
1. `tasks`: `status` + `deadline` (quest list)
2. `tasks`: `status` + `createdAt` (recent quests)
3. `tasks`: `claimedBy` + `status` (user's quests)
4. `coupons`: `buyerId` + `createdAt` (owned coupons)
5. `coupons`: `sellerId` + `isRedeemed` (seller's coupons)

---

## Security Architecture

### Firestore Security Rules

**File:** `firebase/firestore/firestore.rules`

**Key Rules:**

1. **Protected Fields:**
```javascript
allow update: if request.auth.uid == userId
  && !(request.resource.data.diff(resource.data).affectedKeys()
       .hasAny(['coinBalance', 'currentXp', 'level']));
```

2. **Household Isolation:**
```javascript
function isMember(householdId) {
  return request.auth.uid in 
    get(/databases/$(database)/documents/households/$(householdId)).data.members;
}
```

3. **No Deletions:**
```javascript
allow delete: if false;  // All collections
```

4. **Immutable Feed:**
```javascript
// activity_feed
allow read, create: if isMember(householdId);
allow update, delete: if false;
```

### Storage Security Rules

**File:** `firebase/storage/storage.rules`

**Proof Images:**
```javascript
match /proofs/{householdId}/{fileName} {
  allow read: if request.auth != null;
  allow write: if request.auth != null
    && request.resource.contentType == 'image/jpeg'
    && request.resource.size <= 200 * 1024;  // 200KB
}
```

**Avatar Images:**
```javascript
match /avatars/{userId} {
  allow read: if request.auth != null;
  allow write: if request.auth.uid == userId
    && request.resource.contentType == 'image/jpeg'
    && request.resource.size <= 200 * 1024;
}
```

---

## Business Logic

### Reward Distribution
**Trigger:** Task status → `pending_verification`  
**Logic:**
1. Validate `proofImageUrl` is set
2. Read user document
3. Calculate new XP: `currentXp + task.xpReward`
4. Calculate new coins: `coinBalance + task.coinReward`
5. Check level-up: If `currentXp >= levelThreshold`, increment `level`
6. Atomic update: User fields + Task status
7. Write feed entries (quest_completed, level_up if applicable)

### Level-Up System
**XP Thresholds (example):**
- Level 1: 0 XP
- Level 2: 100 XP
- Level 3: 250 XP
- Level 4: 500 XP
- (Progressive scaling)

### Coupon Purchase
**Logic:**
1. Verify `coupon.buyerId == null` (available)
2. Verify `user.coinBalance >= coupon.cost`
3. Transaction:
   - Debit `user.coinBalance`
   - Set `coupon.buyerId = user.userId`
4. Write feed entry (coupon_purchased)

---

## Performance Architecture

### Function Optimization
- **Cold Starts:** Minimize by keeping functions lightweight
- **Timeouts:** Configure appropriately (default 60s)
- **Memory:** Allocate based on function needs

### Database Optimization
- **Composite Indexes:** All complex queries indexed
- **Batch Operations:** Use for multiple writes
- **Pagination:** Limit query results

### Scheduled Maintenance
- **Feed Pruning:** Daily at 03:00 UTC
- **Batch Size:** 500 documents per batch
- **Purpose:** Prevent unbounded collection growth

---

## Error Handling

### Function Error Handling
```typescript
try {
  // Function logic
} catch (error) {
  console.error('Function error:', error);
  // Log to Firebase Console
  throw new functions.https.HttpsError('internal', 'Operation failed');
}
```

### Transaction Failures
- Automatic rollback on transaction failure
- Retry logic for transient errors
- Error messages logged to Firebase Console

---

## Monitoring and Logging

### Cloud Functions Logs
```bash
firebase functions:log
```

### Firebase Console
- **Functions:** Invocation count, errors, execution time
- **Firestore:** Document reads/writes, query performance
- **Storage:** Upload/download metrics
- **Auth:** User sign-ins, failures

---

## Deployment Process

### Deploy All Services
```bash
firebase deploy
```

### Deploy Functions Only
```bash
firebase deploy --only functions
```

### Deploy Security Rules
```bash
firebase deploy --only firestore:rules,storage
```

### Deploy Indexes
```bash
firebase deploy --only firestore:indexes
```

---

## Source Tree

```
firebase/
├── functions/
│   ├── src/
│   │   └── index.ts          # Cloud Functions entry point
│   ├── lib/                  # Compiled JavaScript (gitignored)
│   ├── node_modules/         # Dependencies (gitignored)
│   ├── package.json          # Node dependencies
│   └── tsconfig.json         # TypeScript configuration
├── firestore/
│   ├── firestore.rules       # Database security rules
│   └── firestore.indexes.json # Composite indexes
└── storage/
    └── storage.rules         # Storage security rules
```

---

## Related Documentation

- [Project Overview](./project-overview.md) _(To be generated)_
- [Architecture - Mobile](./architecture-mobile.md)
- [Integration Architecture](./integration-architecture.md)
- [Data Models - Mobile](./data-models-mobile.md) - Firestore schema reference
- [Development Guide - Backend](./development-guide-backend.md)
- [QA Checklist](./QA_CHECKLIST.md) - Security validation scenarios
