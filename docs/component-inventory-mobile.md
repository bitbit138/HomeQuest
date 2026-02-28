# Component Inventory - Android Mobile App

**Project:** HomeQuest  
**Part:** Mobile (Android)  
**Generated:** 2026-02-24  
**Scan Level:** Quick (pattern-based)

---

## Overview

HomeQuest follows a traditional Android Activity-based architecture with ViewBinding, RecyclerView adapters, and singleton utilities. The app adheres to L08 coding standards with strict conventions for naming, initialization, and component communication.

---

## Activities (10)

### Entry Point

#### SplashScreenActivity
- **Purpose:** App launch screen with Lottie animation
- **Layout:** `activity_splash_screen.xml`
- **Navigation:** Routes to LoginActivity or MainActivity based on auth state
- **Special:** Uses Lottie animation library

---

### Authentication (2)

#### LoginActivity
- **Purpose:** User sign-in
- **Layout:** `activity_login.xml`
- **Features:** Email/password authentication via Firebase Auth
- **Navigation:** → MainActivity on success

#### SignUpActivity
- **Purpose:** New user registration
- **Layout:** `activity_sign_up.xml`
- **Features:** 
  - Create account with Firebase Auth
  - Create new household OR join via invite code
  - Household invite code validation
- **Navigation:** → MainActivity on success

---

### Core Screens (7)

#### MainActivity
- **Purpose:** Dashboard/home screen with activity feed
- **Layout:** `activity_main.xml`
- **Features:**
  - Real-time activity feed (RecyclerView with FeedAdapter)
  - User stats display (coins, XP, level)
  - Navigation to Quests, Rewards, Profile
  - Firestore listener for feed updates
- **Adapter:** FeedAdapter
- **Callbacks:** FeedCallback

#### QuestListActivity
- **Purpose:** Browse available and claimed quests
- **Layout:** `activity_quest_list.xml`
- **Features:**
  - List of open quests (status = "open")
  - Ordered by deadline
  - Claim quest functionality
- **Adapter:** QuestAdapter
- **Callbacks:** QuestCallback
- **Navigation:** → QuestDetailActivity on item click

#### QuestDetailActivity
- **Purpose:** View and interact with a specific quest
- **Layout:** `activity_quest_detail.xml`
- **Features:**
  - Quest details display
  - Upload photo proof (ImageCompressor)
  - Submit for verification
  - Status tracking
- **Callbacks:** QuestCallback

#### CreateQuestActivity
- **Purpose:** Create new household quest
- **Layout:** `activity_create_quest.xml`
- **Features:**
  - Quest title (max 80 chars)
  - Description (optional)
  - XP reward (10-500)
  - Coin reward (5-200)
  - Deadline picker
  - Recurring flag
- **Validation:** Input validation per reward ranges

#### RewardsActivity
- **Purpose:** Marketplace for household rewards
- **Layout:** `activity_rewards.xml`
- **Features:**
  - Available coupons (buyerId == null)
  - Owned coupons (buyerId == currentUser)
  - Purchase with coins
  - Redeem owned coupons
- **Adapter:** CouponAdapter
- **Callbacks:** CouponCallback

#### CreateCouponActivity
- **Purpose:** List a new reward in marketplace
- **Layout:** `activity_create_coupon.xml`
- **Features:**
  - Reward title (max 60 chars)
  - Coin cost
- **Validation:** Input validation

#### ProfileActivity
- **Purpose:** User profile and household leaderboard
- **Layout:** `activity_profile.xml`
- **Features:**
  - User stats (level, XP, coins)
  - Household leaderboard (sorted by XP)
  - Sign out
  - Avatar display
- **Adapter:** LeaderboardAdapter
- **Caching:** Leaderboard cached for 60 seconds

---

## RecyclerView Adapters (4)

### QuestAdapter
- **Purpose:** Display quest items in list
- **Item Layout:** `item_quest.xml`
- **Binds:** Quest title, rewards, deadline, status, claim button

### CouponAdapter
- **Purpose:** Display reward items in marketplace
- **Item Layout:** `item_coupon.xml`
- **Binds:** Coupon title, cost, purchase/redeem buttons

### FeedAdapter
- **Purpose:** Display activity feed entries
- **Item Layout:** `item_feed.xml`
- **Binds:** User name, event message, timestamp, avatar

### LeaderboardAdapter
- **Purpose:** Display household member rankings
- **Item Layout:** `item_leaderboard.xml`
- **Binds:** Rank, user name, level, XP, avatar

---

## Utility Singletons (8)

All utilities follow L08 standards:
- Thread-safe double-check locking with `@Volatile`
- Initialized in `App.kt` only
- Use `WeakReference<Context>` (no direct Context storage)

