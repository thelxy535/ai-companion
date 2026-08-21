# Quiet Organic Tool Pages Design

## Goal

Reshape the statistics and data-management screens into quiet, scannable utility pages while preserving all existing actions, navigation, theme customization, and live Room-backed values.

## Visual Direction

Both pages use one continuous scene with a narrow glass top bar. Glass is reserved for the top bar, menus, dialogs, and primary controls. Main information uses stable content surfaces with restrained fill, compact spacing, and separators. No nested glass surfaces, decorative gradients, fixed white highlights, or page-local alpha/blur constants are introduced.

## Statistics Layout

The screen order is:

1. Top bar: back action, companion name, refresh action.
2. Overview strip: total messages, conversation rounds, session duration in a compact three-column layout on wide screens and a two-column layout on narrow screens.
3. Memory section: total active memory, vector memory, and legacy memory context presented as a single content region with dividers.
4. Conversation section: current topics and top traits in readable rows.
5. Emotion section: score, label, and progress indicator.

The existing live values remain the source of truth. Loading is a centered state only before the first aggregate arrives; later updates replace values in place without hiding the page.

## Data Management Layout

The screen order is:

1. Top bar: back action and page title.
2. Storage overview: database, messages, memories, and cache as a compact metric grid with no individual floating cards.
3. Transfer section: import and export actions share one content region and remain enabled only when no operation is active.
4. Preferences section: reset settings is visually separate from transfer actions.
5. Destructive section: clear conversations uses error color, explicit confirmation, and a dialog; it is the only destructive action on the page.

The live storage values update in place from Room flows. Database size includes the main database file and its WAL/SHM companions.

## Responsive and Accessibility Rules

- Use existing `LocalVisualTheme` tokens for colors, typography, borders, and glass roles.
- Keep minimum touch targets at 48dp.
- Use `LazyColumn` for scrolling content and preserve bottom navigation/IME insets.
- Keep labels and values on separate lines when a narrow width would truncate them.
- Preserve content descriptions for every icon action.
- Respect reduced-motion settings by relying on existing glass policy transitions.

## Acceptance Criteria

- No statistics or data-management page section is a glass card nested inside another glass card.
- The first viewport shows the top bar and the primary metrics without decorative empty space.
- Live counts continue updating without manual refresh.
- Import, export, reset, and clear actions retain their current behavior.
- Existing unit tests pass and a debug APK assembles successfully.
