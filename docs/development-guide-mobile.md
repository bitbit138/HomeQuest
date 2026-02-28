# Development Guide - Android Mobile App

**Project:** HomeQuest  
**Part:** Mobile (Android)  
**Generated:** 2026-02-24

---

## Prerequisites

### Required Software
- **Android Studio:** Latest stable version (Iguana or newer recommended)
- **JDK:** Java 11 (configured in `build.gradle.kts`)
- **Android SDK:** API 26 (minimum) to API 36 (target)
- **Gradle:** 8.13.2 (via wrapper, no manual installation needed)
- **Kotlin:** 2.0.21 (managed by Gradle)

### Firebase Setup
- **Firebase Project:** Active Firebase project with Firestore, Storage, Auth, and Functions enabled
- **google-services.json:** Must be present at `app/google-services.json`
- **Firebase CLI:** For local emulator testing (optional but recommended)

---

## Installation

### 1. Clone and Open Project
```bash
cd /Users/tombitran/code/HomeQuest
```

Open the project in Android Studio (open the root `HomeQuest` folder).

### 2. Sync Gradle Dependencies
Android Studio will automatically detect `build.gradle.kts` and prompt to sync. Click **Sync Now**.

Alternatively, run from terminal:
```bash
./gradlew build
```

### 3. Configure Firebase
Ensure `app/google-services.json` is present and matches your Firebase project.

---

## Environment Setup

### Local Development Configuration

**File:** `local.properties` (auto-generated, gitignored)
```properties
sdk.dir=/path/to/Android/sdk
```

**File:** `gradle.properties`
- JVM heap: 2048m
- Configuration cache: enabled
- Parallel builds: enabled
- AndroidX: enabled

### Firebase Emulator (Optional)
For local testing without hitting production Firebase:

```bash
cd firebase
npm install -g firebase-tools
firebase emulators:start
```

Update `FirebaseManager.kt` to point to emulator endpoints during development.

---

## Build Commands

### Debug Build
```bash
./gradlew assembleDebug
```
**Output:** `app/build/outputs/apk/debug/app-debug.apk`  
**Package:** `dev.tombit.homequest.debug`

### Release Build
```bash
./gradlew assembleRelease
```
**Output:** `app/build/outputs/apk/release/app-release.apk`  
**Package:** `dev.tombit.homequest`  
**ProGuard:** Enabled (minification + resource shrinking)

### Build AAB (Android App Bundle)
```bash
./gradlew bundleRelease
```
**Output:** `app/build/outputs/bundle/release/app-release.aab`  
**Use:** Google Play Store uploads

---

## Run Commands

### Run on Device/Emulator (Android Studio)
1. Select device from dropdown
2. Click **Run** (green play button) or press `Shift+F10`

### Run via Command Line
```bash
./gradlew installDebug
adb shell am start -n dev.tombit.homequest.debug/.SplashScreenActivity
```

### Run with Firebase Emulators
1. Start Firebase emulators: `firebase emulators:start`
2. Configure app to use emulator endpoints
3. Run app normally

---

## Testing

### Run Unit Tests
```bash
./gradlew test
```
**Framework:** JUnit 4.13.2  
**Location:** `app/src/test/`

### Run Instrumentation Tests
```bash
./gradlew connectedAndroidTest
```
**Framework:** Espresso 3.6.1  
**Location:** `app/src/androidTest/`  
**Requires:** Connected device or running emulator

### Manual Testing Checklist
See [QA_CHECKLIST.md](./QA_CHECKLIST.md) for comprehensive manual test scenarios.

---

## Code Conventions (L08 Standards)

### Mandatory Patterns

#### 1. XML View IDs
**Format:** `{screen}_{WIDGET}_{descriptor}`
```xml
<!-- Example -->
<Button android:id="@+id/main_BTN_quests" />
<RecyclerView android:id="@+id/quest_list_RV_quests" />
```

#### 2. Activity Structure
Every Activity must have:
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    
    findViews()  // Required method
    initViews()  // Required method
}

