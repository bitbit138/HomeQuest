---
stepsCompleted: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]
inputDocuments:
  - docs/index.md
  - docs/project-overview.md
  - docs/architecture-mobile.md
  - docs/component-inventory-mobile.md
  - docs/data-models-mobile.md
---

# UX Design Specification — HomeQuest

**Author:** Tombitran  
**Date:** 2026-02-24  
**Design Direction:** Gamified • Cozy • Farm House Style • Very Convenient

---

## Executive Summary

### Project Vision

HomeQuest transforms household chores into engaging quests with rewards. The app should feel like a warm, welcoming farmhouse where families turn everyday tasks into shared adventures. The experience combines gamification (XP, coins, levels, leaderboards) with a cozy, approachable aesthetic that makes task management feel rewarding rather than burdensome.

### Target Users

- **Primary:** Families and households (parents, children, roommates) who want to gamify chore management
- **Context:** Users juggle busy schedules and need motivation to complete household tasks
- **Tech comfort:** Mixed—from children to adults; must be intuitive and low-friction
- **Usage:** Mobile-first (Android), often used in short bursts (claim quest, snap proof, redeem reward)

### Key Design Challenges

1. **Balancing gamification with simplicity** — Rich rewards and progression without overwhelming casual users
2. **Family-friendly hierarchy** — Clear information for different ages and roles
3. **Quick, low-friction flows** — Claim quest, upload proof, redeem reward in minimal taps
4. **Cozy vs. functional** — Farm house aesthetic without sacrificing clarity and usability

### Design Opportunities

1. **Quest-as-adventure framing** — Visual language that makes chores feel like achievements
2. **Warm, tactile feedback** — Satisfying animations and micro-interactions for completions
3. **Household identity** — Shared “home” feeling through consistent farm house styling
4. **Convenience-first patterns** — One-tap claims, fast proof upload, quick reward redemption

---

## Core User Experience

### Defining Experience

**Core action:** Claim a quest → complete it → upload proof → earn rewards. This loop should be effortless and feel rewarding. Secondary flows: browse quests, check leaderboard, redeem coins for rewards.

**Primary interaction:** Touch-based mobile; quick access to key actions (claim, upload, redeem) with minimal navigation.

### Platform Strategy

- **Platform:** Android native (API 26–36)
- **Input:** Touch-first; large tap targets for children and one-handed use
- **Offline:** Firestore persistence for viewing; core actions require connectivity
- **Capabilities:** Camera for proof, push notifications for quest reminders and approvals

### Effortless Interactions

- **Claim quest:** Single tap from list; no confirmation dialogs for standard claims
- **Upload proof:** Camera or gallery with one-tap selection; automatic compression
- **Redeem reward:** One-tap purchase when balance is sufficient; clear feedback
- **Activity feed:** Auto-refresh; no manual pull-to-refresh required for normal use

### Critical Success Moments

1. **First quest completed** — Clear XP/coin gain and level-up feedback
2. **Proof approved** — Celebration moment with reward summary
3. **Reward redeemed** — Satisfaction of “cashing in” earned coins
4. **Leaderboard view** — Friendly competition without pressure

### Experience Principles

1. **Convenience first** — Minimize taps and steps for core actions
2. **Reward visibility** — XP and coins always visible; progress always clear
3. **Cozy and approachable** — Warm, farm house aesthetic; no cold or corporate feel
4. **Family-friendly** — Readable, accessible, safe for all ages

---

## Desired Emotional Response

### Primary Emotional Goals

- **Accomplishment** — “I did it!” when completing a quest
- **Warmth** — “This feels like home” from the visual and interaction design
- **Playfulness** — Light gamification without pressure
- **Trust** — Fair rules, clear rewards, transparent progress

### Emotional Journey Mapping

