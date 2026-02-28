# Source Tree Analysis

**Project:** HomeQuest  
**Generated:** 2026-02-24  
**Repository Type:** Multi-part (Android Mobile + Firebase Backend)

---

## Complete Directory Structure

```
HomeQuest/
├── app/                                    # Android Mobile App (Part: mobile)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/dev/tombit/homequest/
│   │   │   │   ├── App.kt                 # 🔹 Application entry point
│   │   │   │   │                          # Initializes Firebase, singletons, offline persistence
│   │   │   │   ├── SplashScreenActivity.kt # 🔹 Launch entry point (Lottie animation)
│   │   │   │   │
│   │   │   │   ├── LoginActivity.kt       # Auth: Sign in
│   │   │   │   ├── SignUpActivity.kt      # Auth: Registration + household setup
│   │   │   │   │
│   │   │   │   ├── MainActivity.kt        # Dashboard with activity feed
│   │   │   │   ├── QuestListActivity.kt   # Browse available quests
│   │   │   │   ├── QuestDetailActivity.kt # Quest details + proof upload
│   │   │   │   ├── CreateQuestActivity.kt # Create new quest
│   │   │   │   ├── RewardsActivity.kt     # Marketplace for rewards
│   │   │   │   ├── CreateCouponActivity.kt # List new reward
│   │   │   │   ├── ProfileActivity.kt     # Profile + leaderboard
│   │   │   │   │
│   │   │   │   ├── model/                 # Data models (Firestore schema mirrors)
│   │   │   │   │   ├── User.kt            # User profile with XP/coins/level
│   │   │   │   │   ├── Household.kt       # Family group with invite code
│   │   │   │   │   ├── Task.kt            # Quest with rewards and status
│   │   │   │   │   ├── Coupon.kt          # Purchasable reward
│   │   │   │   │   └── FeedItem.kt        # Activity feed entry
│   │   │   │   │
│   │   │   │   ├── adapters/              # RecyclerView adapters
│   │   │   │   │   ├── QuestAdapter.kt    # Quest list items
│   │   │   │   │   ├── CouponAdapter.kt   # Coupon list items
│   │   │   │   │   ├── FeedAdapter.kt     # Activity feed items
│   │   │   │   │   └── LeaderboardAdapter.kt # Leaderboard items
│   │   │   │   │
│   │   │   │   ├── utilities/             # Singleton utilities (L08 standards)
│   │   │   │   │   ├── FirebaseManager.kt # 🔹 Central Firebase operations
│   │   │   │   │   ├── ImageLoader.kt     # Glide wrapper (mandatory for all images)
│   │   │   │   │   ├── ImageCompressor.kt # JPEG compression (max 200KB)
│   │   │   │   │   ├── SignalManager.kt   # Toast + vibration (mandatory for feedback)
│   │   │   │   │   ├── SharedPreferencesManager.kt # Local storage
│   │   │   │   │   ├── TimeFormatter.kt   # Timestamp formatting
│   │   │   │   │   ├── HomeQuestMessagingService.kt # FCM handler
│   │   │   │   │   └── Constants.kt       # Centralized constants
│   │   │   │   │
│   │   │   │   └── interfaces/            # Callback interfaces (L08 standards)
│   │   │   │       ├── AuthCallback.kt    # Auth operation results
│   │   │   │       ├── QuestCallback.kt   # Quest operation results
│   │   │   │       ├── CouponCallback.kt  # Coupon operation results
│   │   │   │       └── FeedCallback.kt    # Feed loading results
│   │   │   │
│   │   │   ├── res/                       # Android resources
│   │   │   │   ├── layout/                # XML layouts (14 files)
│   │   │   │   │   ├── activity_*.xml     # Activity layouts
│   │   │   │   │   └── item_*.xml         # RecyclerView item layouts
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml        # All UI strings (no hardcoded text)
│   │   │   │   │   ├── colors.xml         # Color palette
│   │   │   │   │   └── themes.xml         # Material Design theme
│   │   │   │   ├── drawable/              # Vector drawables, icons
│   │   │   │   ├── mipmap-*/              # App launcher icons (all densities)
│   │   │   │   └── raw/                   # Lottie animation JSON
│   │   │   │
│   │   │   └── AndroidManifest.xml        # App configuration + permissions
│   │   │
│   │   ├── test/                          # Unit tests (JUnit)
│   │   └── androidTest/                   # Instrumentation tests (Espresso)
│   │
│   ├── build.gradle.kts                   # App module build config
│   ├── proguard-rules.pro                 # Code obfuscation rules
│   └── google-services.json               # Firebase project config
│
├── firebase/                               # Firebase Backend (Part: backend)
│   ├── functions/                         # Cloud Functions
│   │   ├── src/
│   │   │   └── index.ts                   # 🔹 Functions entry point
│   │   │                                  # Exports: reward approval, feed pruning
│   │   ├── package.json                   # Node dependencies
│   │   └── tsconfig.json                  # TypeScript config (ES2017 target)
│   │
│   ├── firestore/
│   │   ├── firestore.rules                # 🔹 Security rules (critical)
│   │   │                                  # Protects coinBalance, XP, level
│   │   └── firestore.indexes.json         # Composite indexes for queries
│   │
│   └── storage/
│       └── storage.rules                  # 🔹 Storage security (JPEG, 200KB max)
│
├── gradle/                                 # Gradle wrapper + version catalog
│   ├── wrapper/                           # Gradle distribution
│   └── libs.versions.toml                 # 🔹 Centralized dependency versions
│
├── docs/                                   # 📚 Project documentation
│   ├── QA_CHECKLIST.md                    # Existing: Pre-launch QA checklist
│   ├── project-scan-report.json           # Generated: Workflow state
│   ├── data-models-mobile.md              # Generated: Firestore schema
│   └── component-inventory-mobile.md      # Generated: UI components
│
├── _bmad/                                  # BMAD workflow system (internal)
│   ├── core/                              # Core workflow engine
│   ├── bmm/                               # BMM module workflows
│   └── _config/                           # Configuration
│
├── _bmad-output/                           # BMAD generated artifacts
│   ├── planning-artifacts/                # PRDs, epics, stories
│   └── implementation-artifacts/          # Implementation docs
│
├── build.gradle.kts                        # 🔹 Root project build config
├── settings.gradle.kts                     # Gradle project settings
├── gradle.properties                       # Gradle properties
├── gradlew                                 # Gradle wrapper script (Unix)
├── gradlew.bat                             # Gradle wrapper script (Windows)
├── firebase.json                           # 🔹 Firebase project configuration
└── local.properties                        # Local SDK paths (gitignored)
```

