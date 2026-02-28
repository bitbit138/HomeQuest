# Project Overview - HomeQuest

**Generated:** 2026-02-24  
**Version:** 1.0.0

---

## Executive Summary

**HomeQuest** is a gamified household task management mobile application that transforms everyday chores into quests with rewards. Family members earn XP and coins by completing tasks, level up through progression, and redeem coins for real-world rewards in a household marketplace. The app features real-time activity feeds, leaderboards, and a proof-of-completion system using photo verification.

**Project Type:** Multi-part mobile application with serverless backend  
**Primary Users:** Families and households looking to gamify chore management  
**Platform:** Android (native)  
**Backend:** Firebase (serverless)

---

## Project Purpose

HomeQuest solves the problem of household task management and motivation by:
- **Gamification:** Converting chores into quests with XP and coin rewards
- **Accountability:** Photo proof requirement for task completion
- **Motivation:** Level progression and reward marketplace
- **Engagement:** Real-time activity feed and household leaderboard
- **Collaboration:** Shared household with invite code system

---

## Technology Stack Summary

### Mobile App (Android)
- **Language:** Kotlin 2.0.21
- **Platform:** Android API 26-36
- **UI:** Material Design with ViewBinding
- **Architecture:** Activity-based MVC with singleton utilities
- **Backend SDK:** Firebase (Auth, Firestore, Storage, FCM)

### Backend (Firebase)
- **Language:** TypeScript 5.0.4
- **Runtime:** Node.js 20
- **Platform:** Firebase Cloud Functions (serverless)
- **Database:** Firestore (NoSQL)
- **Storage:** Firebase Storage
- **Auth:** Firebase Authentication

---

## Architecture Classification

### Repository Structure
**Type:** Multi-part  
**Parts:** 2 (Mobile + Backend)

**Part 1: Android Mobile App**
- **Location:** `app/`
- **Type:** Mobile (Android native)
- **Entry Point:** `app/src/main/java/dev/tombit/homequest/App.kt`

**Part 2: Firebase Backend**
- **Location:** `firebase/`
- **Type:** Backend (Serverless)
- **Entry Point:** `firebase/functions/src/index.ts`

### Integration Pattern
**Firebase-Mediated Architecture:**
- No direct HTTP/REST calls between parts
- All communication through Firebase services
- Real-time synchronization via Firestore
- Event-driven Cloud Functions

---

## Key Features

### 1. User Authentication
- Email/password sign-in via Firebase Auth
- Household creation with unique invite code
- Household joining via invite code lookup

### 2. Quest System
- Create household quests with XP and coin rewards
- Claim available quests
- Upload photo proof of completion
- Server-side approval and reward distribution
- Status lifecycle: open → claimed → pending_verification → completed

### 3. Reward Marketplace
- Users list rewards (coupons) with coin cost
- Purchase rewards with earned coins
- Redemption tracking
- Concurrent purchase protection

### 4. Gamification
- XP and level progression system
- Coin-based virtual economy
- Household leaderboard (sorted by XP)
- Real-time activity feed

### 5. Real-time Updates
- Activity feed with live updates
- Quest list updates
- Push notifications via FCM

---

## Technical Highlights

### L08 Coding Standards
The mobile app follows strict L08 academic coding conventions:
- Mandatory ViewBinding for all Activities
- Thread-safe singleton pattern with `@Volatile`
- Builder pattern for all data models
- Callback interfaces for async operations
- Centralized constants (no hardcoded strings)
- Standardized XML view ID naming
- Mandatory `findViews()` and `initViews()` in Activities

### Security Features
- Protected fields (coins, XP, level) server-controlled only
- Firestore security rules enforce household isolation
- No client-side deletions (append-only from client)
- Storage rules enforce JPEG format and 200KB max size
- Immutable activity feed (no updates/deletes)

### Performance Optimizations
- Firestore offline persistence
- Composite indexes for complex queries
- Image compression (< 200KB JPEG)
- Glide image caching
- Leaderboard caching (60 seconds)
- Feed pagination (20 items per page)
- Scheduled feed pruning (daily)

---

## Project Statistics

### Mobile App
- **Activities:** 10 (1 splash, 2 auth, 7 core)
- **Data Models:** 5 (User, Household, Task, Coupon, FeedItem)
- **Adapters:** 4 (Quest, Coupon, Feed, Leaderboard)
- **Utilities:** 8 singletons
- **Callback Interfaces:** 4
- **Layouts:** 14 XML files
- **Min SDK:** API 26 (Android 8.0)
- **Target SDK:** API 36

### Backend
- **Cloud Functions:** 3+ expected (reward approval, feed pruning, transactions)
- **Security Rules:** Firestore + Storage
- **Composite Indexes:** 6
- **Runtime:** Node.js 20
- **Language:** TypeScript 5.0.4

---

## Documentation Structure

### Generated Documentation
- [Architecture - Mobile](./architecture-mobile.md)
- [Architecture - Backend](./architecture-backend.md)
- [Integration Architecture](./integration-architecture.md)
- [Data Models - Mobile](./data-models-mobile.md)
- [Component Inventory - Mobile](./component-inventory-mobile.md)
- [Development Guide - Mobile](./development-guide-mobile.md)
- [Development Guide - Backend](./development-guide-backend.md)
- [Source Tree Analysis](./source-tree-analysis.md)

### Existing Documentation
- [QA Checklist](./QA_CHECKLIST.md) - Pre-launch validation checklist

---

## Getting Started

### For Developers

**Mobile Development:**
1. Open project in Android Studio
2. Sync Gradle dependencies
3. Ensure `google-services.json` is configured
4. Run on device/emulator
5. See [Development Guide - Mobile](./development-guide-mobile.md)

**Backend Development:**
1. Install Firebase CLI: `npm install -g firebase-tools`
2. Install dependencies: `cd firebase/functions && npm install`
3. Start emulators: `firebase emulators:start`
4. Deploy functions: `firebase deploy --only functions`
5. See [Development Guide - Backend](./development-guide-backend.md)

### For AI-Assisted Development

**When planning new features:**
1. Start with [Architecture - Mobile](./architecture-mobile.md) and [Architecture - Backend](./architecture-backend.md)
2. Review [Data Models](./data-models-mobile.md) for schema understanding
3. Check [Integration Architecture](./integration-architecture.md) for communication patterns
4. Reference [Component Inventory](./component-inventory-mobile.md) for reusable components
5. Follow L08 coding standards (see [QA Checklist](./QA_CHECKLIST.md))

**For UI features:** Focus on mobile architecture and component inventory  
**For backend features:** Focus on backend architecture and Cloud Functions patterns  
**For full-stack features:** Review integration architecture and data flow

---

## Related Documentation

- [Master Index](./index.md) _(To be generated)_ - Primary navigation
- [Architecture - Mobile](./architecture-mobile.md)
- [Architecture - Backend](./architecture-backend.md)
- [Integration Architecture](./integration-architecture.md)
