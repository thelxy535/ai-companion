# Quiet Organic Material System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task with verification checkpoints.

**Goal:** Implement the approved Quiet Organic material system with local touch optics, semantic theme protection, and consistent Android runtime behavior.

**Architecture:** Keep `VisualThemeResolver` as the token authority. Add a focused interactive glass primitive in `AdaptiveGlass.kt`, use scene-scoped backdrop replication only for glass-control layers, and classify repeated content surfaces separately from controls. Validate each stage with JVM tests, APK builds, emulator screenshots, and source audits.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Canvas/draw modifiers, Gradle 8.13, JUnit, Android emulator.

## Global Constraints

- Preserve current Android layout, navigation, information architecture, and existing features.
- Glass is reserved for navigation, input, dialogs, menus, and key controls; content rows remain quiet surfaces.
- No periodic sweep, neon glow, hover reflection, or distant pointer response.
- `glassOpacity`, accent, backdrop, mode, and contrast are resolved through semantic tokens.
- Reduced mode retains press feedback while disabling ambient motion and backdrop blur.
- The result must not use generic AI UI patterns: neon glow, decorative orbs, blanket glass, universal pills, large gradients, or motion without semantic purpose.
- The visual signature comes from spatial continuity, restrained materials, direct-contact response, and typography-led hierarchy.

### Task 1: Define interactive material tokens

**Files:**
- Modify: `app/src/main/java/com/companion/cc/ui/theme/VisualThemeModel.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/theme/VisualThemeResolver.kt`
- Test: `app/src/test/java/com/companion/cc/ui/theme/VisualThemeResolverTest.kt`

- [ ] Add token fields for contact radius, press scale, contact highlight/shade, edge response, and resolved contrast guard.
- [ ] Add tests for light/dark/image/white/custom backgrounds and opacity clamping.
- [ ] Implement resolver-only derivation with tier-specific motion and blur strengths.
- [ ] Run focused theme tests and verify they pass.

### Task 2: Implement `InteractiveGlassSurface`

**Files:**
- Modify: `app/src/main/java/com/companion/cc/ui/theme/AdaptiveGlass.kt`
- Test: `app/src/test/java/com/companion/cc/ui/theme/GlassEffectPolicyTest.kt`

- [ ] Add local pointer states for down, drag, release, and cancel; accept only in-bounds hits.
- [ ] Draw clipped local highlight, opposite shade, edge emphasis, and bounded backdrop replica.
- [ ] Add press scale and shadow interpolation with no bounce or periodic animation.
- [ ] Reuse the primitive from `GlassSurface` and keep `CompactGlassSurface` as a quiet content recipe.
- [ ] Compile theme sources and run focused tests.

### Task 3: Reclassify page surfaces

**Files:**
- Modify: `app/src/main/java/com/companion/cc/ui/home/ImmersiveHomeScreen.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/chat/NaturalChatScreen.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/character/CharacterListScreen.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/memory/MemoryTreeScreen.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/stats/ImmersiveStatsScreen.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/components/TagManagementDialog.kt`

- [ ] Keep navigation, input, dialog, and primary controls on interactive glass.
- [ ] Move repeated content rows and secondary cards to the quiet content recipe without changing layout or click behavior.
- [ ] Remove page-local material constants introduced during migration.
- [ ] Audit all page-level `Surface(...)` calls.

### Task 4: Verify and package

**Files:**
- Verify: `app/build/outputs/apk/debug/app-debug.apk`

- [ ] Run `:app:testDebugUnitTest` with Gradle 8.13.
- [ ] Run `:app:assembleDebug`.
- [ ] Install on `emulator-5554`, force-stop, and relaunch.
- [ ] Capture home, chat, settings, dialog, and custom-theme screenshots in light/dark and all quality tiers.
- [ ] Verify touch response is local, clipped, and absent when the pointer is outside.
- [ ] Run source audits for direct page `Surface`, legacy theme refs, and ad-hoc blur/shadow/alpha values.
