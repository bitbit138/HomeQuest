# Integration Architecture

**Project:** HomeQuest  
**Generated:** 2026-02-24  
**Repository Type:** Multi-part (Android Mobile + Firebase Backend)

---

## Overview

HomeQuest uses a **Firebase-mediated architecture** where the Android mobile app and Firebase backend communicate exclusively through Firebase services (Firestore, Storage, Auth, Cloud Messaging). There are no direct HTTP/REST API calls between parts.

---

## Architecture Diagram

```
┌─────────────────────────────────────┐
│   Android Mobile App (Kotlin)      │
│   - Activities (UI)                 │
│   - FirebaseManager (client SDK)   │
│   - Real-time listeners             │
└──────────────┬──────────────────────┘
               │
               │ Firebase SDKs
               │ (Auth, Firestore, Storage, FCM)
               │
               ▼
┌─────────────────────────────────────┐
│        Firebase Platform            │
│  ┌─────────────────────────────┐   │
│  │  Firebase Auth              │   │
│  │  - User authentication      │   │
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │  Firestore Database         │   │
│  │  - Real-time sync           │   │
│  │  - Offline persistence      │   │
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │  Firebase Storage           │   │
│  │  - Proof images             │   │
│  │  - Avatar images            │   │
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │  Cloud Messaging (FCM)      │   │
│  │  - Push notifications       │   │
│  └─────────────────────────────┘   │
└──────────────┬──────────────────────┘
               │
               │ Triggers & Admin SDK
               │
               ▼
┌─────────────────────────────────────┐
│   Cloud Functions (TypeScript)     │
│   - Firestore triggers              │
│   - Scheduled functions             │
│   - Admin SDK operations            │
└─────────────────────────────────────┘
```

---

## Integration Points

### 1. Authentication Flow

**Type:** Firebase Auth  
**Direction:** Mobile ↔ Firebase Auth ↔ Backend

**Mobile Side:**
- Uses Firebase Auth SDK for sign-in/sign-up
- Stores auth token locally
- Includes token in all Firebase SDK requests automatically

**Backend Side:**
- Cloud Functions validate `request.auth.uid` automatically
- Admin SDK can create/manage users
- Security rules enforce authentication

**Data Flow:**
1. User signs in via `LoginActivity` → Firebase Auth
2. Auth token stored on device
3. Token included in all Firestore/Storage requests
4. Cloud Functions receive authenticated context

---

### 2. Firestore Database (Primary Integration)

**Type:** Real-time NoSQL Database  
**Direction:** Mobile ↔ Firestore ↔ Backend (triggers)

**Mobile Side:**
- `FirebaseManager` performs CRUD operations
- Real-time listeners for live updates (feed, quests)
- Offline persistence enabled in `App.kt`
- Security rules enforce access control

**Backend Side:**
- Cloud Functions triggered on document changes
- Admin SDK bypasses security rules
- Server-side validation and business logic

**Key Integration Patterns:**

#### Pattern 1: Task Completion Flow
```
Mobile: User uploads proof → Task.status = "pending_verification"
   ↓
Firestore: Document update event
   ↓
Backend: Cloud Function triggered
   ↓
Backend: Validates proof, awards XP/coins, updates status = "completed"
   ↓
Firestore: Document updated
   ↓
Mobile: Real-time listener receives update → UI refreshes
```

#### Pattern 2: Real-time Activity Feed
```
Backend: Cloud Function writes to activity_feed collection
   ↓
Firestore: New document created
   ↓
Mobile: Real-time listener (MainActivity) receives new entry
   ↓
Mobile: FeedAdapter updates UI automatically
```

#### Pattern 3: Coupon Purchase
```
Mobile: User clicks purchase → FirebaseManager.purchaseCoupon()
   ↓
Backend: Cloud Function (callable or trigger)
   ↓
Backend: Transaction: Check coins, debit balance, set buyerId
   ↓
Firestore: Atomic update
   ↓
Mobile: Callback receives success/failure
```

---

### 3. Firebase Storage

**Type:** File Storage  
**Direction:** Mobile → Storage → Backend (optional triggers)

**Mobile Side:**
- `ImageCompressor` compresses images (JPEG, max 200KB)
- Upload to `proofs/{householdId}/{taskId}.jpg`
- Storage rules validate format and size
- URL stored in Firestore document

**Backend Side:**
- Admin SDK can read/write storage
- Storage rules enforced for client uploads
- No direct Storage triggers (URL stored in Firestore instead)

**Data Flow:**
1. User selects image in `QuestDetailActivity`
2. `ImageCompressor` compresses to JPEG < 200KB
3. Upload via Firebase Storage SDK
4. Storage rules validate: JPEG format, 200KB max
5. Get download URL
6. Store URL in Task document (`proofImageUrl`)
7. Update task status to trigger Cloud Function

---

### 4. Firebase Cloud Messaging (FCM)

**Type:** Push Notifications  
**Direction:** Backend → FCM → Mobile

**Mobile Side:**
- `HomeQuestMessagingService` receives notifications
- FCM token refreshed in `App.onCreate()`
- Token stored in User document (`fcmToken` field)

**Backend Side:**
- Cloud Functions send notifications via Admin SDK
- Target specific users by FCM token
- Notification payload includes event details

**Use Cases:**
- Quest assigned to user
- Quest completed by teammate
- Coupon purchased
- Level-up achievement

