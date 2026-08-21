package com.companion.cc.ui.theme

/** User-facing visual quality preference persisted by the appearance settings. */
enum class VisualEffectsPreference {
    AUTO,
    ENHANCED,
    REDUCED;

    companion object {
        fun fromStoredValue(value: String?): VisualEffectsPreference =
            entries.firstOrNull { it.name == value } ?: AUTO
    }
}

/** Rendering tier selected for the current device and runtime state. */
enum class GlassEffectTier {
    IMMERSIVE,
    BALANCED,
    STEADY
}

enum class GlassBackdropBackend {
    PLATFORM,
    SCENE_REPLICA,
    NONE
}

data class GlassEffectProfile(
    val backdropBackend: GlassBackdropBackend,
    val followsPointer: Boolean,
    val releaseDurationMillis: Int
)

/**
 * Platform facts collected by the Android integration layer.
 *
 * Keeping these facts as data lets the selection policy stay deterministic and
 * JVM-testable without coupling the visual system to Android services.
 */
data class GlassEffectEnvironment(
    val sdkInt: Int,
    val isLowRamDevice: Boolean,
    val isPowerSaveMode: Boolean,
    val areSystemAnimationsEnabled: Boolean
)

/**
 * Selects premium rendering only when it is both supported and safe.
 *
 * Enhanced is a preference, not an override for low-memory, power-saving,
 * animation-disabled, or pre-Android-12 devices.
 */
object GlassEffectPolicy {
    private const val MIN_IMMERSIVE_GLASS_SDK = 31

    fun resolve(
        preference: VisualEffectsPreference,
        environment: GlassEffectEnvironment
    ): GlassEffectTier {
        if (preference == VisualEffectsPreference.REDUCED) {
            return GlassEffectTier.STEADY
        }
        if (environment.sdkInt < MIN_IMMERSIVE_GLASS_SDK) {
            return GlassEffectTier.BALANCED
        }
        if (environment.isLowRamDevice || environment.isPowerSaveMode) {
            return GlassEffectTier.BALANCED
        }
        if (!environment.areSystemAnimationsEnabled) {
            return GlassEffectTier.BALANCED
        }
        return GlassEffectTier.IMMERSIVE
    }

    fun profile(
        tier: GlassEffectTier,
        environment: GlassEffectEnvironment
    ): GlassEffectProfile = when (tier) {
        GlassEffectTier.IMMERSIVE -> GlassEffectProfile(
            backdropBackend = if (environment.sdkInt >= MIN_IMMERSIVE_GLASS_SDK) {
                GlassBackdropBackend.PLATFORM
            } else {
                GlassBackdropBackend.SCENE_REPLICA
            },
            followsPointer = environment.areSystemAnimationsEnabled,
            releaseDurationMillis = 220
        )
        GlassEffectTier.BALANCED -> GlassEffectProfile(
            backdropBackend = GlassBackdropBackend.SCENE_REPLICA,
            followsPointer = environment.areSystemAnimationsEnabled,
            releaseDurationMillis = 200
        )
        GlassEffectTier.STEADY -> GlassEffectProfile(
            backdropBackend = GlassBackdropBackend.NONE,
            followsPointer = false,
            releaseDurationMillis = 180
        )
    }
}
