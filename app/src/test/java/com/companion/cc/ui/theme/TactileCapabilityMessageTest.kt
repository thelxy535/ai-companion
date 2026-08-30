package com.companion.cc.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class TactileCapabilityMessageTest {
    @Test
    fun `reports missing vibrator before other capability details`() {
        assertEquals(
            "当前设备没有可用的振动器。",
            tactileCapabilityMessage(
                hasVibrator = false,
                systemHapticsEnabled = true,
                powerSave = false,
                preference = TactileIntensityPreference.SYSTEM,
                effectTier = GlassEffectTier.BALANCED
            )
        )
    }

    @Test
    fun `explains when steady visual mode suppresses feedback`() {
        assertEquals(
            "当前视觉模式会关闭应用触感反馈。",
            tactileCapabilityMessage(
                hasVibrator = true,
                systemHapticsEnabled = true,
                powerSave = false,
                preference = TactileIntensityPreference.MEDIUM,
                effectTier = GlassEffectTier.STEADY
            )
        )
    }

    @Test
    fun `explains power save reduction for enabled feedback`() {
        assertEquals(
            "省电模式下，应用会自动降低自定义触感强度。",
            tactileCapabilityMessage(
                hasVibrator = true,
                systemHapticsEnabled = true,
                powerSave = true,
                preference = TactileIntensityPreference.MEDIUM,
                effectTier = GlassEffectTier.BALANCED
            )
        )
    }
}
