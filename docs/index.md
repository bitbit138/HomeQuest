# HomeQuest - Project Documentation Index

**Generated:** 2026-02-24  
**Project Version:** 1.0.0  
**Documentation Version:** 1.2.0

---

## Project Overview

**HomeQuest** is a gamified household task management mobile application with a serverless Firebase backend.

- **Repository Type:** Multi-part (2 parts)
- **Primary Language:** Kotlin (Mobile) + TypeScript (Backend)
- **Architecture:** Firebase-mediated (Activity-based MVC + Serverless Cloud Functions)

---

## Quick Reference

### Part 1: Android Mobile App
- **Type:** Mobile (Android native)
- **Tech Stack:** Kotlin 2.0.21, Android SDK API 26-36, Material Design, Firebase SDK
- **Root:** `app/`
- **Entry Point:** `app/src/main/java/dev/tombit/homequest/App.kt`
- **Package:** `dev.tombit.homequest`

### Part 2: Firebase Backend
- **Type:** Backend (Serverless)
- **Tech Stack:** TypeScript 5.0.4, Node.js 20, Firebase Cloud Functions
- **Root:** `firebase/`
- **Entry Point:** `firebase/functions/src/index.ts`
- **Services:** Cloud Functions, Firestore, Storage, Auth, FCM

---

## Generated Documentation

### Core Architecture
- [Project Overview](./project-overview.md) - Executive summary and project purpose
- [Architecture - Mobile](./architecture-mobile.md) - Android app architecture
- [Architecture - Backend](./architecture-backend.md) - Firebase backend architecture
- [Integration Architecture](./integration-architecture.md) - How parts communicate
- [Source Tree Analysis](./source-tree-analysis.md) - Complete annotated directory structure

### Mobile App Documentation
- [Component Inventory - Mobile](./component-inventory-mobile.md) - Activities, adapters, utilities
- [Data Models - Mobile](./data-models-mobile.md) - Firestore schema and data models
- [Development Guide - Mobile](./development-guide-mobile.md) - Setup, build, run, test

### Backend Documentation
- [Development Guide - Backend](./development-guide-backend.md) - Functions setup, deployment, testing

### Metadata
- [project-parts.json](./project-parts.json) - Machine-readable project structure

---

## Existing Documentation

- [QA Checklist](./QA_CHECKLIST.md) - Pre-launch QA checklist covering coding standards, data layer, security, performance, and manual test scenarios

---

## Getting Started

### For New Developers

**Mobile Development:**
1. Install Android Studio and JDK 11
2. Open project root in Android Studio
3. Sync Gradle dependencies: `./gradlew build`
4. Configure `google-services.json` with Firebase project
5. Run on device/emulator
6. See [Development Guide - Mobile](./development-guide-mobile.md) for details

**Backend Development:**
1. Install Node.js 20 and Firebase CLI
2. Install dependencies: `cd firebase/functions && npm install`
3. Start emulators: `firebase emulators:start`
4. Deploy functions: `firebase deploy --only functions`
5. See [Development Guide - Backend](./development-guide-backend.md) for details

---

### For AI-Assisted Development

**Planning New Features:**

**UI-Only Features** (e.g., new screen, UI component):
- Reference: [Architecture - Mobile](./architecture-mobile.md)
- Components: [Component Inventory - Mobile](./component-inventory-mobile.md)
- Standards: [QA Checklist](./QA_CHECKLIST.md) - L08 coding conventions

**Backend-Only Features** (e.g., new Cloud Function, scheduled task):
- Reference: [Architecture - Backend](./architecture-backend.md)
- Security: [Data Models - Mobile](./data-models-mobile.md) - Firestore rules
- Guide: [Development Guide - Backend](./development-guide-backend.md)

**Full-Stack Features** (e.g., new quest type, reward system):
- Reference: [Integration Architecture](./integration-architecture.md)
- Mobile: [Architecture - Mobile](./architecture-mobile.md)
- Backend: [Architecture - Backend](./architecture-backend.md)
- Data: [Data Models - Mobile](./data-models-mobile.md)

