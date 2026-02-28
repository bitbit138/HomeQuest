# Development Guide - Firebase Backend

**Project:** HomeQuest  
**Part:** Backend (Firebase)  
**Generated:** 2026-02-24

---

## Prerequisites

### Required Software
- **Node.js:** Version 20 (specified in `firebase/functions/package.json`)
- **npm:** Comes with Node.js
- **Firebase CLI:** For deployment and emulator
- **TypeScript:** 5.0.4 (installed as dev dependency)

### Firebase Project
- Active Firebase project with:
  - Cloud Functions enabled
  - Firestore database created
  - Firebase Storage enabled
  - Firebase Authentication enabled
  - Billing account linked (required for Cloud Functions)

---

## Installation

### 1. Install Firebase CLI
```bash
npm install -g firebase-tools
```

### 2. Login to Firebase
```bash
firebase login
```

### 3. Install Function Dependencies
```bash
cd firebase/functions
npm install
```

---

## Environment Setup

### Firebase Project Configuration

**File:** `firebase.json` (root)
```json
{
  "functions": [{
    "source": "firebase/functions",
    "codebase": "default"
  }],
  "firestore": {
    "rules": "firebase/firestore/firestore.rules",
    "indexes": "firebase/firestore/firestore.indexes.json"
  },
  "storage": {
    "rules": "firebase/storage/storage.rules"
  }
}
```

### TypeScript Configuration

**File:** `firebase/functions/tsconfig.json`
- **Target:** ES2017
- **Module:** CommonJS
- **Output:** `lib/` directory
- **Strict Mode:** Enabled

---

## Build Commands

### Compile TypeScript
```bash
cd firebase/functions
npm run build
```
**Output:** `firebase/functions/lib/index.js`

### Watch Mode (Auto-compile)
```bash
cd firebase/functions
tsc --watch
```

---

## Run Commands

### Start Firebase Emulators
```bash
firebase emulators:start
```

**Emulator Ports:**
- **Auth:** 9099
- **Functions:** 5001
- **Firestore:** 8080
- **Storage:** 9199
- **UI Dashboard:** 4000

**Access Emulator UI:** http://localhost:4000

### Run Functions Locally
```bash
cd firebase/functions
npm run serve
```
This builds TypeScript and starts the Functions emulator.

### Functions Shell (Interactive)
```bash
cd firebase/functions
npm run shell
```
Allows direct function invocation for testing.

---

## Deployment

### Deploy All Firebase Services
```bash
firebase deploy
```

### Deploy Functions Only
```bash
firebase deploy --only functions
```
or
```bash
cd firebase/functions
npm run deploy
```

### Deploy Firestore Rules
```bash
firebase deploy --only firestore:rules
```

### Deploy Firestore Indexes
```bash
firebase deploy --only firestore:indexes
```

### Deploy Storage Rules
```bash
firebase deploy --only storage
```

---

## Testing

### Run Unit Tests
```bash
cd firebase/functions
npm test
```
**Note:** No test files detected in quick scan.

