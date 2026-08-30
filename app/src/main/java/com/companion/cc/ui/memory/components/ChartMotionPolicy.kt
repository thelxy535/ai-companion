package com.companion.cc.ui.memory.components

import com.companion.cc.ui.theme.GlassEffectTier

internal fun chartAnimationEnabled(tier: GlassEffectTier): Boolean =
    tier != GlassEffectTier.STEADY
