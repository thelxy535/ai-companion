# Native Preview Material Parity Design

**Date:** 2026-08-18

## Goal

Make the native Android UI behaviorally and materially match `visual-preview/index-v6.html` while preserving the current Android layout, information architecture, features, custom themes, and interaction flows.

## Scope

This work covers the shared material system and every major UI surface that can visibly diverge from the preview: backgrounds, top bars, input docks, cards, bubbles, lists, dialogs, settings, character pages, memory/stat panels, empty/loading/error states, and press/long-press feedback.

No layout redesign, feature removal, or restoration of deleted legacy screens is allowed.

## Rendering Strategy

`VisualEffectsPreference` maps to the preview quality modes:

- `ENHANCED` / capable `AUTO`: immersive mode with animated ambient backdrop, aligned backdrop blur, local touch optics, responsive shadow offset, and press feedback.
- `AUTO` on Android 12+ when not power-saving and system animations are enabled: immersive mode by default.
- `AUTO` on unsupported or constrained devices: balanced fallback with material depth and touch feedback, not a flat opaque surface.
- `REDUCED`: steady mode with no continuous animation or backdrop blur, while retaining borders, tonal separation, readable fills, and press feedback.

The fallback must never silently remove material hierarchy or make the UI indistinguishable from a plain `Surface`.

## Material Architecture

`VisualThemeResolver` remains the single source of truth for semantic tokens. The preview roles map to these native roles:

- `GlassSurface`: floating navigation, input dock, important panels, overlays.
- `CompactGlassSurface`: repeated cards, rows, character entries, settings entries.
- `GlassDialogSurface`: dialogs and modal content.
- `MaterialSurface`: content that should remain stable and non-glass.

Every visible surface must use one of these semantic roles or an explicitly documented non-material primitive. Direct page-level `Surface` calls are not allowed for visual containers after migration.

## Dynamic Backdrop

`AdaptiveBackdropLayer` will render a low-contrast, theme-derived animated field on immersive and balanced tiers. Motion is slow and continuous, with no periodic sweep, neon glow, or repeating flash. The field uses the resolved custom background and accent colors, remains readable in light and dark modes, and stops completely in steady mode.

## Touch Optics

Touch feedback is local to the active surface:

- Pointer hover never changes material state.
- Touch down creates a restrained local highlight and shadow response.
- Dragging while pressed moves the local response within that surface.
- Pointer up, cancel, or leaving the surface clears the response.
- Long press preserves the pressed state without bounce or glow loops.

The same behavior must work for mouse/trackpad testing and Android touch input without allowing an unrelated surface to react.

## Theme and Accessibility Rules

Custom background, accent, mode, and glass opacity are resolved through semantic tokens. Foreground colors and borders are derived from the composed surface color, not hardcoded per page. Light, dark, and extreme custom backgrounds must maintain readable primary text, secondary text, icons, focus states, and control boundaries.

## No-Residual Acceptance Criteria

- No legacy theme classes are referenced by active source.
- No page-level visual containers use direct `Surface(...)` outside the shared material primitives or an explicitly documented stable primitive.
- No page introduces ad-hoc glass opacity, blur, gradient, shadow, or corner-radius values that bypass the token system.
- All major routes use the same semantic material roles.
- APK is built from the current source after tests pass.

## Verification

Run:

```text
:app:testDebugUnitTest
:app:assembleDebug
```

Verify screenshots or device inspection for home, chat, settings, character, memory/stat, and dialog surfaces in immersive, balanced, steady, light, dark, and custom-theme states. Confirm the APK metadata version and timestamp are newer than the source changes.
