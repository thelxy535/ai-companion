# Settings Information Architecture Design

## Goal

Reshape the existing settings screen into a quieter, easier-to-scan utility page while preserving every current setting, validation flow, picker, dialog, and ViewModel action.

## Scope

This phase changes only settings presentation and local component organization. It does not change provider discovery, API validation, vision pairing, model switching, visual customization persistence, avatar storage, theme values, font-size values, or navigation routes.

## Information Architecture

The page keeps one glass top bar and presents the body in this order:

1. AI service: provider status, API key validation, model selection, and supported-provider reference.
2. Visual understanding: hosted Gemini credentials and self-hosted pairing controls.
3. Appearance: theme mode, backdrop and glass customization, and font size.
4. Identity: user, Xiao Chan, and Muse avatar settings.
5. Advanced: custom base URL behind a compact disclosure row.

Each group uses a stable `UtilitySection` surface with dividers. Groups are not nested inside cards or glass containers. Existing conditional content remains conditional: model selection appears only when models exist, validation messages remain adjacent to their controls, and self-hosted pairing controls follow the selected vision mode.

## Component Boundaries

`SettingsScreen` remains the state collection and event-wiring boundary. It owns picker launchers and avatar dialog visibility, then passes values and callbacks to focused group composables.

The large screen file is split by responsibility:

- `SettingsScreen.kt`: page scaffold, state collection, group ordering, picker launchers, and dialogs.
- `AiServiceSettings.kt`: provider status, API credentials, model selection, supported providers, and advanced endpoint disclosure.
- `VisionSettings.kt`: visual-understanding service selection, Gemini credentials, and self-hosted pairing.
- `AppearanceSettings.kt`: theme, visual customization integration, and font-size controls.

Existing `AvatarSettings.kt` and `VisualCustomizationEditor.kt` remain the owners of their established controls. Shared `UtilitySection` and `UtilityDivider` remain the stable section primitives.

## Material Rules

- Glass is used only for the top bar and existing modal or transient overlays.
- Section bodies use stable surfaces and restrained dividers.
- Theme, vision-service, and font-size choices use single grouped selection rows rather than one glass surface per option.
- API keys and base URL remain text fields because they are editable data, not display rows.
- Primary validation and pairing actions remain buttons with at least 48dp touch targets.
- No page-local blur, opacity, gradient, or fake-highlight constants are introduced.
- No card is nested inside another card or section surface.

## State And Actions

All current `SettingsViewModel` flows continue to be collected by `SettingsScreen`. The following behaviors must remain unchanged:

- API key input synchronizes when the stored key changes.
- API validation exposes progress and the current validation message.
- Available models update and the selected model can be switched.
- Gemini visual API credentials can be stored.
- Self-hosted visual service can be paired and unpaired.
- Theme, font size, accent, backdrop, backdrop appearance, effects preference, and glass opacity persist through existing callbacks.
- User, Xiao Chan, and Muse avatar dialogs continue to open and save through existing implementations.
- Custom base URL remains editable in the advanced disclosure section.

## Error Handling

No new error channel is introduced. Existing validation and pairing messages remain visible near the action that produced them. Buttons retain the existing in-progress disabled state. Picker cancellation remains a no-op, and picker success continues through the existing ViewModel callback.

## Accessibility And Responsive Behavior

- The screen remains a `LazyColumn` and respects scaffold insets.
- Icon-only top-bar actions retain content descriptions.
- Every selectable row exposes its selected state through the existing Material selection control.
- Long labels and provider names may wrap instead of being forced into one line.
- Editable fields and buttons fill the available width on narrow screens.
- Section spacing stays compact enough that the first viewport shows provider state and the beginning of API configuration.

## Verification

Acceptance requires all of the following:

- `SettingsScreen.kt` no longer contains `CompactGlassSurface`, traditional `Card`, `OutlinedCard`, or per-option `GlassSurface` usage.
- Only the settings top bar retains page-level `GlassSurface`.
- Every state flow and ViewModel action wired before the redesign remains reachable afterward.
- Obsolete private card and option helpers are removed rather than left unused.
- UTF-8 Chinese display text remains intact.
- `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, and `:app:assembleDebug` succeed offline.
- `git diff --check` reports no whitespace errors.
