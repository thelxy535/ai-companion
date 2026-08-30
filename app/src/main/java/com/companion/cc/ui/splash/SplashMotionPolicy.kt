package com.companion.cc.ui.splash

import com.companion.cc.ui.theme.GlassEffectTier

internal fun splashDurationMillis(tier: GlassEffectTier): Long =
    if (tier == GlassEffectTier.STEADY) 0L else 800L
