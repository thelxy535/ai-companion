# Chat IME Insets Implementation Plan

> **For agentic workers:** Execute inline in the current session. Do not create a worktree, commit, push, change version values, clear app data, or run connected instrumentation tests.

**Goal:** Keep the natural chat input bar visible directly above the Android soft keyboard, while the message list continues to avoid the moved input bar.

**Architecture:** `MainActivity` already relies on `adjustResize`, so the fix remains local to `NaturalChatScreen`. The `Scaffold.bottomBar` wrapper will consume the IME bottom inset; because the bottom bar's measured height grows while the keyboard is visible, the existing `Scaffold` content padding continues to reserve matching message-list space.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android WindowInsets, Gradle JVM tests.

## Global Constraints

- Modify only `app/src/main/java/com/companion/cc/ui/chat/NaturalChatScreen.kt` for behavior.
- Do not modify the server vision gateway, Room/DataStore data, AndroidManifest soft-input mode, application version values, or `ImmersiveChatScreen.kt`.
- Preserve image selection, voice controls, draft persistence, send behavior, top bar, and message rendering.
- `AndroidManifest.xml` already declares `android:windowSoftInputMode="adjustResize"`; do not duplicate or replace it.
- Do not commit or push.
- Run Gradle tasks serially because concurrent Gradle/Kotlin jobs have previously corrupted build outputs.
- Do not run connected instrumentation tests because installed debug data must be preserved.

---

### Task 1: Make the natural chat bottom bar IME-aware

**Files:**
- Modify: `app/src/main/java/com/companion/cc/ui/chat/NaturalChatScreen.kt:220-269`
- Test: Manual device validation only; Compose IME position is a platform-window integration behavior and no connected instrumentation run is permitted.

**Interfaces:**
- Consumes: existing Material 3 `Scaffold.bottomBar` and `ChatInputBar(...)`.
- Produces: a bottom bar that reserves `WindowInsets.ime` space while the keyboard is visible, without changing `ChatInputBar` arguments or message-send callbacks.

- [ ] **Step 1: Establish the pre-change evidence**

Confirm the existing bottom-bar wrapper has no IME inset modifier and that the activity already uses `adjustResize`:

```kotlin
bottomBar = {
    Column {
        // existing indicators and ChatInputBar
    }
}
```

Expected evidence: `NaturalChatScreen.kt` has no `imePadding()` call; `AndroidManifest.xml` contains `android:windowSoftInputMode="adjustResize"`.

- [ ] **Step 2: Implement the minimal modifier change**

Apply `imePadding()` to the `Column` that is the direct `Scaffold.bottomBar` content:

```kotlin
bottomBar = {
    Column(
        modifier = Modifier.imePadding()
    ) {
        // existing VoiceListeningIndicator, TTSSpeakingIndicator, and ChatInputBar unchanged
    }
}
```

Do not apply IME padding to the `LazyColumn`, `ChatInputBar`, or whole `Scaffold`; applying it there can either double-apply space or leave the Scaffold content padding unaware of the bar's changed height.

- [ ] **Step 3: Compile the change**

Run serially:

```powershell
Set-Location "D:\CC-Switch\cc-native-android"
& .\gradlew.bat :app:compileDebugKotlin --no-parallel
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Run JVM regression tests and package the debug APK**

Run serially:

```powershell
Set-Location "D:\CC-Switch\cc-native-android"
& .\gradlew.bat :app:testDebugUnitTest --no-parallel
& .\gradlew.bat :app:assembleDebug --no-parallel
git diff --check
```

Expected: all JVM tests pass, debug APK is produced, and whitespace validation has no errors.

- [ ] **Step 5: Perform manual device acceptance without clearing data**

Install the debug APK as an update over the existing debug app. In a normal and a custom-character chat:

1. Focus the text field with a short message.
2. Confirm the entire input bar, image button, microphone button, and send button move above the keyboard.
3. Confirm the newest message remains above the input bar rather than behind it.
4. Type multiple lines until the field expands, then dismiss the keyboard and verify the bar returns to its original position.
5. Select and clear an image, use voice input, send a text message, and confirm existing controls behave unchanged.

Expected: no content is obscured by the keyboard and no existing chat action regresses.
