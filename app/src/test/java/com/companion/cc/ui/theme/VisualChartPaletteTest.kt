package com.companion.cc.ui.theme

import com.companion.cc.domain.model.VisualCustomization
import org.junit.Assert.assertNotEquals
import org.junit.Test

class VisualChartPaletteTest {
    @Test
    fun trendAndTrustUseDistinctChartRoles() {
        val theme = VisualThemeResolver.resolve(
            isDark = false,
            route = VisualRoute.UTILITY,
            companionId = null,
            customization = VisualCustomization.default(),
            effectTier = GlassEffectTier.STEADY
        )

        assertNotEquals(theme.tokens.chart.trend, theme.tokens.chart.trust)
    }
}