### Test with Emulators
1. Start emulators: `firebase emulators:start`
2. Point mobile app to emulator endpoints
3. Trigger functions via mobile app actions
4. Monitor logs in emulator UI (http://localhost:4000)

### View Function Logs (Production)
```bash
firebase functions:log
```
or
```bash
cd firebase/functions
npm run logs
```

---

## Cloud Functions Structure

**Entry Point:** `firebase/functions/src/index.ts`

### Expected Functions (Based on Architecture)

#### 1. Reward Approval Function
**Trigger:** Firestore document update on `/households/{householdId}/tasks/{taskId}`  
**Condition:** Task status changes to `pending_verification`  
**Actions:**
- Validate proof image exists
- Update task status to `completed`
- Award XP and coins to user (update `currentXp`, `coinBalance`)
- Check for level-up (update `level` if threshold crossed)
- Write activity feed entry (quest_completed, level_up if applicable)

#### 2. Feed Pruning Function
**Trigger:** Scheduled (daily at 03:00 UTC)  
**Actions:**
- Query old feed entries (> 30 days)
- Batch delete in chunks
- Maintain performance

#### 3. Coupon Purchase Transaction
**Trigger:** HTTPS callable or Firestore trigger  
**Actions:**
- Verify user has sufficient coins
- Atomic transaction: debit coins, set coupon buyerId
- Prevent concurrent purchase conflicts
- Write activity feed entry

---

## Security Rules Development

### Firestore Rules

**File:** `firebase/firestore/firestore.rules`

**Key Security Principles:**
1. Protected fields: `coinBalance`, `currentXp`, `level` (Cloud Function only)
2. Household isolation: Users only access their household data
3. No client deletions: All collections prevent `delete`
4. Immutable feed: Activity feed entries cannot be updated/deleted

**Test Rules:**
```bash
firebase emulators:start --only firestore
# Then run mobile app against emulator
```

### Storage Rules

**File:** `firebase/storage/storage.rules`

**Enforced Constraints:**
- JPEG format only
- 200KB max file size
- Path-based access control

**Test Rules:**
```bash
firebase emulators:start --only storage
# Upload images via mobile app
```

---

## Database Indexes

**File:** `firebase/firestore/firestore.indexes.json`

**Composite Indexes:**
1. Tasks: `status` + `deadline` (for quest list queries)
2. Tasks: `status` + `createdAt` (for recent quests)
3. Tasks: `claimedBy` + `status` (for user's claimed quests)
4. Coupons: `buyerId` + `createdAt` (for owned coupons)
5. Coupons: `sellerId` + `isRedeemed` (for seller's coupons)

**Deploy Indexes:**
```bash
firebase deploy --only firestore:indexes
```

---

## Common Development Tasks

### Add New Cloud Function
1. Open `firebase/functions/src/index.ts`
2. Define function using Firebase Functions SDK:
```typescript
export const myFunction = functions.firestore
  .document('path/{docId}')
  .onCreate(async (snap, context) => {
    // Function logic
  });
```
3. Build: `npm run build`
4. Test locally: `firebase emulators:start`
5. Deploy: `firebase deploy --only functions`

### Update Firestore Rules
1. Edit `firebase/firestore/firestore.rules`
2. Test with emulator: `firebase emulators:start --only firestore`
3. Validate with mobile app test scenarios
4. Deploy: `firebase deploy --only firestore:rules`

### Add New Composite Index
1. Edit `firebase/firestore/firestore.indexes.json`
2. Add index definition
3. Deploy: `firebase deploy --only firestore:indexes`
4. Wait for index build (check Firebase Console)

---

## Debugging

### View Function Logs (Local)
Logs appear in terminal when running `firebase emulators:start`

### View Function Logs (Production)
```bash
firebase functions:log
```

### Emulator UI
**URL:** http://localhost:4000  
**Features:**
- View Firestore data in real-time
- Browse Storage files
- See Auth users
- Monitor function invocations
- View function logs

### Common Issues

**Issue:** Functions fail to deploy  
**Fix:** Ensure billing is enabled on Firebase project

**Issue:** TypeScript compilation errors  
**Fix:** Run `npm run build` and fix type errors

**Issue:** Function timeout  
**Fix:** Increase timeout in function definition (default 60s, max 540s)

---

## Performance Considerations

### Function Cold Starts
- First invocation after idle period is slower
- Keep functions lightweight
- Use Firebase Functions 2nd gen for better performance

### Firestore Query Optimization
- Use composite indexes for complex queries
- Limit query results (pagination)
- Avoid large document reads in loops

### Batch Operations
- Use Firestore batch writes for multiple updates
- Max 500 operations per batch

---

## Deployment Checklist

Before deploying to production:

- [ ] TypeScript compiles without errors (`npm run build`)
- [ ] Functions tested with emulators
- [ ] Firestore rules tested and validated
- [ ] Storage rules tested and validated
- [ ] Composite indexes deployed
- [ ] Environment variables set (if any)
- [ ] Function timeouts configured appropriately
- [ ] Error handling implemented
- [ ] Logging added for debugging

---

## Related Documentation

- [Architecture - Backend](./architecture-backend.md) _(To be generated)_
- [Data Models - Mobile](./data-models-mobile.md) - Firestore schema reference
- [Integration Architecture](./integration-architecture.md) _(To be generated)_
- [Development Guide - Mobile](./development-guide-mobile.md)
- [QA Checklist](./QA_CHECKLIST.md) - Security validation scenarios