---

## Data Flow Patterns

### Write Operations

#### Client-Initiated Writes
Mobile app can write to:
- User profile (except `coinBalance`, `currentXp`, `level`)
- Household data (members can update)
- Tasks (create, update status, add proof)
- Coupons (create, update)
- Activity feed (create only)

#### Server-Initiated Writes (Cloud Functions)
Backend has exclusive write access to:
- `User.coinBalance`
- `User.currentXp`
- `User.level`
- Task status transitions to `completed`
- Activity feed entries for system events

### Read Operations

#### Real-time Listeners (Mobile)
- Activity feed: Live updates in `MainActivity`
- Quest list: Updates when new quests created
- User profile: Updates when XP/coins change

#### Query Patterns (Mobile)
- Quest list: `status == "open"`, ordered by `deadline`
- Available coupons: `buyerId == null`
- Owned coupons: `buyerId == currentUserId`
- Activity feed: Paginated with `limit(20)`, ordered by `timestamp` DESC

---

## Security Architecture

### Authentication Layer
- All requests require authenticated user (`request.auth != null`)
- User ID from auth token used for authorization
- No API keys or secrets in mobile app

### Authorization Layer
- Household-based access control
- Helper function: `isMember(householdId)` checks membership
- Users can only access their household's data

### Protected Operations
- **Client Cannot:**
  - Modify `coinBalance`, `currentXp`, `level`
  - Delete any documents
  - Update/delete activity feed entries
  - Access other households' data
- **Server Can:**
  - Update all fields via Admin SDK
  - Perform atomic transactions
  - Bypass security rules

---

## State Synchronization

### Offline Support
- **Mobile:** Firestore offline persistence enabled
- **Behavior:** 
  - Reads from local cache when offline
  - Writes queued and synced when online
  - Real-time listeners resume on reconnection

### Conflict Resolution
- Firestore handles automatic conflict resolution
- Last-write-wins for most fields
- Transactions used for critical operations (coin purchases)

---

## Error Handling

### Mobile Error Handling
- All Firebase operations use callbacks (`AuthCallback`, `QuestCallback`, etc.)
- Errors displayed via `SignalManager.toast()`
- Network errors handled gracefully with offline support

### Backend Error Handling
- Cloud Functions log errors to Firebase Console
- Failed transactions rolled back automatically
- Retry logic for transient failures

---

## Performance Optimizations

### Caching Strategy
- **Leaderboard:** Cached for 60 seconds (mobile-side)
- **Firestore:** Offline persistence caches all accessed data
- **Images:** Glide caches via `ImageLoader` singleton

### Query Optimization
- Composite indexes for all complex queries
- Pagination for feed (limit 20 items)
- Selective field reads where possible

### Function Optimization
- Scheduled feed pruning (daily at 03:00 UTC)
- Batch operations for multiple updates
- Minimal cold start time

---

## Communication Protocols

### Firebase SDK Communication
- **Protocol:** HTTPS (Firebase REST API under the hood)
- **Authentication:** Firebase Auth token in headers
- **Serialization:** Protocol Buffers (Firebase SDK handles)

### Real-time Updates
- **Protocol:** WebSocket (Firestore real-time listeners)
- **Reconnection:** Automatic with exponential backoff
- **Bandwidth:** Optimized by Firebase SDK

---

## Deployment Architecture

### Mobile Deployment
- **Distribution:** Google Play Store (AAB format)
- **Build:** Gradle assembles APK/AAB
- **Signing:** Release builds signed with keystore
- **ProGuard:** Code obfuscation enabled for release

### Backend Deployment
- **Platform:** Firebase Cloud Functions (Google Cloud Platform)
- **Regions:** Configurable (default: us-central1)
- **Scaling:** Automatic based on load
- **Deployment:** `firebase deploy --only functions`

---

## Integration Testing

### Local Testing (Emulators)
1. Start Firebase emulators: `firebase emulators:start`
2. Configure mobile app to use emulator endpoints:
   - Auth: `http://10.0.2.2:9099` (Android emulator)
   - Firestore: `http://10.0.2.2:8080`
   - Storage: `http://10.0.2.2:9199`
3. Run mobile app on emulator/device
4. Test full integration flows

### Production Testing
- Use Firebase Test Lab for device testing
- Monitor Cloud Functions logs: `firebase functions:log`
- Check Firestore usage in Firebase Console
- Validate security rules with test scenarios (see QA_CHECKLIST.md)

---

## Shared Dependencies

### Firebase Project
Both parts depend on the same Firebase project:
- **Project ID:** Configured in `google-services.json` and Firebase CLI
- **Firestore Database:** Shared data layer
- **Storage Bucket:** Shared file storage
- **Auth Users:** Single user pool

### Data Schema
Both parts must maintain schema compatibility:
- Mobile models mirror Firestore schema exactly
- Backend functions expect specific field names and types
- Schema changes require coordinated updates

---

## Related Documentation

- [Architecture - Mobile](./architecture-mobile.md) _(To be generated)_
- [Architecture - Backend](./architecture-backend.md) _(To be generated)_
- [Data Models - Mobile](./data-models-mobile.md) - Firestore schema
- [Development Guide - Mobile](./development-guide-mobile.md)
- [Development Guide - Backend](./development-guide-backend.md)
- [QA Checklist](./QA_CHECKLIST.md) - Security validation scenarios