| Stage | Desired Feeling |
|-------|-----------------|
| First open | Welcoming, easy to understand |
| Browsing quests | Curiosity, “I can do that” |
| Claiming quest | Commitment, slight excitement |
| Completing & uploading | Pride, anticipation |
| Approval & rewards | Satisfaction, achievement |
| Redeeming reward | Delight, “worth it” |
| Viewing leaderboard | Friendly competition, belonging |

### Micro-Emotions

- **Confidence** over confusion — Clear labels, obvious next steps
- **Excitement** over anxiety — Positive framing, no punitive language
- **Belonging** over isolation — Shared household, visible family activity

### Design Implications

- **Accomplishment** → Celebration animations, clear reward summaries, progress indicators
- **Warmth** → Farm house palette, soft shapes, organic imagery
- **Playfulness** → Light gamification UI (badges, levels, coins) without clutter
- **Trust** → Transparent rules, consistent feedback, no hidden mechanics

### Emotional Design Principles

1. Celebrate every completion
2. Use warm, natural colors and textures
3. Keep gamification light and optional-feeling
4. Make household members feel part of a team

---

## UX Pattern Analysis & Inspiration

### Inspiring Products Analysis

| Product | UX Strength | Transferable Pattern |
|---------|-------------|----------------------|
| **Duolingo** | Streaks, XP, levels; low-pressure gamification | Progress bars, celebratory feedback, simple core loop |
| **Habitica** | RPG-style tasks and rewards | Quest framing, avatar/level display |
| **Cozy games (Stardew Valley, Animal Crossing)** | Warm, farm-life aesthetic | Organic colors, soft UI, cozy typography |
| **Notion / Todoist** | Quick task capture | Fast input, minimal friction |
| **Venmo** | Social feed of transactions | Activity feed as social proof |

### Transferable UX Patterns

**Navigation:** Bottom nav for primary areas (Home, Quests, Rewards, Profile); tab or list for sub-views.

**Gamification:** Persistent XP/coin display; level badges; subtle animations on reward events.

**Cozy aesthetic:** Rounded corners, warm neutrals, wood/grain accents, soft shadows.

**Convenience:** Swipe actions, large buttons, one-tap primary actions, smart defaults.

### Anti-Patterns to Avoid

- Cluttered dashboards with too many metrics
- Cold, corporate color schemes
- Multi-step confirmations for routine actions
- Hidden or unclear reward rules
- Punitive or shaming language

### Design Inspiration Strategy

**Adopt:** Duolingo-style celebration feedback, Cozy game color palette, Habitica quest framing.  
**Adapt:** Farm house styling for mobile; simplified level/XP display for families.  
**Avoid:** Overly complex RPG mechanics; dark or aggressive themes.

---

## Design System Foundation

### Design System Choice

**Material Design 3 (Material You) — themed for Farm House**

- Use Material 3 as base for components, accessibility, and patterns
- Apply custom Farm House theme (colors, typography, shapes)
- Retain Material’s touch targets and motion guidelines

### Rationale for Selection

- Android native; Material aligns with platform expectations
- Strong accessibility and component library
- Themeable; supports custom farm house look
- Familiar to Android users; reduces learning curve

### Implementation Approach

- Use Material 3 components (buttons, cards, chips, FABs)
- Override theme with Farm House tokens
- Add custom illustrations/icons for quests and rewards
- Use Lottie for celebration and feedback animations

### Customization Strategy

- **Colors:** Warm earth tones, cream, sage, barn red, wheat
- **Typography:** Friendly sans-serif (e.g., Nunito, Quicksand) for headings; readable body
- **Shapes:** Rounded corners (16–24dp), soft card elevation
- **Icons:** Outlined, slightly playful; farm/quest metaphors where helpful

---

## Visual Design Foundation

### Color System — Farm House Palette

