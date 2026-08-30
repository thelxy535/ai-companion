package com.companion.cc.ui.chat

import com.companion.cc.ui.theme.GlassEffectTier

internal fun typingDotMotionEnabled(tier: GlassEffectTier): Boolean =
    tier != GlassEffectTier.STEADY
