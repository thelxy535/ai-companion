# Tool Pages Layout Implementation Plan

> **For agentic workers:** Execute inline task-by-task. Each task ends with a compile or test checkpoint.

**Goal:** Rebuild statistics and data management as compact Quiet Organic utility pages without changing their live data or actions.

**Architecture:** Add one shared stable content-section component, then compose each page from unframed metric grids and section rows. Keep glass only at top bars and dialogs; ViewModels remain the data/action boundary.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Hilt, Room Flow, Gradle.

## Global Constraints

- Preserve all unrelated dirty-worktree changes.
- Use existing theme tokens; add no page-local blur or glass constants.
- Keep all existing actions and live values.
- Verify with `:app:testDebugUnitTest :app:assembleDebug`.

---

### Task 1: Shared utility section

**Files:**
- Create: `app/src/main/java/com/companion/cc/ui/components/UtilitySection.kt`

- [ ] Implement a stable `Surface` section with optional title/icon and divider-based rows.
- [ ] Compile debug Kotlin.

### Task 2: Statistics page

**Files:**
- Modify: `app/src/main/java/com/companion/cc/ui/stats/ImmersiveStatsScreen.kt`

- [ ] Replace stacked glass cards with overview metric grid and three stable sections.
- [ ] Preserve loading, refresh, topics, traits, memory counts, and emotional score.
- [ ] Compile debug Kotlin.

### Task 3: Data management page

**Files:**
- Modify: `app/src/main/java/com/companion/cc/ui/data/ImmersiveDataManagementScreen.kt`

- [ ] Replace action cards with storage grid, transfer rows, settings row, and isolated destructive row.
- [ ] Preserve file picker, snackbar, processing overlay, confirmation dialog, and all actions.
- [ ] Compile debug Kotlin.

### Task 4: Residual and regression verification

- [ ] Scan both screens for nested glass cards, decorative gradients, obsolete helpers, and mojibake display strings.
- [ ] Run `./gradlew.bat --offline :app:testDebugUnitTest :app:assembleDebug`.
- [ ] Confirm the debug APK exists.
