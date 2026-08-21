# iOS-like Glass Interaction Design

## Goal

Upgrade the Android material system so glass controls feel closer to iOS while preserving the current layout, navigation, theme customization, and Android compatibility. The implementation combines a stable base material, local touch optics, and an optional platform blur backend with explicit quality fallbacks.

## Scope

Glass behavior applies only to navigation, input docks, dialogs, menus, and primary controls. Repeated content rows, chat messages, settings rows, and statistics remain quiet content surfaces. No new page-level cards or blanket translucency are introduced.

## Architecture

`VisualThemeResolver` remains the single authority for resolved colors, opacity, border, shadow, blur, contrast, and motion tier.

`GlassSurface` is the only public glass primitive. It composes four bounded layers:

1. Base material: semantic fill, border, elevation, and readable content contrast.
2. Local touch optics: hit-gated highlight, opposite shade, edge response, and press compression.
3. Backdrop backend: platform real-time blur when supported; aligned scene replica blur otherwise; no blur in steady mode.
4. Content: caller content drawn above all optical layers.

`GlassEffectPolicy` selects the backend and strengths from quality tier, reduced-motion state, Android capability, and contrast requirements. Callers must not provide private alpha, blur, shadow, or animation constants.

## Touch Behavior

The surface tracks `Idle`, `Pressed`, `Dragging`, and `Released` locally. Pointer coordinates are accepted only after an in-bounds hit and are clamped to the owning surface. While pressed or dragging:

- the surface compresses by at most 0.8%;
- a short-lag highlight follows the actual local finger position;
- an opposing low-alpha shade and nearest-edge emphasis create depth;
- all effects are clipped to the surface bounds and never affect neighboring views.

On release, the material returns to rest over 180-260 ms with a critically damped spring or ease-out, without bounce, shimmer, periodic sweep, cursor halo, or remote pointer response. Reduced motion removes continuous follow animation but keeps semantic press feedback.

## Backdrop Backends

- `IMMERSIVE`: platform blur where available, otherwise scene replica blur; full local optics and press transition.
- `BALANCED`: scene replica or lower-radius platform blur, reduced sampling and motion; same interaction semantics.
- `STEADY`: no real-time or replica blur; semantic fill, border, compression, and local edge feedback remain.

The platform backend must be capability-gated and must fail closed to the replica backend. Contrast protection may increase fill or scrim density; readability takes precedence over transparency.

## Migration and No-Residual Rules

- Migrate home, chat, settings, character, memory, statistics, dialogs, navigation, and input controls to shared primitives.
- Remove direct page-level `Surface` usage unless explicitly classified as a quiet content surface.
- Remove page-local glass alpha, blur, shadow, gradient, and motion constants.
- Remove legacy immersive glass components and stale theme references after migration.
- Do not keep duplicate old and new implementations reachable from navigation.
- Do not add decorative orbs, neon glow, full-screen touch halos, periodic sweeps, or universal pills.

## Verification

1. Unit tests cover tier selection, capability fallback, opacity clamping, contrast protection, local hit gating, and reduced motion.
2. Source audits find no unclassified direct page `Surface` calls or private glass constants.
3. Build and install a debug APK.
4. Capture home, chat, settings, dialog, and custom-theme screenshots on light/dark/image backgrounds across all three tiers.
5. Test Android 17 hardware for press, drag, cancel, release, outside-pointer behavior, and frame stability.
6. Confirm no old glass component, stale import, or unreachable duplicate route remains.

## Acceptance Criteria

- The same semantic material system is used across all glass controls.
- Touch feedback is local, clipped, and visibly stronger than the current generic ripple without becoming a glow.
- Platform blur improves capable devices and never blocks unsupported devices.
- Content remains readable in light, dark, white, colored, and image-backed themes.
- The project has one reachable glass implementation and passes the verification checklist.
