# Architecture - Android Mobile App

**Project:** HomeQuest  
**Part:** Mobile (Android)  
**Generated:** 2026-02-24  
**Version:** 1.0.0

---

## Executive Summary

HomeQuest is a native Android mobile application built with Kotlin that gamifies household chores through a quest-and-reward system. The app follows a traditional Activity-based MVC architecture with strict adherence to L08 coding standards, including mandatory ViewBinding, singleton utilities with thread-safe initialization, and callback-based async operations. The app integrates deeply with Firebase services (Auth, Firestore, Storage, FCM) for backend functionality and uses Material Design for UI.

**Key Characteristics:**
- **Architecture Pattern:** Activity-based MVC with singleton utilities
- **Language:** Kotlin 2.0.21
- **Min SDK:** API 26 (Android 8.0 Oreo)
- **Target SDK:** API 36
- **Backend Integration:** Firebase (Auth, Firestore, Storage, FCM)
- **Coding Standards:** L08 conventions (strict naming, initialization, patterns)

---

## Technology Stack

| Category | Technology | Version | Purpose |
|----------|-----------|---------|---------|
| **Platform** | Android | API 26-36 | Native mobile platform |
| **Language** | Kotlin | 2.0.21 | Primary development language |
| **Build System** | Gradle (AGP) | 8.13.2 | Build automation |
| **UI Framework** | Material Design | 1.13.0 | Google's design system |
| **View Binding** | ViewBinding | Built-in | Type-safe view access (mandatory) |
| **Layouts** | ConstraintLayout | 2.2.1 | Flexible layouts |
| **Lists** | RecyclerView | 1.4.0 | Efficient list rendering |
| **Async** | Kotlin Coroutines | 1.8.1 | Asynchronous operations |
| **Image Loading** | Glide | 5.0.5 | Image loading/caching |
| **Animations** | Lottie | 6.6.6 | JSON-based animations |
| **JSON** | Gson | 2.10.1 | JSON serialization |
| **Backend** | Firebase BOM | 33.7.0 | Firebase services |
| **Auth** | Firebase Auth | Via BOM | User authentication |
| **Database** | Firestore | Via BOM | NoSQL database |
| **Storage** | Firebase Storage | Via BOM | File storage |
| **Messaging** | FCM | Via BOM | Push notifications |
| **Testing** | JUnit + Espresso | 4.13.2 / 3.6.1 | Unit and UI testing |

---

## Architecture Pattern

### Activity-Based MVC

**Model:**
- Data classes in `model/` package (User, Household, Task, Coupon, FeedItem)
- Mirror Firestore schema exactly
- Use private constructor + Builder pattern (L08 standard)

**View:**
- XML layouts in `res/layout/`
- ViewBinding for type-safe view access
- Material Design components
- RecyclerView with custom adapters

**Controller:**
- Activities handle UI logic and user interactions
- `FirebaseManager` singleton handles data operations
- Callback interfaces for async results
- Utilities provide cross-cutting concerns

---

## Component Architecture

### Layer 1: Application Entry Point

**App.kt**
- Extends `Application`
- Initializes all singleton utilities
- Configures Firebase (offline persistence, FCM)
- Runs once at app launch

**SplashScreenActivity.kt**
- First visible screen (launcher intent)
- Displays Lottie animation
- Checks authentication state
- Routes to LoginActivity or MainActivity

---

### Layer 2: Activities (UI Controllers)

**10 Activities organized by function:**

#### Authentication (2)
- `LoginActivity` - Email/password sign-in
- `SignUpActivity` - Registration + household creation/join

#### Dashboard (1)
- `MainActivity` - Home screen with activity feed, navigation

#### Quest Management (3)
- `QuestListActivity` - Browse available quests
- `QuestDetailActivity` - View quest, upload proof
- `CreateQuestActivity` - Create new quest

#### Rewards (2)
- `RewardsActivity` - Marketplace for rewards
- `CreateCouponActivity` - List new reward

#### Profile (1)
- `ProfileActivity` - User stats, leaderboard, sign out

**Activity Lifecycle Pattern:**
```kotlin
override fun onCreate() {
    // ViewBinding setup
    findViews()  // Required by L08
    initViews()  // Required by L08
    // Load data via FirebaseManager
}

override fun onPause() {
    // Remove Firestore listeners
}

override fun onResume() {
    // Re-attach listeners if needed
}
```

---

