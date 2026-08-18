# Native Preview Material Parity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task with verification checkpoints.

**Goal:** Make the native Android material system behaviorally and visually match `visual-preview/index-v6.html` while preserving the existing Android layouts and features.

**Architecture:** Keep `VisualThemeResolver` as the semantic-token source of truth. Add a scene-scoped animated backdrop and local pointer state to the shared Compose material primitives, map the three quality modes directly to preview behavior, and migrate page-level visual containers to semantic wrappers.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Compose Canvas, Gradle, JUnit.

## Global Constraints

- Preserve current Android layout, navigation, information architecture, and existing features.
- `AUTO` on capable Android 12+ devices defaults to immersive behavior.
- Reduced mode stops continuous animation and blur but retains material depth and press feedback.
- No periodic sweep, neon glow, hover reflection, or unrelated surface response.
- Custom background, accent, mode, and glass opacity must resolve through semantic tokens.
- No page-level visual container may retain a direct `Surface(...)` call after migration unless documented as a stable primitive.

---

### Task 1: Align quality policy with preview modes

**Files:**
- Modify: `app/src/main/java/com/companion/cc/ui/theme/GlassEffectPolicy.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/theme/AppVisualTheme.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/settings/VisualCustomizationEditor.kt`
- Test: `app/src/test/java/com/companion/cc/ui/theme/GlassEffectPolicyTest.kt`

**Interfaces:**
- `GlassEffectPolicy.resolve(preference, environment)` returns `FULL` for capable `AUTO` and `ENHANCED`, `STATIC` only for `REDUCED` or constrained devices.
- Settings labels describe `沉浸动效`, `智能平衡`, and `稳定省电`.

- [ ] Add failing tests for capable `AUTO`, constrained `AUTO`, explicit enhanced, and reduced behavior.
- [ ] Run `:app:testDebugUnitTest --tests '*GlassEffectPolicyTest'` and verify the new assertions fail before implementation.
- [ ] Implement the policy and update labels without changing persisted enum values.
- [ ] Re-run the focused tests and verify all pass.

### Task 2: Add a scene-scoped animated backdrop

**Files:**
- Modify: `app/src/main/java/com/companion/cc/ui/theme/AdaptiveGlass.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/theme/VisualThemeModel.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/theme/VisualThemeResolver.kt`
- Test: `app/src/test/java/com/companion/cc/ui/theme/VisualThemeResolverTest.kt`

**Interfaces:**
- `BackdropSpec` exposes resolved motion and optics strengths for the current `GlassEffectTier`.
- `AdaptiveBackdropLayer` accepts the resolved spec and animates only when the tier allows it.

- [ ] Add resolver tests proving immersive/balanced motion strengths are non-zero and steady strength is zero.
- [ ] Run the focused resolver tests and verify the new assertions fail.
- [ ] Implement a low-contrast Canvas field using theme background/accent colors, slow time-based drift, and no periodic sweep.
- [ ] Stop animation completely for steady mode and preserve custom backdrop image/scrim behavior.
- [ ] Re-run resolver tests.

### Task 3: Match local touch optics and press feedback

**Files:**
- Modify: `app/src/main/java/com/companion/cc/ui/theme/AdaptiveGlass.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/theme/VisualThemeModel.kt`
- Test: `app/src/test/java/com/companion/cc/ui/theme/GlassEffectPolicyTest.kt`

**Interfaces:**
- Shared surfaces maintain local `contact`, `touchPosition`, and press state.
- Pointer hover does not mutate material state; down/drag/up/cancel are local to the owning surface.

- [ ] Add deterministic policy/token assertions for reduced mode preserving press feedback while disabling ambient motion.
- [ ] Implement bounded local highlight, scale, and shadow interpolation for `GlassSurface` and `CompactGlassSurface`.
- [ ] Ensure pointer state clears on up, cancel, and gesture end; do not use periodic shimmer.
- [ ] Run focused tests and compile the theme package.

### Task 4: Migrate page-level visual containers

**Files:**
- Modify: `app/src/main/java/com/companion/cc/ui/chat/NaturalChatScreen.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/character/CharacterCustomizationScreen.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/character/CharacterListScreen.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/components/TagManagementDialog.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/favorites/FavoritesScreen.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/home/ImmersiveHomeScreen.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/memory/MemoryTreeScreen.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/memory/TreeVisualization.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/settings/AvatarSettings.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/stats/ImmersiveStatsScreen.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/chat/components/EmotionalStatsDialog.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/chat/components/EmotionalStatusBar.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/components/Avatar.kt`

**Interfaces:**
- Important panels use `GlassSurface`/`GlassDialogSurface`.
- Repeated rows use `CompactGlassSurface`.
- Non-glass status primitives are explicitly documented and use semantic colors.

- [ ] Inventory each direct `Surface(...)` and classify it as floating, repeated, dialog, or stable primitive.
- [ ] Replace each visual-container call with `GlassSurface`, `CompactGlassSurface`, `GlassDialogSurface`, or the documented stable primitive selected during classification, preserving padding, shape, and click/long-press behavior.
- [ ] Remove ad-hoc glass opacity, blur, gradient, and shadow values from migrated surfaces.
- [ ] Run a source audit proving only shared primitives contain direct `Surface(...)` calls.

### Task 5: Verify theme readability and material parity

**Files:**
- Modify: `app/src/main/java/com/companion/cc/ui/theme/VisualThemeResolver.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/theme/VisualTypography.kt`
- Test: `app/src/test/java/com/companion/cc/ui/theme/VisualThemeResolverTest.kt`
- Test: `app/src/test/java/com/companion/cc/data/theme/VisualCustomizationCodecTest.kt`

- [ ] Add tests for light, dark, white, yellow, pink, blue, near-black, and custom background inputs.
- [ ] Verify primary/secondary text and outline contrast after glass opacity composition.
- [ ] Adjust only semantic token derivation; do not add per-page color overrides.
- [ ] Run all theme and codec tests.

### Task 6: Full verification and packaging

**Files:**
- Verify: `app/build/outputs/apk/debug/app-debug.apk`
- Verify: `app/build/outputs/apk/debug/output-metadata.json`

- [ ] Run `:app:testDebugUnitTest` with the clean Gradle home and `-Pkotlin.compiler.execution.strategy=in-process`.
- [ ] Run `:app:assembleDebug` with the same environment.
- [ ] Verify test XML reports contain zero failures and errors.
- [ ] Verify APK metadata is `versionCode 11`, `versionName 2.1.0-beta.10`, and timestamp is newer than the final source change.
- [ ] Run source audits for legacy theme references, direct page-level `Surface(...)`, and ad-hoc material constants.
- [ ] Capture or inspect home, chat, settings, character, memory/stat, and dialog states in all three quality modes and light/dark/custom themes.
