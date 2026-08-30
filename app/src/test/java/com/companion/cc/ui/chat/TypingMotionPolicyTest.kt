package com.companion.cc.ui.chat

import com.companion.cc.ui.theme.GlassEffectTier
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TypingMotionPolicyTest {
    @Test
    fun steadyTierDisablesTypingMotion() {
        assertFalse(typingDotMotionEnabled(GlassEffectTier.STEADY))
        assertTrue(typingDotMotionEnabled(GlassEffectTier.BALANCED))
    }
}
