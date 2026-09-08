# Home Dreamlike Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Give the SYLORA home screen a distinctive night-navigation atmosphere while preserving existing message, unread, avatar, and navigation behavior.

**Architecture:** Keep `HomeViewModel` and all persistence untouched. Add a small presentation-only visual layer inside `ImmersiveHomeScreen.kt`: an atmospheric header, bounded orbit decoration, clearer unread treatment, and a warmer empty state. Reuse existing Compose theme tokens and animation helpers.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, existing SYLORA design system, Gradle unit tests and Android lint.

## Global Constraints

- Do not change message queries, unread timestamps, database schema, or navigation callbacks.
- Keep key content readable at 320dp width and avoid large decorative gradients.
- Preserve per-character avatar aura colors and online ring behavior.
- Keep motion low-amplitude and compatible with the existing stagger entrance.

### Task 1: Add the night-navigation presentation layer

**Files:**
- Modify: `app/src/main/java/com/companion/cc/ui/home/ImmersiveHomeScreen.kt`

**Interfaces:**
- Consumes: existing `HomeUiState`, `HomeCharacterItem`, theme tokens, avatar resolver, and navigation callbacks.
- Produces: the same `ImmersiveHomeScreen` and `ChatListItem` public composables with unchanged callback signatures.

- [ ] Replace the generic header copy with a short SYLORA night-navigation title and a restrained supporting status line.
- [ ] Add a clipped, low-contrast orbit decoration behind the content without intercepting clicks.
- [ ] Update empty-state copy and icon treatment while keeping `onNavigateToCharacterList` unchanged.
- [ ] Add a small unread light marker inside the existing card content, preserving `hasUnreadMessage` as the only source of truth.
- [ ] Keep the existing avatar and online ring behavior intact.

### Task 2: Verify presentation states

**Files:**
- Inspect: `app/src/test/java/com/companion/cc/ui/home/HomeViewModelTest.kt`
- Inspect: `app/src/test/java/com/companion/cc/ui/home/HomeMessageNoticeTest.kt`

**Interfaces:**
- Consumes: unchanged home state projection and unread helper behavior.
- Produces: verification evidence that behavior is unchanged while the visual layer changes.

- [ ] Run the focused home tests and confirm unread projection and ordering remain green.
- [ ] Build the debug APK and use the emulator to inspect populated, unread, and empty home states.
- [ ] Confirm no text overlap, clipped controls, or repeated full-screen entrance animation at 320dp and normal emulator dimensions.

### Task 3: Repository verification

**Files:**
- No additional source files.

- [ ] Run `D:\CC-Switch\cc-native-android\gradlew.bat -p D:\CC-Switch\cc-native-android :app:testDebugUnitTest`.
- [ ] Run `D:\CC-Switch\cc-native-android\gradlew.bat -p D:\CC-Switch\cc-native-android :app:lintDebug`.
- [ ] Run `D:\CC-Switch\cc-native-android\gradlew.bat -p D:\CC-Switch\cc-native-android :app:assembleDebug`.
- [ ] Review `git diff` and leave unrelated files untouched.
