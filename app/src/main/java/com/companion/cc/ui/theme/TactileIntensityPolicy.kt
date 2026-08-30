package com.companion.cc.ui.theme

enum class TactileIntensityPreference {
    SYSTEM,
    LIGHT,
    MEDIUM,
    STRONG,
    OFF
}

data class TactileIntensityResult(
    val enabled: Boolean,
    val amplitude: Int?,
    val useSystemDefault: Boolean = false
)

object TactileIntensityPolicy {
    fun resolve(
        preference: TactileIntensityPreference,
        systemHapticsEnabled: Boolean,
        hasVibrator: Boolean,
        hasAmplitudeControl: Boolean,
        powerSave: Boolean,
        effectTier: GlassEffectTier
    ): TactileIntensityResult {
        if (preference == TactileIntensityPreference.OFF ||
            !systemHapticsEnabled ||
            !hasVibrator ||
            effectTier == GlassEffectTier.STEADY
        ) {
            return TactileIntensityResult(enabled = false, amplitude = null)
        }

        if (preference == TactileIntensityPreference.SYSTEM) {
            return TactileIntensityResult(
                enabled = true,
                amplitude = null,
                useSystemDefault = true
            )
        }

        val base = when (preference) {
            TactileIntensityPreference.LIGHT -> 64
            TactileIntensityPreference.MEDIUM -> 128
            TactileIntensityPreference.STRONG -> 224
            TactileIntensityPreference.SYSTEM,
            TactileIntensityPreference.OFF -> 0
        }
        val adjusted = if (powerSave) {
            (base * 5 / 7).coerceIn(1, 255)
        } else {
            base
        }
        return TactileIntensityResult(
            enabled = true,
            amplitude = adjusted.takeIf { hasAmplitudeControl }
        )
    }
}