| Role | Color | Hex | Usage |
|------|-------|-----|-------|
| **Primary** | Barn Red | `#8B4513` | CTAs, key actions, level badges |
| **Secondary** | Sage Green | `#6B8E6B` | Success, completed states, nature accents |
| **Background** | Cream | `#F5F0E8` | Main backgrounds |
| **Surface** | Warm White | `#FFFBF5` | Cards, sheets |
| **Accent** | Wheat Gold | `#D4A84B` | Coins, rewards, highlights |
| **Text Primary** | Warm Brown | `#3D2C29` | Body text |
| **Text Secondary** | Muted Brown | `#6B5B57` | Secondary text |
| **Error** | Soft Red | `#C45C4A` | Errors, warnings |

**Gamification accents:**
- **XP/Level:** Sage green gradient
- **Coins:** Wheat gold with subtle shine
- **Quest card:** Cream card with sage border when available, warm brown when claimed

### Typography System

| Element | Font | Size | Weight | Use |
|---------|------|------|--------|-----|
| **H1** | Nunito | 24sp | Bold | Screen titles |
| **H2** | Nunito | 20sp | SemiBold | Section headers |
| **H3** | Nunito | 16sp | SemiBold | Card titles |
| **Body** | Nunito | 14sp | Regular | Body text |
| **Caption** | Nunito | 12sp | Regular | Timestamps, hints |
| **Button** | Nunito | 14sp | SemiBold | Buttons |

- **Tone:** Friendly, readable, slightly playful
- **Accessibility:** Min 14sp body; contrast ≥ 4.5:1 for text

### Spacing & Layout Foundation

- **Base unit:** 8dp
- **Scale:** 8, 16, 24, 32, 48dp
- **Card padding:** 16dp
- **Screen padding:** 16dp horizontal, 24dp vertical
- **Density:** Comfortable; avoid cramped layouts

### Accessibility Considerations

- Touch targets ≥ 48dp
- Color not sole indicator (icons + text)
- Support for system font scaling
- Sufficient contrast for all text

---

## Design Direction Decision

### Design Directions Explored

**Chosen: Cozy Farm House with Gamified Accents**

- Warm, cream-based backgrounds
- Rounded cards with soft shadows
- Quest cards styled like “task slips” or “chore cards”
- Persistent XP/coin bar at top or in nav
- Activity feed as a “family bulletin board”
- Reward marketplace as a “farm stand” or “pantry”

### Chosen Direction

**Farm House Quest**

- Primary layout: Feed-first home; quest list as main work area
- Navigation: Bottom nav (Home, Quests, Rewards, Profile)
- Gamification: Compact XP/coin bar; level badge on profile
- Cards: Cream background, sage/wheat accents, rounded corners

### Design Rationale

- Farm house style supports “home” and “family” mental model
- Gamification is visible but not dominant
- Warm palette reduces friction and feels inviting
- Familiar Material patterns keep interactions predictable

### Implementation Approach

- Implement Farm House theme in `themes.xml`
- Create custom drawables for quest/reward cards
- Add Lottie animations for quest complete, level up, reward redeem
- Use consistent 16dp corner radius on cards

---

## User Journey Flows

### Journey 1: Claim and Complete a Quest

1. **Home** → See activity feed; tap “Quests” in nav
2. **Quest List** → Browse open quests; tap one to view details
3. **Quest Detail** → Tap “Claim” (one tap)
4. **Complete task** (off-app)
5. **Quest Detail** → Tap “Upload proof” → Camera or gallery → Select/capture → Auto-upload
6. **Feedback** → “Submitted for approval” toast; return to home or list
7. **Later** → Push notification on approval; open app to see XP/coins

**Convenience:** Claim and upload in 2–3 taps; no extra confirmations.

### Journey 2: Redeem a Reward

1. **Home** or **Rewards** → Tap “Rewards” in nav
2. **Rewards List** → See available (marketplace) and owned
3. **Tap reward** → Detail with cost and “Purchase” or “Redeem”
4. **Purchase** → One tap if balance sufficient; immediate feedback
5. **Redeem** → Seller marks redeemed; buyer sees updated state