---

## Critical Directories

### Android Mobile App (`app/`)

#### `/app/src/main/java/dev/tombit/homequest/`
**Purpose:** Main application source code  
**Entry Point:** `App.kt` - Application class that initializes Firebase and singletons  
**Launch Entry:** `SplashScreenActivity.kt` - First screen shown (Lottie animation)

**Key Subdirectories:**
- **`model/`** - Data models mirroring Firestore schema (5 classes)
- **`adapters/`** - RecyclerView adapters for lists (4 adapters)
- **`utilities/`** - Singleton utilities following L08 standards (8 utilities)
- **`interfaces/`** - Callback interfaces for async operations (4 interfaces)

#### `/app/src/main/res/`
**Purpose:** Android resources (layouts, strings, drawables)  
**Key Subdirectories:**
- **`layout/`** - XML layouts (14 files: 10 activities + 4 item layouts)
- **`values/`** - Strings, colors, themes (no hardcoded text per L08)
- **`drawable/`** - Vector graphics and icons
- **`mipmap-*/`** - App launcher icons (all density variants)
- **`raw/`** - Lottie animation JSON for splash screen

---

### Firebase Backend (`firebase/`)

#### `/firebase/functions/`
**Purpose:** Serverless Cloud Functions (TypeScript)  
**Entry Point:** `src/index.ts` - Exports all Cloud Functions  
**Runtime:** Node.js 20

**Key Functions:**
- Reward approval (approve quest completion)
- Feed pruning (scheduled daily cleanup)
- XP/coin balance updates (server-side only)

#### `/firebase/firestore/`
**Purpose:** Firestore database configuration  
**Critical Files:**
- **`firestore.rules`** - Security rules protecting sensitive fields
- **`firestore.indexes.json`** - Composite indexes for query performance

#### `/firebase/storage/`
**Purpose:** Firebase Storage configuration  
**Critical Files:**
- **`storage.rules`** - Enforces JPEG format and 200KB max size

---

## Build System

### Root Level
- **`build.gradle.kts`** - Root project build configuration
- **`settings.gradle.kts`** - Defines project structure (includes `:app`)
- **`gradle/libs.versions.toml`** - Version catalog for all dependencies

