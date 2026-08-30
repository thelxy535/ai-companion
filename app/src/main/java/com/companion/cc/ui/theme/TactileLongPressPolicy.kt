package com.companion.cc.ui.theme

/** Pure long-press activation boundary used by tactile gesture handlers. */
object TactileLongPressPolicy {
    const val defaultTimeoutMillis: Long = 500L

    fun shouldActivate(
        elapsedMillis: Long,
        enabled: Boolean = true,
        cancelled: Boolean = false,
        alreadyActivated: Boolean = false,
        effectTier: GlassEffectTier = GlassEffectTier.IMMERSIVE,
        timeoutMillis: Long = defaultTimeoutMillis
    ): Boolean = enabled &&
        !cancelled &&
        !alreadyActivated &&
        effectTier != GlassEffectTier.STEADY &&
        elapsedMillis >= timeoutMillis
}