### Layer 3: Data Layer

**FirebaseManager Singleton**
- Central hub for all Firebase operations
- Provides methods for:
  - User CRUD
  - Household operations
  - Task/quest operations
  - Coupon operations
  - Feed queries
- Uses callback interfaces for async results
- Manages Firestore listeners

**Data Models (5)**
- `User` - Profile with gamification metrics
- `Household` - Family group with invite system
- `Task` - Quest with rewards and status lifecycle
- `Coupon` - Purchasable reward
- `FeedItem` - Activity log entry

---

### Layer 4: Presentation Layer

**RecyclerView Adapters (4)**
- `QuestAdapter` - Quest list items
- `CouponAdapter` - Coupon list items
- `FeedAdapter` - Activity feed items
- `LeaderboardAdapter` - Ranking items

**Adapter Pattern:**
- ViewHolder with ViewBinding
- Click listeners via callbacks
- Data binding in `onBindViewHolder()`

---

### Layer 5: Utility Layer

**8 Singleton Utilities (L08 Standards):**

1. **FirebaseManager** - Firebase operations hub
2. **ImageLoader** - Centralized image loading (wraps Glide)
3. **ImageCompressor** - JPEG compression (max 200KB)
4. **SignalManager** - User feedback (Toast, vibration)
5. **SharedPreferencesManager** - Local storage
6. **TimeFormatter** - Timestamp formatting
7. **HomeQuestMessagingService** - FCM handler
8. **Constants** - Centralized constants

**Singleton Requirements:**
- Thread-safe double-check locking with `@Volatile`
- `WeakReference<Context>` (no direct Context storage)
- Initialized in `App.kt` only

---

### Layer 6: Interface Layer

**4 Callback Interfaces:**
- `AuthCallback` - Authentication results
- `QuestCallback` - Quest operation results
- `CouponCallback` - Coupon operation results
- `FeedCallback` - Feed loading results

**Purpose:** Decouple async operations from UI components

---

## Data Architecture

### Firestore Schema

**Hierarchy:**
```
/users/{userId}
/households/{householdId}
  /tasks/{taskId}
  /coupons/{couponId}
  /activity_feed/{entryId}
```

**Key Relationships:**
- User → Household (via `householdId` field)
- Household → Tasks (subcollection)
- Household → Coupons (subcollection)
- Household → Activity Feed (subcollection)

**Protected Fields (Server-Only):**
- `User.coinBalance`
- `User.currentXp`
- `User.level`

### Firebase Storage Structure

**Paths:**
- `proofs/{householdId}/{taskId}.jpg` - Quest proof images
- `avatars/{userId}.jpg` - User profile pictures

**Constraints:**
- JPEG format only
- 200KB max size (enforced by storage rules)
- Client-side compression via `ImageCompressor`

---

## State Management

### Local State
- **SharedPreferences:** User session, preferences
- **In-Memory:** Activity-level UI state
- **ViewModels:** Not used (traditional Activity pattern)

### Remote State
- **Firestore:** Source of truth for all data
- **Offline Persistence:** Enabled in `App.kt`
- **Real-time Sync:** Listeners for feed and quest updates

### State Synchronization
- Firestore SDK handles sync automatically
- Offline writes queued and synced when online
- Conflict resolution: last-write-wins (Firestore default)

---

## API Design

### Firebase SDK Integration

**Authentication API:**
```kotlin
FirebaseAuth.getInstance()
    .signInWithEmailAndPassword(email, password)
    .addOnSuccessListener { /* success */ }
    .addOnFailureListener { /* error */ }
```

**Firestore API:**
```kotlin
FirebaseFirestore.getInstance()
    .collection("households/{householdId}/tasks")
    .whereEqualTo("status", "open")
    .orderBy("deadline")
    .addSnapshotListener { snapshot, error ->
        // Real-time updates
    }
```

**Storage API:**
```kotlin
FirebaseStorage.getInstance()
    .reference.child("proofs/{householdId}/{taskId}.jpg")
    .putFile(compressedImageUri)
    .addOnSuccessListener { /* get download URL */ }
```

---

## Security Architecture

### Authentication
- Firebase Auth with email/password
- Auth token included in all Firebase SDK requests
- Session persists until sign-out

### Authorization
- Firestore security rules enforce household isolation
- Users can only access their household's data
- Protected fields (coins, XP, level) read-only from client

