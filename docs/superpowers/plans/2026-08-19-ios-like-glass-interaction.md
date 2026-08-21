# iOS-like Glass Interaction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task with verification checkpoints.

**Goal:** Upgrade the Android glass system with iOS-like local touch optics, optional platform blur, and complete migration without leaving legacy material paths reachable.

**Architecture:** `VisualThemeResolver` owns all semantic material values. `GlassSurface` owns bounded base material, local touch state, and content layering. `GlassEffectPolicy` chooses platform blur, scene-replica blur, or no blur based on quality tier, reduced motion, capability, and contrast.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Compose pointer input/drawing, Android RenderEffect when available, JUnit, Gradle 8.13.

## Global Constraints

- Glass is limited to navigation, input, dialogs, menus, and primary controls.
- Content rows remain quiet surfaces; no blanket translucency or nested glass.
- All alpha, blur, border, shadow, contrast, and motion values come from `VisualThemeResolver`.
- Touch effects are local, clipped, hit-gated, and have no shimmer, neon glow, periodic sweep, or remote halo.
- `IMMERSIVE`, `BALANCED`, and `STEADY` tiers must remain functional on Android 17 and older supported devices.
- Existing layout, navigation, theme customization, and user-facing copy remain intact.

---

### Task 1: Extend semantic material tokens

**Files:**
- Modify: `app/src/main/java/com/companion/cc/ui/theme/VisualThemeModel.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/theme/VisualThemeResolver.kt`
- Test: `app/src/test/java/com/companion/cc/ui/theme/VisualThemeResolverTest.kt`

- [ ] Add resolved fields for contact radius, press scale, local highlight/shade, edge response, blur backend, and contrast guard.
- [ ] Add tests for light, dark, image, white, custom backgrounds, opacity clamping, and each quality tier.
- [ ] Implement resolver-only derivation; remove caller-specific material constants.
- [ ] Run `./gradlew :app:testDebugUnitTest --tests '*VisualThemeResolverTest'` and require PASS.

### Task 2: Implement local iOS-like touch optics

**Files:**
- Modify: `app/src/main/java/com/companion/cc/ui/theme/AdaptiveGlass.kt`
- Modify: `app/src/main/java/com/companion/cc/ui/theme/GlassEffectPolicy.kt`
- Test: `app/src/test/java/com/companion/cc/ui/theme/GlassEffectPolicyTest.kt`

- [ ] Add explicit `Idle`, `Pressed`, `Dragging`, and `Released` state handling with in-bounds hit gating and cancellation.
- [ ] Draw clipped local highlight, opposite shade, nearest-edge emphasis, and bounded shadow interpolation.
- [ ] Add at most 0.8% press compression and 180-260 ms non-bouncy release interpolation.
- [ ] Add platform blur capability selection with scene-replica and no-blur fallbacks.
- [ ] Add tests for outside-pointer rejection, reduced motion, backend fallback, and tier strengths.
- [ ] Run focused policy tests and compile theme sources.

### Task 3: Migrate all glass control consumers

**Files:**
- Modify: `ui/home/ImmersiveHomeScreen.kt`
- Modify: `ui/chat/NaturalChatScreen.kt`
- Modify: `ui/settings/SettingsScreen.kt`
- Modify: `ui/character/CharacterListScreen.kt`
- Modify: `ui/memory/MemoryTreeScreen.kt`
- Modify: `ui/stats/ImmersiveStatsScreen.kt`
- Modify: `ui/components/TagManagementDialog.kt`
- Modify: `ui/navigation/NavGraph.kt`

- [ ] Route navigation, input, dialogs, menus, and primary actions through `GlassSurface` or its focused wrappers.
- [ ] Convert repeated rows and secondary cards to the quiet content recipe without changing click behavior.
- [ ] Remove direct page-level `Surface` calls and local alpha/blur/shadow/gradient constants unless documented as content surfaces.
- [ ] Remove stale imports and ensure deleted legacy immersive components are not reachable from navigation.
- [ ] Run source audits for `Surface(`, `blur(`, `shadow(`, alpha literals, and legacy theme references.

### Task 4: Verify and package

**Files:**
- Verify: `app/build/outputs/apk/debug/app-debug.apk`
- Test: all modified theme and UI test sources

- [ ] Run `./gradlew :app:testDebugUnitTest`.
- [ ] Run `./gradlew :app:assembleDebug`.
- [ ] Install the APK on Android 17 hardware/emulator and force-stop/relaunch.
- [ ] Capture home, chat, settings, dialog, and custom-theme screenshots in light/dark/image backgrounds for all tiers.
- [ ] Exercise press, drag, release, cancel, and outside-pointer cases; confirm the effect never escapes its owner.
- [ ] Record frame stability and confirm no legacy glass route, stale import, or duplicate implementation remains.

## Self-review

The plan covers token ownership, local interaction, three blur backends, page migration, no-residual source audits, unit tests, APK build, and Android 17 verification. No placeholders or unresolved type names are used; the public integration point remains `GlassSurface`, while `VisualThemeResolver` and `GlassEffectPolicy` provide its inputs.
