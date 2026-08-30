package com.companion.cc.ui.theme

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.VibratorManager
import android.provider.Settings

/** Device-aware tactile output. System settings and hardware always remain authoritative. */
class TactileFeedbackController(private val context: Context) {
    @Suppress("DEPRECATION")
    fun perform(
        gesture: TactileGesture,
        preference: TactileIntensityPreference,
        effectTier: GlassEffectTier
    ): Boolean {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.os.Vibrator::class.java)
        } ?: return false
        val powerManager = context.getSystemService(PowerManager::class.java)
        val systemEnabled = Settings.System.getInt(
            context.contentResolver,
            Settings.System.HAPTIC_FEEDBACK_ENABLED,
            1
        ) != 0
        val result = TactileIntensityPolicy.resolve(
            preference = preference,
            systemHapticsEnabled = systemEnabled,
            hasVibrator = vibrator.hasVibrator(),
            hasAmplitudeControl = vibrator.hasAmplitudeControl(),
            powerSave = powerManager?.isPowerSaveMode == true,
            effectTier = effectTier
        )
        if (!result.enabled) return false

        val duration = if (gesture == TactileGesture.LONG_PRESS ||
            gesture == TactileGesture.DESTRUCTIVE_CONFIRM
        ) 32L else 18L
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && result.useSystemDefault) {
                vibrator.vibrate(
                    VibrationEffect.createPredefined(
                        if (gesture == TactileGesture.LONG_PRESS ||
                            gesture == TactileGesture.DESTRUCTIVE_CONFIRM
                        ) {
                            VibrationEffect.EFFECT_HEAVY_CLICK
                        } else {
                            VibrationEffect.EFFECT_CLICK
                        }
                    )
                )
            } else if (result.amplitude != null) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, result.amplitude))
            } else {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }.onFailure { return false }
        return true
    }
}