### Data Validation
- Client-side: Input validation in Activities
- Server-side: Firestore rules + Cloud Functions
- Storage rules: Format and size validation

---

## Testing Strategy

### Unit Testing
- **Framework:** JUnit 4.13.2
- **Location:** `app/src/test/`
- **Target:** Utility classes, data models, business logic
- **Status:** No tests detected in quick scan

### Instrumentation Testing
- **Framework:** Espresso 3.6.1
- **Location:** `app/src/androidTest/`
- **Target:** UI flows, Activity interactions
- **Status:** No tests detected in quick scan

### Manual Testing
- **Checklist:** See [QA_CHECKLIST.md](./QA_CHECKLIST.md)
- **Scenarios:** Auth flow, quest flow, rewards flow, feed updates, offline behavior
- **Security Tests:** Attempt protected field writes, cross-household access

---

## Development Workflow

### Local Development
1. Open project in Android Studio
2. Sync Gradle dependencies
3. Start Firebase emulators (optional)
4. Run on device/emulator
5. Use Logcat for debugging

### Code Standards Enforcement
- **L08 Conventions:** Enforced by code review (see QA_CHECKLIST.md)
- **ViewBinding:** Mandatory (enabled in `build.gradle.kts`)
- **No Hardcoded Strings:** All text in `strings.xml`
- **Singleton Pattern:** Thread-safe with `@Volatile`

### Build Process
1. Gradle compiles Kotlin → Java bytecode
2. Android build tools package into APK/AAB
3. ProGuard obfuscates release builds
4. Signing applied for release builds

---

## Deployment Architecture

### Build Variants
- **Debug:** `dev.tombit.homequest.debug` - Debuggable, no obfuscation
- **Release:** `dev.tombit.homequest` - Obfuscated, optimized, signed

### Distribution
- **Platform:** Google Play Store
- **Format:** Android App Bundle (AAB)
- **Signing:** Keystore-based signing for release
- **ProGuard:** Code obfuscation and resource shrinking enabled

### Firebase Configuration
- **Development:** Use Firebase emulators
- **Production:** `google-services.json` with production project ID

---

## Performance Considerations

### Image Optimization
- **Compression:** `ImageCompressor` reduces images to < 200KB
- **Caching:** Glide caches loaded images via `ImageLoader`
- **Format:** JPEG only for storage efficiency

### Query Optimization
- **Composite Indexes:** Deployed for all complex queries
- **Pagination:** Feed limited to 20 items with `startAfterDocument()`
- **Caching:** Leaderboard cached for 60 seconds

### Offline Support
- **Firestore Persistence:** Enabled for offline access
- **Write Queue:** Offline writes synced when online
- **Listener Management:** Detached in `onPause()`, reattached in `onResume()`

---

## Code Organization Principles

### Package Structure
```
dev.tombit.homequest/
├── (root)         # Activities
├── model/         # Data models
├── adapters/      # RecyclerView adapters
├── utilities/     # Singleton utilities
└── interfaces/    # Callback interfaces
```

### Naming Conventions
- **Activities:** `{Purpose}Activity.kt` (e.g., `MainActivity.kt`)
- **Layouts:** `activity_{purpose}.xml`, `item_{type}.xml`
- **View IDs:** `{screen}_{WIDGET}_{descriptor}`
- **Callbacks:** `{Domain}Callback.kt`

### Dependency Flow
```
Activities
    ↓ (use)
Adapters + Utilities
    ↓ (use)
Models + Interfaces
    ↓ (mirror)
Firebase Schema
```

---

## Source Tree

See [Source Tree Analysis](./source-tree-analysis.md) for complete annotated directory structure.

**Critical Paths:**
- `app/src/main/java/dev/tombit/homequest/` - All Kotlin source
- `app/src/main/res/layout/` - All UI layouts
- `app/src/main/AndroidManifest.xml` - App configuration
- `app/build.gradle.kts` - Build configuration

---

## Related Documentation

- [Project Overview](./project-overview.md) _(To be generated)_
- [Architecture - Backend](./architecture-backend.md) _(To be generated)_
- [Integration Architecture](./integration-architecture.md)
- [Data Models - Mobile](./data-models-mobile.md)
- [Component Inventory - Mobile](./component-inventory-mobile.md)
- [Development Guide - Mobile](./development-guide-mobile.md)
- [Source Tree Analysis](./source-tree-analysis.md)
- [QA Checklist](./QA_CHECKLIST.md)
