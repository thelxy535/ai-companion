package com.companion.cc.ui.memory.components

import com.companion.cc.ui.theme.GlassEffectTier
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartMotionPolicyTest {
    @Test
    fun steadyTierUsesStaticCharts() {
        assertFalse(chartAnimationEnabled(GlassEffectTier.STEADY))
        assertTrue(chartAnimationEnabled(GlassEffectTier.BALANCED))
    }
}