### Gradle Wrapper
- **`gradlew`** / **`gradlew.bat`** - Platform-specific Gradle wrapper scripts
- **`gradle/wrapper/`** - Gradle distribution files

---

## Configuration Files

### Firebase
- **`firebase.json`** - Firebase project configuration
  - Functions source: `firebase/functions`
  - Firestore rules: `firebase/firestore/firestore.rules`
  - Storage rules: `firebase/storage/storage.rules`
  - Emulator ports configured

### Android
- **`app/google-services.json`** - Firebase project credentials
- **`app/src/main/AndroidManifest.xml`** - App permissions and component declarations
- **`local.properties`** - Local Android SDK path (not in version control)

---

## Integration Points

### Mobile → Backend Communication

1. **Firebase Auth**
   - Mobile: `FirebaseManager` uses Firebase Auth SDK
   - Backend: Cloud Functions validate `request.auth.uid`

2. **Firestore Database**
   - Mobile: Real-time listeners via Firestore SDK
   - Backend: Cloud Functions trigger on document changes
   - Path: `/households/{householdId}/tasks/{taskId}` → Triggers reward function

3. **Firebase Storage**
   - Mobile: `ImageCompressor` → Upload to `proofs/{householdId}/{taskId}.jpg`
   - Backend: Storage rules validate format and size

4. **Firebase Cloud Messaging (FCM)**
   - Mobile: `HomeQuestMessagingService` receives notifications
   - Backend: Cloud Functions send notifications on events

---

## Asset Locations

### Mobile Assets
- **Launcher Icons:** `app/src/main/res/mipmap-*/` (all density variants)
- **Vector Drawables:** `app/src/main/res/drawable/`
- **Lottie Animation:** `app/src/main/res/raw/` (splash screen)

### User-Generated Content (Firebase Storage)
- **Proof Images:** `proofs/{householdId}/{taskId}.jpg`
- **Avatars:** `avatars/{userId}.jpg`

---

## Testing Structure

### Unit Tests
**Location:** `app/src/test/`  
**Framework:** JUnit 4.13.2  
**Status:** No test files detected in quick scan

### Instrumentation Tests
**Location:** `app/src/androidTest/`  
**Framework:** Espresso 3.6.1  
**Status:** No test files detected in quick scan

---

## Build Artifacts (Excluded from Scans)

- `.gradle/` - Gradle cache
- `.idea/` - IntelliJ IDEA project files
- `.kotlin/` - Kotlin compiler cache
- `app/build/` - Android build outputs
- `build/` - Root build directory
- `firebase/functions/lib/` - Compiled TypeScript (not in repo)
- `firebase/functions/node_modules/` - Node dependencies (not in repo)

---

## Documentation System

### BMAD Workflow System
- **`_bmad/`** - Workflow automation system (internal tooling)
- **`_bmad-output/`** - Generated planning and implementation artifacts
- **`docs/`** - Project knowledge base (this documentation)

---

## Entry Points Summary

| Part | Entry Point | Purpose |
|------|-------------|---------|
| **Mobile** | `app/src/main/java/dev/tombit/homequest/App.kt` | Application initialization |
| **Mobile** | `app/src/main/java/dev/tombit/homequest/SplashScreenActivity.kt` | UI entry point (launcher) |
| **Backend** | `firebase/functions/src/index.ts` | Cloud Functions exports |

---

## Critical Folders by Part

### Mobile (Android)
1. **`app/src/main/java/dev/tombit/homequest/`** - All Kotlin source code
2. **`app/src/main/res/layout/`** - All UI layouts (ViewBinding)
3. **`app/src/main/res/values/`** - Strings, colors, themes
4. **`app/src/main/AndroidManifest.xml`** - App configuration

### Backend (Firebase)
1. **`firebase/functions/src/`** - Cloud Functions source
2. **`firebase/firestore/firestore.rules`** - Database security (critical)
3. **`firebase/storage/storage.rules`** - Storage security (critical)
4. **`firebase/firestore/firestore.indexes.json`** - Query indexes

---

## Related Documentation

- [Project Overview](./project-overview.md) _(To be generated)_
- [Architecture - Mobile](./architecture-mobile.md) _(To be generated)_
- [Architecture - Backend](./architecture-backend.md) _(To be generated)_
- [Integration Architecture](./integration-architecture.md) _(To be generated)_
- [Data Models - Mobile](./data-models-mobile.md)
- [Component Inventory - Mobile](./component-inventory-mobile.md)