**Understanding Data Flow:**
- Start: [Integration Architecture](./integration-architecture.md)
- Schema: [Data Models - Mobile](./data-models-mobile.md)
- Security: [QA Checklist](./QA_CHECKLIST.md) - Section 11.3

**Exploring Codebase:**
- Structure: [Source Tree Analysis](./source-tree-analysis.md)
- Components: [Component Inventory - Mobile](./component-inventory-mobile.md)

---

## Key Architectural Decisions

### Why Firebase?
- **Serverless:** No server management, automatic scaling
- **Real-time:** Built-in real-time sync for activity feed
- **Offline:** Automatic offline persistence
- **Security:** Declarative security rules
- **Integration:** Single SDK for auth, database, storage, messaging

### Why Activity-Based (Not Jetpack Compose)?
- **L08 Standards:** Academic project following specific conventions
- **ViewBinding:** Mandatory per coding standards
- **Traditional Pattern:** Proven, stable, well-documented

### Why Singleton Utilities?
- **L08 Requirement:** Specific pattern mandated by coding standards
- **Centralization:** Single point of access for cross-cutting concerns
- **Thread Safety:** Double-check locking ensures safe initialization

---

## Project Conventions

### L08 Coding Standards (Mandatory)

**ViewBinding:**
- Enabled in all Activities and Adapters
- `findViews()` and `initViews()` methods required

**Singletons:**
- Thread-safe double-check locking with `@Volatile`
- `WeakReference<Context>` (no direct Context storage)
- Initialized in `App.kt` only

**Data Models:**
- Private constructor + inner Builder class
- Mirror Firestore field names exactly

**Callbacks:**
- Named interfaces in `interfaces/` package
- Used for all async operations

**Constants:**
- Centralized in `Constants.kt` with nested objects
- No hardcoded strings in Kotlin files

**User Feedback:**
- All Toast calls via `SignalManager.getInstance().toast()`

**Image Loading:**
- All image loading via `ImageLoader.getInstance().loadImage()`

**XML View IDs:**
- Format: `{screen}_{WIDGET}_{descriptor}`
- Example: `main_BTN_quests`, `quest_list_RV_quests`

See [QA Checklist](./QA_CHECKLIST.md) Section 11.1 for complete standards.

---

## Security Considerations

### Protected Operations (Server-Only)
- Updating `coinBalance`, `currentXp`, `level`
- Deleting any documents
- Modifying activity feed entries

### Client Responsibilities
- Input validation
- Image compression before upload
- Proper authentication state management

### Server Responsibilities
- Reward distribution
- Balance updates
- Security rule enforcement
- Transaction atomicity

---

## Performance Notes

- **Offline First:** Firestore persistence enabled
- **Image Optimization:** All images compressed to < 200KB JPEG
- **Query Optimization:** Composite indexes for all complex queries
- **Caching:** Leaderboard cached 60s, images cached by Glide
- **Pagination:** Feed limited to 20 items per load

---

## Next Steps

1. **Review Documentation:** Familiarize yourself with the architecture and components
2. **Setup Development Environment:** Follow development guides for mobile and backend
3. **Run Local Tests:** Use Firebase emulators for integration testing
4. **Plan New Features:** Use this documentation as context for brownfield PRD workflow
5. **Validate Security:** Run security tests from QA checklist before production

---

## Documentation Maintenance

**Last Updated:** 2026-02-24  
**Scan Level:** Quick (pattern-based, no source file reading)  
**State File:** [project-scan-report.json](./project-scan-report.json)

**To Update Documentation:**
- Run `/bmad-bmm-document-project` command
- Choose "Re-scan entire project" to update all docs
- Choose "Deep-dive" for detailed analysis of specific areas

---

## Support

For questions about:
- **Mobile Architecture:** See [Architecture - Mobile](./architecture-mobile.md)
- **Backend Architecture:** See [Architecture - Backend](./architecture-backend.md)
- **Integration Patterns:** See [Integration Architecture](./integration-architecture.md)
- **Development Setup:** See development guides for [Mobile](./development-guide-mobile.md) or [Backend](./development-guide-backend.md)
- **Coding Standards:** See [QA Checklist](./QA_CHECKLIST.md)
