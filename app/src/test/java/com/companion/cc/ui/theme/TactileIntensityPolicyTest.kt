package com.companion.cc.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TactileIntensityPolicyTest {
    @Test
    fun `system preference maps to medium when hardware supports amplitude`() {
        val result = TactileIntensityPolicy.resolve(
            preference = TactileIntensityPreference.SYSTEM,
            systemHapticsEnabled = true,
            hasVibrator = true,
            hasAmplitudeControl = true,
            powerSave = false,
            effectTier = GlassEffectTier.BALANCED
        )

        assertTrue(result.enabled)
        assertEquals(null, result.amplitude)
        assertTrue(result.useSystemDefault)
    }

    @Test
    fun `strong preference is reduced for power save and never exceeds safe amplitude`() {
        val result = TactileIntensityPolicy.resolve(
            preference = TactileIntensityPreference.STRONG,
            systemHapticsEnabled = true,
            hasVibrator = true,
            hasAmplitudeControl = true,
            powerSave = true,
            effectTier = GlassEffectTier.IMMERSIVE
        )

        assertTrue(result.enabled)
        assertEquals(160, result.amplitude)
    }

    @Test
    fun `system disabled hardware absent and steady all suppress feedback`() {
        assertFalse(TactileIntensityPolicy.resolve(
            TactileIntensityPreference.MEDIUM, false, true, true, false, GlassEffectTier.IMMERSIVE
        ).enabled)
        assertFalse(TactileIntensityPolicy.resolve(
            TactileIntensityPreference.MEDIUM, true, false, true, false, GlassEffectTier.IMMERSIVE
        ).enabled)
        assertFalse(TactileIntensityPolicy.resolve(
            TactileIntensityPreference.MEDIUM, true, true, true, false, GlassEffectTier.STEADY
        ).enabled)
    }

    @Test
    fun `device without amplitude control keeps action enabled without amplitude request`() {
        val result = TactileIntensityPolicy.resolve(
            TactileIntensityPreference.LIGHT, true, true, false, false, GlassEffectTier.IMMERSIVE
        )

        assertTrue(result.enabled)
        assertEquals(null, result.amplitude)
    }
}
