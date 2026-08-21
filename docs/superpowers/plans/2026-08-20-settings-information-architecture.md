# Settings Information Architecture Implementation Plan

> **For agentic workers:** Use executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Reorganize settings into stable, scannable groups while preserving every existing state and action.

**Architecture:** Keep `SettingsScreen` as the state and picker boundary. Move AI, vision, and appearance controls into focused files composed from `UtilitySection`, dividers, standard Material fields, and selection rows. Keep glass only at the top bar and modal surfaces.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Hilt, StateFlow, Gradle.

## Global Constraints

- Preserve unrelated dirty-worktree changes.
- Preserve every current `SettingsViewModel` flow and callback.
- Add no page-local blur, opacity, gradient, or fake-highlight constants.
- Remove obsolete helpers instead of leaving unused card implementations.
- Verify offline with `:app:testDebugUnitTest :app:assembleDebug`.

---

### Task 1: Stable selection primitive and appearance group

**Files:**
- Create: `app/src/main/java/com/companion/cc/ui/settings/SettingsSelectionRow.kt`
- Create: `app/src/main/java/com/companion/cc/ui/settings/AppearanceSettings.kt`

- [ ] Add a 48dp minimum-height selection row using `RadioButton`, wrapping labels, and a whole-row click target.
- [ ] Compose theme and font-size choices inside stable `UtilitySection` containers.
- [ ] Keep `VisualCustomizationEditor` connected between theme and font-size groups.
- [ ] Run `:app:compileDebugKotlin`.

### Task 2: AI and visual-understanding groups

**Files:**
- Create: `app/src/main/java/com/companion/cc/ui/settings/AiServiceSettings.kt`
- Create: `app/src/main/java/com/companion/cc/ui/settings/VisionSettings.kt`

- [ ] Move provider status, API validation, model selection, supported-provider reference, and advanced Base URL disclosure into stable sections.
- [ ] Preserve loading, validation messages, dropdown selection, and test-connection behavior.
- [ ] Move visual service mode, Gemini credential, pairing, unpairing, and pairing-message controls into stable sections.
- [ ] Run `:app:compileDebugKotlin`.

### Task 3: Settings page composition and identity group

**Files:**
- Modify: `app/src/main/java/com/companion/cc/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/settings/AvatarSettings.kt`

- [ ] Reduce `SettingsScreen` to state collection, picker/dialog ownership, ordered group composition, and one glass top bar.
- [ ] Place three avatar rows in one stable Identity section separated by dividers.
- [ ] Change `AvatarSettingItem` from a per-row glass surface to a stable clickable row.
- [ ] Delete all replaced private card/option helpers from the original settings file.
- [ ] Run `:app:compileDebugKotlin`.

### Task 4: Residual and regression verification

- [ ] Scan settings files for obsolete helpers, nested cards, gradients, mojibake, and body-level glass surfaces.
- [ ] Run `:app:testDebugUnitTest :app:assembleDebug`.
- [ ] Run `git diff --check`.
- [ ] Confirm `app/build/outputs/apk/debug/app-debug.apk` exists and report its timestamp.