private fun findViews() {
    // ViewBinding already handles this
}

private fun initViews() {
    // Set up click listeners, adapters, etc.
}
```

#### 3. Singleton Pattern
All utilities must use thread-safe double-check locking:
```kotlin
class MySingleton private constructor(context: Context) {
    private val contextRef = WeakReference(context.applicationContext)
    
    companion object {
        @Volatile
        private var instance: MySingleton? = null
        
        fun getInstance(context: Context): MySingleton {
            return instance ?: synchronized(this) {
                instance ?: MySingleton(context).also { instance = it }
            }
        }
    }
}
```

**Initialization:** All singletons initialized in `App.kt` only, never in Activities.

#### 4. Data Model Builder Pattern
```kotlin
data class User private constructor(
    val userId: String,
    val displayName: String,
    // ... other fields
) {
    class Builder {
        private var userId: String = ""
        private var displayName: String = ""
        
        fun userId(value: String) = apply { userId = value }
        fun displayName(value: String) = apply { displayName = value }
        fun build() = User(userId, displayName)
    }
}
```

#### 5. Callback Interfaces
All inter-component callbacks must be named interfaces in `interfaces/` package:
```kotlin
// interfaces/QuestCallback.kt
interface QuestCallback {
    fun onSuccess(quest: Task)
    fun onFailure(error: String)
}
```

#### 6. Constants
No hardcoded strings in Kotlin files. Use `Constants.kt` with nested objects:
```kotlin
object Constants {
    object Collections {
        const val USERS = "users"
        const val HOUSEHOLDS = "households"
    }
    object TaskStatus {
        const val OPEN = "open"
        const val CLAIMED = "claimed"
    }
}
```

#### 7. User Feedback
All Toast calls route through `SignalManager`:
```kotlin
SignalManager.getInstance(context).toast("Message here")
```

#### 8. Image Loading
All image loading routes through `ImageLoader`:
```kotlin
ImageLoader.getInstance(context).loadImage(url, imageView)
```

---

## Common Development Tasks

### Add New Activity
1. Create Activity class extending `AppCompatActivity`
2. Create layout XML in `res/layout/`
3. Implement `findViews()` and `initViews()`
4. Register in `AndroidManifest.xml`
5. Use ViewBinding for view access

### Add New Data Model
1. Create data class in `model/` package
2. Use private constructor + Builder pattern
3. Mirror Firestore field names exactly
4. Add to `FirebaseManager` CRUD methods

### Add New Singleton Utility
1. Create class in `utilities/` package
2. Implement thread-safe double-check locking
3. Use `WeakReference<Context>`
4. Initialize in `App.kt`

### Add New Firebase Operation
1. Add method to `FirebaseManager`
2. Create callback interface in `interfaces/`
3. Use Kotlin coroutines for async operations
4. Handle errors with callback

---

## Debugging

### View Logs
```bash
adb logcat | grep HomeQuest
```

### Firebase Emulator UI
```
http://localhost:4000
```
View Firestore data, Storage files, Auth users in browser.

### Common Issues

**Issue:** `google-services.json` not found  
**Fix:** Download from Firebase Console → Project Settings → Add Android app

**Issue:** Build fails with "SDK location not found"  
**Fix:** Create `local.properties` with `sdk.dir=/path/to/Android/sdk`

**Issue:** Firebase operations fail  
**Fix:** Check internet connection, verify Firebase project is active, check `google-services.json`

---

## Code Quality

### Linting
```bash
./gradlew lint
```
**Output:** `app/build/reports/lint-results.html`

### Code Analysis
Android Studio includes built-in inspections. Run **Analyze → Inspect Code** for full analysis.

---

## Related Documentation

- [Architecture - Mobile](./architecture-mobile.md) _(To be generated)_
- [Component Inventory - Mobile](./component-inventory-mobile.md)
- [Data Models - Mobile](./data-models-mobile.md)
- [QA Checklist](./QA_CHECKLIST.md) - Existing validation checklist
- [Development Guide - Backend](./development-guide-backend.md) _(To be generated)_
