package com.companion.cc.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.companion.cc.domain.model.VisualCustomization
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualThemeResolverTest {
    @Test
    fun `glass density monotonically increases fill and blur across effect tiers`() {
        GlassEffectTier.entries.forEach { tier ->
            val clear = resolveWithDensity(0.28f, tier)
            val balanced = resolveWithDensity(0.58f, tier)
            val dense = resolveWithDensity(0.88f, tier)

            assertTrue(clear.tokens.glass.fill.alpha < balanced.tokens.glass.fill.alpha)
            assertTrue(balanced.tokens.glass.fill.alpha < dense.tokens.glass.fill.alpha)
            if (tier == GlassEffectTier.STEADY) {
                assertEquals(0f, clear.tokens.glass.blurRadiusDp, 0f)
                assertEquals(0f, dense.tokens.glass.blurRadiusDp, 0f)
            } else {
                assertTrue(clear.tokens.glass.blurRadiusDp < balanced.tokens.glass.blurRadiusDp)
                assertTrue(balanced.tokens.glass.blurRadiusDp < dense.tokens.glass.blurRadiusDp)
            }
            assertTrue(clear.tokens.glass.backdropDetail > dense.tokens.glass.backdropDetail)
        }
    }

    @Test
    fun `glass density clamps to supported endpoints`() {
        GlassEffectTier.entries.forEach { tier ->
            assertEquals(
                resolveWithDensity(0.28f, tier).tokens.glass,
                resolveWithDensity(-1f, tier).tokens.glass
            )
            assertEquals(
                resolveWithDensity(0.88f, tier).tokens.glass,
                resolveWithDensity(2f, tier).tokens.glass
            )
        }
    }
    @Test
    fun `uses the users valid accent as the Material primary color`() {
        val theme = VisualThemeResolver.resolve(
            isDark = false,
            route = VisualRoute.CHAT,
            companionId = "custom-character",
            customization = VisualCustomization(accentHex = "#7a6ff0"),
        effectTier = GlassEffectTier.IMMERSIVE
        )

        assertEquals(Color(0xFF7A6FF0), theme.materialColors.primary)
    }

    @Test
    fun `unknown companion does not inherit Xiaocans accent`() {
        val customTheme = VisualThemeResolver.resolve(
            isDark = false,
            route = VisualRoute.CHAT,
            companionId = "custom-character",
            customization = VisualCustomization.default(),
        effectTier = GlassEffectTier.STEADY
        )
        val xiaocanTheme = VisualThemeResolver.resolve(
            isDark = false,
            route = VisualRoute.CHAT,
            companionId = "xiaocan",
            customization = VisualCustomization.default(),
        effectTier = GlassEffectTier.STEADY
        )

        assertNotEquals(customTheme.tokens.accent, xiaocanTheme.tokens.accent)
    }

    @Test
    fun `uses blur only for the full effect tier`() {
        val full = VisualThemeResolver.resolve(
            isDark = true,
            route = VisualRoute.HOME,
            companionId = "muse",
            customization = VisualCustomization.default(),
        effectTier = GlassEffectTier.IMMERSIVE
        )
        val fallback = VisualThemeResolver.resolve(
            isDark = true,
            route = VisualRoute.HOME,
            companionId = "muse",
            customization = VisualCustomization.default(),
        effectTier = GlassEffectTier.STEADY
        )

        assertTrue(full.tokens.glass.usesBackdropBlur)
        assertFalse(fallback.tokens.glass.usesBackdropBlur)
        assertTrue(fallback.tokens.glass.fill.alpha > full.tokens.glass.fill.alpha)
    }

    @Test
    fun `backdrop motion follows the selected quality tier`() {
        val immersive = VisualThemeResolver.resolve(
            isDark = true,
            route = VisualRoute.HOME,
            companionId = null,
            customization = VisualCustomization.default(),
            effectTier = GlassEffectTier.IMMERSIVE
        )
        val balanced = VisualThemeResolver.resolve(
            isDark = true,
            route = VisualRoute.HOME,
            companionId = null,
            customization = VisualCustomization.default(),
            effectTier = GlassEffectTier.BALANCED
        )
        val steady = VisualThemeResolver.resolve(
            isDark = true,
            route = VisualRoute.HOME,
            companionId = null,
            customization = VisualCustomization.default(),
            effectTier = GlassEffectTier.STEADY
        )

        assertTrue(immersive.tokens.backdrop.motionScale > balanced.tokens.backdrop.motionScale)
        assertTrue(balanced.tokens.backdrop.motionScale > steady.tokens.backdrop.motionScale)
        assertEquals(0f, steady.tokens.backdrop.motionScale, 0f)
    }

    @Test
    fun `keeps surface text readable in both color modes`() {
        listOf(false, true).forEach { isDark ->
            val theme = VisualThemeResolver.resolve(
                isDark = isDark,
                route = VisualRoute.UTILITY,
                companionId = "muse",
                customization = VisualCustomization.default(),
        effectTier = GlassEffectTier.STEADY
            )

            val contrast = contrastRatio(
                theme.materialColors.onSurface,
                theme.materialColors.surface
            )
            assertTrue("contrast=$contrast for isDark=$isDark", contrast >= 4.5)
        }
    }

    @Test
    fun `resolves quiet organic contact optics without exceeding restrained bounds`() {
        val light = VisualThemeResolver.resolve(
            isDark = false,
            route = VisualRoute.HOME,
            companionId = null,
            customization = VisualCustomization(glassOpacity = 0.66f),
            effectTier = GlassEffectTier.IMMERSIVE
        )
        val steady = VisualThemeResolver.resolve(
            isDark = true,
            route = VisualRoute.HOME,
            companionId = null,
            customization = VisualCustomization(glassOpacity = 0.66f),
            effectTier = GlassEffectTier.STEADY
        )

        assertTrue(light.tokens.glass.contactHighlightAlpha in 0.04f..0.16f)
        assertTrue(light.tokens.glass.highlight.alpha <= 0.18f)
        assertTrue(light.tokens.glass.contactShadeAlpha in 0.02f..0.12f)
        assertTrue(light.tokens.glass.contactTint.luminance() < 0.8f)
        assertTrue(light.tokens.glass.pressScale in 0.99f..1f)
        assertTrue(light.tokens.glass.contactRadiusDp in 44f..96f)
        assertTrue(light.tokens.glass.edgeResponseAlpha in 0.02f..0.12f)
        assertTrue(light.tokens.glass.releaseDurationMillis in 180..260)
        assertTrue(steady.tokens.glass.contactHighlightAlpha > 0f)
        assertTrue(steady.tokens.glass.blurRadiusDp == 0f)
        assertTrue(steady.tokens.glass.contactRadiusDp < light.tokens.glass.contactRadiusDp)
    }

    private fun contrastRatio(foreground: Color, background: Color): Double {
        val foregroundLuminance = foreground.relativeLuminance()
        val backgroundLuminance = background.relativeLuminance()
        val lighter = maxOf(foregroundLuminance, backgroundLuminance)
        val darker = minOf(foregroundLuminance, backgroundLuminance)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun resolveWithDensity(density: Float, tier: GlassEffectTier) =
        VisualThemeResolver.resolve(
            isDark = false,
            route = VisualRoute.HOME,
            companionId = null,
            customization = VisualCustomization(glassOpacity = density),
            effectTier = tier
        )

    private fun Color.relativeLuminance(): Double {
        fun linearize(channel: Float): Double = if (channel <= 0.04045f) {
            (channel / 12.92f).toDouble()
        } else {
            Math.pow(((channel + 0.055f) / 1.055f).toDouble(), 2.4)
        }

        return 0.2126 * linearize(red) +
            0.7152 * linearize(green) +
            0.0722 * linearize(blue)
    }
}
