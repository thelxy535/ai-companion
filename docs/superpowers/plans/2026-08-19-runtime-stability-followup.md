# Runtime Stability Follow-up Implementation Plan

> **For agentic workers:** Execute these tasks task-by-task with a test/build checkpoint after each behavior change.

**Goal:** Make the current Android build observable and resilient enough to diagnose the menu crash and validate the glass rectangle fix when a device is available.

**Architecture:** Keep navigation callbacks pure and add a small guarded navigation helper at the NavGraph boundary. Keep Room subscriptions as the source of truth. Keep glass rendering clipped at every layer and expose only diagnostic logging, without changing visual behavior based on guessed device state.

**Tech Stack:** Kotlin, Jetpack Compose, Navigation Compose, Hilt, Room, Gradle offline test/build.

## Global Constraints

- Preserve unrelated user changes in the dirty worktree.
- Do not replace routes or database schemas without runtime evidence.
- Verify with `:app:testDebugUnitTest :app:assembleDebug`.

---

### Task 1: Navigation diagnostics and guard

**Files:**
- Modify: `app/src/main/java/com/companion/cc/ui/navigation/NavGraph.kt`
- Test: `app/src/test/java/com/companion/cc/ui/navigation/NavGraphRouteTest.kt`

- [ ] Add a pure route test covering every menu destination's required argument.
- [ ] Add a single `navigateSafely(route: String)` extension/helper that logs route failures and rethrows, preserving the original crash stack.
- [ ] Use it for menu callbacks only.
- [ ] Run the focused test and then the full test/build.

### Task 2: Glass clipping regression coverage

**Files:**
- Modify: `app/src/main/java/com/companion/cc/ui/theme/AdaptiveGlass.kt`
- Test: `app/src/test/java/com/companion/cc/ui/theme/GlassTouchOpticsTest.kt`

- [ ] Add a pure bounds assertion for the backdrop replica offset calculation.
- [ ] Keep clipping after blur and ensure touch gradients are emitted only while pressed.
- [ ] Run glass tests and the full test/build.

### Task 3: Device verification artifact

**Files:**
- Create: `tools/capture-android-crash.ps1`

- [ ] Provide a script that clears logcat, force-stops `com.companion.cc.debug`, launches it, waits for user reproduction, and saves only `FATAL EXCEPTION`, `AndroidRuntime`, and navigation lines to `build/device-crash.log`.
- [ ] Make the script fail clearly when no ADB device is connected.
- [ ] Run its no-device validation and record the expected message.

### Task 4: Final verification

- [ ] Run `./gradlew.bat --offline :app:testDebugUnitTest :app:assembleDebug`.
- [ ] Confirm APK exists at `app/build/outputs/apk/debug/app-debug.apk`.
- [ ] Report device-dependent items separately from verified items.