**Convenience:** Purchase in 2 taps when balance is enough.

### Journey 3: Create a Quest (Household Admin)

1. **Quest List** → FAB or “Create quest”
2. **Create form** → Title, description (optional), XP, coins, deadline
3. **Submit** → Quest appears in list for household

**Convenience:** Form with smart defaults; optional fields collapsible.

### Journey 4: View Leaderboard and Profile

1. **Profile** → Stats (level, XP, coins), leaderboard, settings
2. **Leaderboard** → Rank, name, level, XP; friendly framing

**Convenience:** Single tap to profile; leaderboard in same screen or one tap.

---

## UX Patterns & Component Strategy

### Navigation Pattern

- **Bottom navigation:** Home, Quests, Rewards, Profile (4 items)
- **Home:** Activity feed + quick stats (XP, coins)
- **Quests:** List with filters (Open / Mine / All)
- **Rewards:** Marketplace + My Rewards
- **Profile:** Stats, leaderboard, household, sign out

### Quest Card Pattern

- **Layout:** Title, rewards (XP + coins), deadline, status
- **States:** Open (claim CTA), Claimed (upload proof CTA), Pending (waiting), Completed (checkmark)
- **Style:** Cream card, rounded corners, sage border when available

### Reward Card Pattern

- **Layout:** Title, cost (coins), seller, Purchase/Redeem CTA
- **States:** Available, Purchased (by me), Redeemed

### Feed Item Pattern

- **Layout:** Avatar, user name, message, timestamp
- **Style:** Compact row; “bulletin board” feel with subtle card or divider

### Convenience Patterns

| Pattern | Implementation |
|---------|----------------|
| **One-tap claim** | No confirmation for standard claim |
| **Fast proof upload** | Direct camera/gallery; single selection |
| **Smart defaults** | Pre-filled XP/coins when creating quest |
| **Persistent stats** | XP/coins in app bar or nav |
| **Swipe actions** | Optional: swipe to claim (future) |
| **Pull-to-refresh** | On lists; optional on feed |

---

## Responsive & Accessibility

### Responsive Behavior

- **Primary:** Phone (360–428dp width)
- **Layout:** Single column; cards full-width with padding
- **Orientation:** Portrait primary; landscape supported

### Accessibility

- Touch targets ≥ 48dp
- Labels for all interactive elements
- Sufficient color contrast (≥ 4.5:1)
- Support dynamic type / font scaling
- Focus order logical for keyboard/switch access

---

## Summary: Design Principles

1. **Gamified** — Visible XP, coins, levels; celebration on completions; friendly leaderboard
2. **Cozy** — Warm farm house palette, soft shapes, welcoming copy
3. **Farm House Style** — Cream, sage, barn red, wheat; organic, homey feel
4. **Very Convenient** — One-tap claims, fast proof upload, minimal steps, clear feedback

---

## Implementation Reference

### Existing Alignment

The app already uses a **Farm House palette** in `app/src/main/res/values/colors.xml`:
- Terracotta primary (`hq_purple_primary`), sage secondary (`hq_teal_secondary`)
- Cream backgrounds (`hq_background`, `hq_surface`)
- Gold for coins (`hq_coin`, `hq_gold`)
- Material 3 theme in `themes.xml` with `Theme.HomeQuest`

### Recommended Next Steps

1. **Typography:** Add Nunito or similar friendly font; update `themes.xml` `fontFamily`
2. **Convenience:** Audit quest claim/upload flows for one-tap patterns; remove unnecessary confirmations
3. **Celebration:** Add Lottie animations for quest complete, level up, reward redeem
4. **Card styling:** Apply `HomeQuest.Card` consistently; use sage border for open quests
5. **Stats bar:** Ensure XP/coins are always visible (e.g., in app bar or nav)

---

*UX design specification for HomeQuest. Ready for implementation.*