### FirebaseManager
- **Purpose:** Central Firebase operations manager
- **Responsibilities:**
  - User CRUD operations
  - Household operations
  - Task/quest operations
  - Coupon operations
  - Feed queries
  - Real-time listener management
- **Pattern:** Callback-based async operations

### ImageLoader
- **Purpose:** Centralized image loading (wraps Glide)
- **Usage:** All image loading must route through this singleton
- **Features:** Caching, placeholder, error handling

### ImageCompressor
- **Purpose:** Compress images before upload
- **Compression Rules:**
  - Max dimension: 1280px
  - Primary quality: 80% (target < 200KB)
  - Fallback quality: 60% if still > 200KB
- **Output:** JPEG format only

### SignalManager
- **Purpose:** User feedback (Toast, Vibration)
- **Usage:** All Toast calls route through `SignalManager.getInstance().toast()`
- **Features:** Toast display, vibration

### SharedPreferencesManager
- **Purpose:** Local persistent storage
- **Storage:** User session data, preferences
- **Serialization:** Gson for complex objects

### TimeFormatter
- **Purpose:** Format timestamps for display
- **Features:** Relative time formatting, deadline formatting

### HomeQuestMessagingService
- **Purpose:** FCM push notification handler
- **Type:** Android Service (extends FirebaseMessagingService)
- **Responsibilities:**
  - Receive push notifications
  - Display notifications
  - Handle notification taps

### Constants
- **Purpose:** Centralized string and numeric constants
- **Pattern:** Nested object groups (no hardcoded strings in code)
- **Categories:** API keys, collection names, status values, limits

---

## Callback Interfaces (4)

All inter-component callbacks use named interfaces in `interfaces/` package (per L08 standards).

### AuthCallback
- **Purpose:** Authentication operation results
- **Methods:** `onSuccess()`, `onFailure(error)`

### QuestCallback
- **Purpose:** Quest operation results
- **Methods:** Quest CRUD callbacks

### CouponCallback
- **Purpose:** Coupon operation results
- **Methods:** Purchase, redeem callbacks

### FeedCallback
- **Purpose:** Feed loading results
- **Methods:** Feed data callbacks

---

## UI Patterns

### ViewBinding
- **Status:** Enabled (mandatory per L08 standards)
- **Usage:** All Activities and Adapters use ViewBinding
- **Pattern:** `findViews()` and `initViews()` called from `onCreate()`

### XML View ID Convention
- **Format:** `{screen}_{WIDGET}_{descriptor}`
- **Example:** `main_BTN_quests`, `quest_list_RV_quests`

### Lifecycle Management
- **Firestore Listeners:** Stored as `ListenerRegistration`, removed in `onPause()`
- **Memory Safety:** Proper cleanup to prevent leaks

---

## Design System

### Material Design
- **Version:** 1.13.0
- **Theme:** `Theme.HomeQuest`
- **Components:** Material buttons, cards, text fields

### Layout Components
- **ConstraintLayout:** Primary layout system
- **RecyclerView:** All lists (quests, coupons, feed, leaderboard)
- **CardView:** Item containers

---

## Reusable Components

### Singleton Utilities
All utilities are reusable across the app:
- `ImageLoader` - Use for all image loading
- `SignalManager` - Use for all user feedback
- `FirebaseManager` - Use for all Firebase operations
- `ImageCompressor` - Use for all image uploads

### Adapters
Adapters follow consistent pattern:
- ViewHolder pattern with ViewBinding
- Click listeners via callbacks
- Data binding in `onBindViewHolder()`

---

## Component Dependencies

### Activity → Utility Dependencies
- All Activities → `FirebaseManager` (data operations)
- All Activities → `SignalManager` (user feedback)
- Activities with images → `ImageLoader` (image display)
- Upload screens → `ImageCompressor` (image processing)

### Activity → Adapter Dependencies
- `MainActivity` → `FeedAdapter`
- `QuestListActivity` → `QuestAdapter`
- `RewardsActivity` → `CouponAdapter`
- `ProfileActivity` → `LeaderboardAdapter`

---

## Testing Components

**Location:** `app/src/test/` and `app/src/androidTest/`

**Test Types:**
- Unit tests: JUnit 4.13.2
- UI tests: Espresso 3.6.1
- Instrumentation runner: AndroidJUnitRunner

**Note:** No test files currently detected in quick scan.

---

## Related Documentation

- [Architecture - Mobile](./architecture-mobile.md) _(To be generated)_
- [Data Models - Mobile](./data-models-mobile.md)
- [Development Guide - Mobile](./development-guide-mobile.md) _(To be generated)_
- [QA Checklist](./QA_CHECKLIST.md) - Existing validation checklist
