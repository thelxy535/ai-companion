package com.companion.cc.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassEffectPolicyTest {
    @Test
    fun `immersive selects platform blur on capable devices`() {
        val profile = GlassEffectPolicy.profile(
            tier = GlassEffectTier.IMMERSIVE,
            environment = capableEnvironment()
        )

        assertEquals(GlassBackdropBackend.PLATFORM, profile.backdropBackend)
        assertTrue(profile.followsPointer)
        assertTrue(profile.releaseDurationMillis in 180..260)
    }

    @Test
    fun `balanced uses scene replica and steady disables blur`() {
        val balanced = GlassEffectPolicy.profile(
            tier = GlassEffectTier.BALANCED,
            environment = capableEnvironment()
        )
        val steady = GlassEffectPolicy.profile(
            tier = GlassEffectTier.STEADY,
            environment = capableEnvironment()
        )

        assertEquals(GlassBackdropBackend.SCENE_REPLICA, balanced.backdropBackend)
        assertEquals(GlassBackdropBackend.NONE, steady.backdropBackend)
        assertFalse(steady.followsPointer)
    }

    @Test
    fun `immersive falls back to scene replica without platform support`() {
        val profile = GlassEffectPolicy.profile(
            tier = GlassEffectTier.IMMERSIVE,
            environment = capableEnvironment().copy(sdkInt = 30)
        )

        assertEquals(GlassBackdropBackend.SCENE_REPLICA, profile.backdropBackend)
    }

    @Test
    fun `auto uses full glass on a capable Android 12 device`() {
        assertEquals(
            GlassEffectTier.IMMERSIVE,
            GlassEffectPolicy.resolve(
                preference = VisualEffectsPreference.AUTO,
                environment = GlassEffectEnvironment(
                    sdkInt = 31,
                    isLowRamDevice = false,
                    isPowerSaveMode = false,
                    areSystemAnimationsEnabled = true
                )
            )
        )
    }

    @Test
    fun `auto uses static glass before Android 12`() {
        assertEquals(
            GlassEffectTier.BALANCED,
            GlassEffectPolicy.resolve(
                preference = VisualEffectsPreference.AUTO,
                environment = GlassEffectEnvironment(
                    sdkInt = 30,
                    isLowRamDevice = false,
                    isPowerSaveMode = false,
                    areSystemAnimationsEnabled = true
                )
            )
        )
    }

    @Test
    fun `enhanced cannot enable blur on a low memory device`() {
        assertEquals(
            GlassEffectTier.BALANCED,
            GlassEffectPolicy.resolve(
                preference = VisualEffectsPreference.ENHANCED,
                environment = GlassEffectEnvironment(
                    sdkInt = 34,
                    isLowRamDevice = true,
                    isPowerSaveMode = false,
                    areSystemAnimationsEnabled = true
                )
            )
        )
    }

    @Test
    fun `enhanced cannot enable blur during power save`() {
        assertEquals(
            GlassEffectTier.BALANCED,
            GlassEffectPolicy.resolve(
                preference = VisualEffectsPreference.ENHANCED,
                environment = GlassEffectEnvironment(
                    sdkInt = 34,
                    isLowRamDevice = false,
                    isPowerSaveMode = true,
                    areSystemAnimationsEnabled = true
                )
            )
        )
    }

    @Test
    fun `reduced always uses static glass`() {
        assertEquals(
            GlassEffectTier.STEADY,
            GlassEffectPolicy.resolve(
                preference = VisualEffectsPreference.REDUCED,
                environment = GlassEffectEnvironment(
                    sdkInt = 34,
                    isLowRamDevice = false,
                    isPowerSaveMode = false,
                    areSystemAnimationsEnabled = true
                )
            )
        )
    }

    @Test
    fun `auto uses static glass when system animations are disabled`() {
        assertEquals(
            GlassEffectTier.BALANCED,
            GlassEffectPolicy.resolve(
                preference = VisualEffectsPreference.AUTO,
                environment = GlassEffectEnvironment(
                    sdkInt = 34,
                    isLowRamDevice = false,
                    isPowerSaveMode = false,
                    areSystemAnimationsEnabled = false
                )
            )
        )
    }

    private fun capableEnvironment() = GlassEffectEnvironment(
        sdkInt = 36,
        isLowRamDevice = false,
        isPowerSaveMode = false,
        areSystemAnimationsEnabled = true
    )
}
