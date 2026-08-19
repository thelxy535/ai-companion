# Glass Backdrop Visibility Design

## Goal

Make the existing glass-density setting control how clearly the scene behind a glass surface remains visible. Low density exposes more recognizable background detail. High density increases material fill and blur so the background becomes softer and less distinct, matching the perceptual behavior of iOS frosted glass.

## Semantic Direction

The persisted `glassOpacity` value is treated as material density, not raw component alpha:

- `0.28`: clearest supported glass; low fill, low blur, visible background detail.
- `0.58`: balanced glass; moderate fill and blur.
- `0.88`: densest supported glass; stronger fill, blur, and contrast protection.

The setting label remains unchanged for compatibility. All callers consume resolved material tokens and must not interpret the stored value directly.

## Resolver Mapping

`VisualThemeResolver` maps density to a coordinated material profile:

- fill alpha increases monotonically with density;
- backdrop blur radius increases monotonically with density;
- backdrop detail/saturation decreases as density rises;
- border strength changes only enough to preserve the material edge;
- contrast protection may raise the fill floor when text would otherwise become unreadable.

The resolver clamps density to `0.28..0.88`. Compact content surfaces remain more opaque than glass controls and do not receive backdrop blur.

## Rendering Backends

- `IMMERSIVE`: use the supported platform backdrop backend when verified, otherwise the aligned scene-replica backend. Both consume the same resolved density profile.
- `BALANCED`: use aligned scene replication with a lower maximum blur cost.
- `STEADY`: disable blur while preserving density through semantic fill and border changes.

Backend selection must never reverse the slider direction. At every tier, a higher value must obscure more background detail than a lower value.

## Interaction

Touch optics remain independent of material density. Local highlight, opposite shade, edge response, press compression, drag tracking, and release behavior continue to use their own semantic tokens. Changing density must not restore fixed white reflections or introduce full-surface glow.

## No-Residual Rules

- No page or component reads `glassOpacity` directly.
- No page defines local blur radius or glass fill alpha.
- No fixed full-surface white gradient is reintroduced.
- `GlassSurface` and its wrappers remain the only reachable glass implementation.
- The old behavior where opacity changed fill but left backdrop blur effectively constant is removed.

## Verification

1. Unit tests prove fill alpha and blur radius increase monotonically from `0.28` to `0.88` for every quality tier.
2. Unit tests prove values outside the allowed range clamp to the same profiles as their nearest endpoint.
3. Source audit confirms only `VisualThemeResolver` interprets `glassOpacity`.
4. Full unit tests and Debug APK build pass.
5. Android 17 screenshots compare low, medium, and high density over the same image background in light and dark modes.

## Acceptance Criteria

- Low density visibly reveals more of the scene behind each glass control.
- High density produces a more opaque, frosted result without becoming a white card.
- Text remains readable at both endpoints.
- The slider direction is consistent across all three quality tiers.
- No fixed fake reflection or duplicate glass implementation remains.
