package com.companion.cc.domain.model

import com.companion.cc.ui.theme.VisualEffectsPreference

/** Where a user-selected backdrop is rendered. */
enum class BackdropTarget {
    CHAT,
    HOME,
    UTILITY
}

/** Backdrop sources supported by the visual system. */
enum class BackdropKind {
    NONE,
    IMAGE
}

/**
 * A normalized background reference. Image references are managed private
 * `file:` URIs only; picker-owned content URIs are never persisted here.
 */
data class VisualBackdrop(
    val kind: BackdropKind = BackdropKind.NONE,
    val managedImageReference: String? = null,
    val imageOpacity: Float = DEFAULT_IMAGE_OPACITY,
    val scrimOpacity: Float = DEFAULT_SCRIM_OPACITY
) {
    companion object {
        const val DEFAULT_IMAGE_OPACITY = 0.18f
        const val DEFAULT_SCRIM_OPACITY = 0.24f
    }
}

/** Persisted appearance choices independent of light/dark system mode. */
data class VisualCustomization(
    val accentHex: String? = null,
    val effectsPreference: VisualEffectsPreference = VisualEffectsPreference.AUTO,
    val glassOpacity: Float = DEFAULT_GLASS_OPACITY,
    val backdrops: Map<BackdropTarget, VisualBackdrop> = defaultBackdrops()
) {
    fun backdropFor(target: BackdropTarget): VisualBackdrop =
        backdrops[target] ?: VisualBackdrop()

    companion object {
        const val DEFAULT_GLASS_OPACITY = 0.66f
        fun default(): VisualCustomization = VisualCustomization()

        private fun defaultBackdrops(): Map<BackdropTarget, VisualBackdrop> =
            BackdropTarget.entries.associateWith { VisualBackdrop() }
    }
}
