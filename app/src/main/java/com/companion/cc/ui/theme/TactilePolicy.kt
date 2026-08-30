package com.companion.cc.ui.theme

/** Interaction phases that may or may not represent a confirmed user action. */
enum class TactileGesture {
    CLICK,
    LONG_PRESS,
    DESTRUCTIVE_CONFIRM,
    DRAG,
    CANCEL,
    OUTSIDE_PRESS
}

/** Pure interaction rules shared by tactile Compose components. */
object TactilePolicy {
    const val minimumTouchTargetDp: Int = 48

    fun isActivationEnabled(enabled: Boolean): Boolean = enabled

    fun shouldPerformHaptic(
        gesture: TactileGesture,
        enabled: Boolean = true,
        effectTier: GlassEffectTier = GlassEffectTier.IMMERSIVE
    ): Boolean = enabled && effectTier != GlassEffectTier.STEADY && when (gesture) {
        TactileGesture.CLICK,
        TactileGesture.LONG_PRESS,
        TactileGesture.DESTRUCTIVE_CONFIRM -> true
        TactileGesture.DRAG,
        TactileGesture.CANCEL,
        TactileGesture.OUTSIDE_PRESS -> false
    }
}
