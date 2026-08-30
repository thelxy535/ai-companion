package com.companion.cc.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TactilePolicyTest {
    @Test
    fun `haptic is allowed only for confirmed click and long press`() {
        assertTrue(TactilePolicy.shouldPerformHaptic(TactileGesture.CLICK))
        assertTrue(TactilePolicy.shouldPerformHaptic(TactileGesture.LONG_PRESS))
        assertFalse(TactilePolicy.shouldPerformHaptic(TactileGesture.DRAG))
        assertFalse(TactilePolicy.shouldPerformHaptic(TactileGesture.CANCEL))
        assertFalse(TactilePolicy.shouldPerformHaptic(TactileGesture.OUTSIDE_PRESS))
    }

    @Test
    fun `disabled tactile feedback never requests haptic`() {
        assertFalse(TactilePolicy.shouldPerformHaptic(TactileGesture.CLICK, enabled = false))
    }

    @Test
    fun `disabled action does not expose an activation boundary`() {
        assertTrue(TactilePolicy.isActivationEnabled(true))
        assertFalse(TactilePolicy.isActivationEnabled(false))
    }

    @Test
    fun `destructive confirmation uses a tactile activation boundary`() {
        assertTrue(TactilePolicy.shouldPerformHaptic(TactileGesture.DESTRUCTIVE_CONFIRM))
        assertFalse(TactilePolicy.shouldPerformHaptic(TactileGesture.CANCEL))
    }

    @Test
    fun `long press activates only after threshold and once`() {
        assertFalse(TactileLongPressPolicy.shouldActivate(499L))
        assertTrue(TactileLongPressPolicy.shouldActivate(500L))
        assertFalse(TactileLongPressPolicy.shouldActivate(700L, alreadyActivated = true))
    }

    @Test
    fun `long press does not activate when cancelled disabled or steady`() {
        assertFalse(TactileLongPressPolicy.shouldActivate(600L, cancelled = true))
        assertFalse(TactileLongPressPolicy.shouldActivate(600L, enabled = false))
        assertFalse(TactileLongPressPolicy.shouldActivate(600L, effectTier = GlassEffectTier.STEADY))
    }

    @Test
    fun `steady mode suppresses tactile feedback`() {
        assertFalse(
            TactilePolicy.shouldPerformHaptic(
                gesture = TactileGesture.CLICK,
                effectTier = GlassEffectTier.STEADY
            )
        )
    }
}
