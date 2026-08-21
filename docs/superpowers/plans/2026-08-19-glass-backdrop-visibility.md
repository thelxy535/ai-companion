# Glass Backdrop Visibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task with verification checkpoints.

**Goal:** Make glass density monotonically control background visibility through coordinated fill and blur profiles.

**Architecture:** `VisualThemeResolver` remains the sole interpreter of `glassOpacity`. It resolves fill, blur, edge, and contrast values consumed by the existing `GlassSurface` backend without page-level material constants.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit, Gradle 8.2.1.

## Global Constraints

- Clamp material density to `0.28..0.88`.
- Higher density must increase fill and obscure more background detail at every tier.
- Compact content surfaces remain non-blurred and more opaque than glass controls.
- Do not restore fixed white gradients or modify touch optics.

---

### Task 1: Test and implement density profiles

**Files:**
- Modify: `app/src/test/java/com/companion/cc/ui/theme/VisualThemeResolverTest.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/theme/VisualThemeResolver.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/theme/VisualThemeModel.kt`

- [ ] Add failing tests for monotonic fill/blur mapping and endpoint clamping in all tiers.
- [ ] Add a resolved backdrop-detail value and density-dependent blur radii.
- [ ] Run focused resolver tests and require PASS.

### Task 2: Apply resolved background visibility

**Files:**
- Modify: `app/src/main/java/com/companion/cc/ui/theme/AdaptiveGlass.kt`

- [ ] Apply the resolved detail value only to the owned backdrop replica.
- [ ] Keep content and touch optics above the backdrop treatment.
- [ ] Compile and run theme tests.

### Task 3: Audit and package

**Files:**
- Verify: `app/build/outputs/apk/debug/app-debug.apk`

- [ ] Audit direct `glassOpacity`, page blur values, and fixed full-surface highlights.
- [ ] Run `:app:testDebugUnitTest` and `:app:assembleDebug`.
- [ ] Record Android 17 low/medium/high screenshot verification as the remaining hardware check if no device is attached.
