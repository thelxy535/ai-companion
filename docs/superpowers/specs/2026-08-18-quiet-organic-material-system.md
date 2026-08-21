# Quiet Organic Material System

## Goal

Rebuild the Android visual system around a restrained, spatial material language inspired by modern mobile materials while preserving the existing layout, navigation, features, and theme customization model.

## Visual Direction

Quiet Organic uses a calm scene background, quiet content surfaces, and a separate interactive glass layer. Glass is reserved for navigation, input docks, dialogs, menus, and primary controls. Contact rows, chat content, settings rows, and statistics remain readable content surfaces rather than repeated floating cards.

The palette is semantic rather than page-specific: neutral canvas, elevated content, glass control, primary text, secondary text, outline, accent, and state colors. Accent colors are sparse and are derived from the existing customization system. No page may introduce private glass alpha, blur, gradient, or shadow constants.

## Anti-Pattern Guardrails

The visual system must not resemble the current generic AI-generated app aesthetic. The following are explicit prohibitions:

- No large purple/blue/pink gradients used as a substitute for identity.
- No neon glow, bloom, bokeh, decorative orbs, or periodic light sweeps.
- No blanket backdrop blur, identical translucent cards, or glass nested inside glass.
- No universal pill shape, identical corner radius, or floating-card treatment for every section.
- No oversized marketing-style headings, excessive empty hero space, or decorative copy explaining the UI.
- No animation added only to signal technology; every motion must communicate contact, hierarchy, continuity, or state change.
- No single-hue palette that makes the whole product read as purple, blue, beige, or dark gray.

The signature should come from quiet spatial continuity: stable content, controlled functional glass, material edges that respond only to direct contact, and typography that remains the primary reading surface.

## Material Layers

1. `Scene`: low-contrast background field with optional user image, scrim, and slow drift. No periodic light sweep.
2. `Content`: stable surfaces with enough fill to preserve text contrast. Rows use spacing and dividers before extra decoration.
3. `GlassControl`: backdrop replica, restrained fill, edge line, top-plane highlight, and bounded shadow. Used for navigation, input, dialogs, menus, and key controls.
4. `Interaction`: local contact response clipped to the owning surface. It never affects neighboring surfaces or distant areas.

## Touch-Switched Material

Every interactive glass surface has a local state machine: `Idle`, `Pressed`, `Dragging`, and `Released`. A pointer is accepted only after a hit inside the component. The surface stores local coordinates, clamps them to its bounds, and clears them on cancel or release.

When pressed, the surface compresses by at most 0.8%, its shadow moves inward, and a bounded radial optical response follows the actual finger with a short ease-out lag. The response combines a low-alpha highlight, an opposite low-alpha shade, and a small edge emphasis based on distance to the nearest edge. It is not a full-surface glow, cursor halo, shimmer, or periodic animation. Release returns to rest in 180-260ms with no bounce.

Content and text are drawn above the interaction layer. The response is disabled when the pointer is outside the owning surface. Reduced-motion mode keeps the semantic press state and edge change but removes continuous follow animation.

## Theme and Readability

`VisualThemeResolver` remains the only source of resolved colors, opacity, blur, border, shadow, and contrast roles. `glassOpacity` controls material density from clear to tinted, but the resolver may raise fill or scrim density when required to preserve readability. Primary and secondary text must remain readable over light, dark, image, and custom-color backdrops. Light mode uses darker outlines and stronger content fill than dark mode; dark mode uses distinct surface steps instead of a single black canvas.

## Quality Tiers

- `IMMERSIVE`: scene drift, backdrop replica blur, local touch optics, and full press transitions.
- `BALANCED`: lower motion and blur sampling, same interaction semantics and hierarchy.
- `STEADY`: no ambient scene animation or backdrop blur, but retains surface fills, borders, press compression, and local edge feedback.

The labels remain `沉浸动效`, `智能平衡`, and `稳定省电` and are not coupled to implementation names in user-facing copy.

## Acceptance Criteria

- Existing page layout and navigation remain unchanged.
- No page-level direct `Surface(...)` remains outside documented shared material primitives.
- No periodic shimmer, remote hover response, neon glow, or full-screen touch halo exists.
- Touch feedback is visible only after a real hit and is clipped to the owner.
- Home, chat, settings, dialogs, character, memory, and stats use the same semantic material system.
- Light, dark, white-background, colored-background, and image-background themes preserve readable text and distinct surface layers.
- Unit tests cover tier selection, opacity clamping, backdrop motion, and contrast fallback.
- Debug APK is built and verified with real emulator screenshots in all three tiers.
