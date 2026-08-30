package com.companion.cc.ui.splash

import com.companion.cc.ui.theme.GlassEffectTier
import org.junit.Assert.assertEquals
import org.junit.Test

class SplashMotionPolicyTest {
    @Test
    fun steadyTierUsesImmediateSplashDuration() {
        assertEquals(0L, splashDurationMillis(GlassEffectTier.STEADY))
        assertEquals(800L, splashDurationMillis(GlassEffectTier.BALANCED))
    }
}
