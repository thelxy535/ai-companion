package com.companion.cc.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.companion.cc.domain.model.BackdropTarget
import com.companion.cc.domain.model.VisualCustomization

/** Resolves every active visual role from deterministic, persisted inputs. */
object VisualThemeResolver {
    fun resolve(
        isDark: Boolean,
        route: VisualRoute,
        companionId: String?,
        customization: VisualCustomization,
        effectTier: GlassEffectTier
    ): VisualTheme {
        val accent = customization.accentHex.toColorOrNull()
            ?: builtInAccent(companionId, isDark)
        val colors = if (isDark) {
            darkScheme(accent)
        } else {
            lightScheme(accent)
        }
        val backdrop = backdropFor(
            isDark = isDark,
            route = route,
            companionId = companionId,
            accent = accent,
            customization = customization,
            effectTier = effectTier
        )
        val glass = glassFor(isDark, effectTier, customization.glassOpacity)

        return VisualTheme(
            materialColors = colors,
            tokens = VisualTokens(
                accent = accent,
                contentPrimary = colors.onSurface,
                contentSecondary = colors.onSurfaceVariant,
                contentMuted = colors.onSurfaceVariant.copy(alpha = if (isDark) 0.72f else 0.68f),
                glass = glass,
                status = statusColors(isDark),
                chart = chartColors(isDark),
                backdrop = backdrop
            ),
            effectTier = effectTier
        )
    }

    private fun lightScheme(accent: Color) = lightColorScheme(
        primary = accent,
        onPrimary = foregroundFor(accent),
        primaryContainer = blend(accent, LightCanvas, 0.82f),
        onPrimaryContainer = DarkInk,
        inversePrimary = blend(accent, Color.White, 0.22f),
        secondary = Color(0xFF77536A),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFD9E6),
        onSecondaryContainer = Color(0xFF2D1522),
        tertiary = Color(0xFF476179),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFCFE6FF),
        onTertiaryContainer = Color(0xFF001E31),
        background = LightCanvas,
        onBackground = DarkInk,
        surface = LightSurface,
        onSurface = DarkInk,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = Color(0xFF4A4651),
        surfaceTint = accent,
        inverseSurface = Color(0xFF302D36),
        inverseOnSurface = Color(0xFFF5F1FA),
        error = Color(0xFFBA1A1A),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        outline = Color(0xFF7A7582),
        outlineVariant = Color(0xFFCBC5D3),
        scrim = Color.Black
    )

    private fun darkScheme(accent: Color) = darkColorScheme(
        primary = accent,
        onPrimary = foregroundFor(accent),
        primaryContainer = blend(accent, DarkSurface, 0.62f),
        onPrimaryContainer = Color(0xFFE5E0FF),
        inversePrimary = blend(accent, Color.Black, 0.18f),
        secondary = Color(0xFFE8B9CF),
        onSecondary = Color(0xFF462636),
        secondaryContainer = Color(0xFF5F3D4C),
        onSecondaryContainer = Color(0xFFFFD9E6),
        tertiary = Color(0xFFB2CAE4),
        onTertiary = Color(0xFF1C344A),
        tertiaryContainer = Color(0xFF344B62),
        onTertiaryContainer = Color(0xFFCFE6FF),
        background = DarkCanvas,
        onBackground = Color(0xFFE8E4EE),
        surface = DarkSurface,
        onSurface = Color(0xFFE8E4EE),
        surfaceVariant = Color(0xFF373641),
        onSurfaceVariant = Color(0xFFCBC5D2),
        surfaceTint = accent,
        inverseSurface = Color(0xFFE8E4EE),
        inverseOnSurface = Color(0xFF302D36),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFF948F9B),
        outlineVariant = Color(0xFF48464F),
        scrim = Color.Black
    )

    private fun backdropFor(
        isDark: Boolean,
        route: VisualRoute,
        companionId: String?,
        accent: Color,
        customization: VisualCustomization,
        effectTier: GlassEffectTier
    ): BackdropSpec {
        val target = when (route) {
            VisualRoute.CHAT -> BackdropTarget.CHAT
            VisualRoute.HOME -> BackdropTarget.HOME
            VisualRoute.UTILITY -> BackdropTarget.UTILITY
        }
        val (start, end) = when {
            isDark && route == VisualRoute.CHAT -> Color(0xFF10121D) to Color(0xFF1A1B2A)
            isDark && route == VisualRoute.HOME -> Color(0xFF10121B) to Color(0xFF171923)
            isDark -> Color(0xFF13141B) to Color(0xFF1C1D27)
            route == VisualRoute.CHAT -> Color(0xFFFBF9FF) to Color(0xFFF1EFF9)
            route == VisualRoute.HOME -> Color(0xFFF9F8FF) to Color(0xFFF2F0FA)
            else -> Color(0xFFF9F8FC) to Color(0xFFF1F0F6)
        }
        val identityBloom = when (companionId) {
            "xiaocan" -> if (isDark) Color(0xFFFF9DBA) else Color(0xFFD96B8B)
            "muse" -> if (isDark) Color(0xFFB7A7FF) else Color(0xFF6657C8)
            else -> accent
        }
        return BackdropSpec(
            baseStart = start,
            baseEnd = end,
            primaryBloom = accent.copy(alpha = if (isDark) 0.26f else 0.18f),
            secondaryBloom = identityBloom.copy(alpha = if (isDark) 0.19f else 0.13f),
            userBackdrop = customization.backdropFor(target),
            isDark = isDark,
            motionScale = when (effectTier) {
                GlassEffectTier.IMMERSIVE -> 1f
                GlassEffectTier.BALANCED -> 0.48f
                GlassEffectTier.STEADY -> 0f
            }
        )
    }

    private fun glassFor(
        isDark: Boolean,
        effectTier: GlassEffectTier,
        opacity: Float
    ): GlassSurfaceSpec {
        val immersive = effectTier == GlassEffectTier.IMMERSIVE
        val steady = effectTier == GlassEffectTier.STEADY
        val balanced = effectTier == GlassEffectTier.BALANCED
        val normalized = opacity.coerceIn(0.28f, 0.88f)
        val baseFill = if (isDark) Color(0xFFFFFFFF) else Color(0xFFFFFFFF)  // 纯白色，依靠透明度控制
        val fillAlpha = when {
            immersive -> 0.22f + normalized * 0.34f
            balanced -> 0.38f + normalized * 0.26f
            else -> 0.64f + normalized * 0.2f
        }
        val compactAlpha = 0.0f  // 完全透明，只保留边框
        return GlassSurfaceSpec(
            fill = baseFill.copy(alpha = fillAlpha),
            strongFill = baseFill.copy(alpha = if (steady) 0.94f else 0.38f + normalized * 0.3f),
            compactFill = baseFill.copy(alpha = compactAlpha),
            border = if (isDark) Color.White.copy(alpha = 0.14f + normalized * 0.06f) else Color(0xFF708074).copy(alpha = 0.22f + normalized * 0.22f),
            highlight = if (isDark) Color.White.copy(alpha = 0.07f + normalized * 0.05f) else Color.White.copy(alpha = 0.08f + normalized * 0.08f),
            shadow = Color.Black.copy(alpha = if (isDark) 0.36f else 0.14f),
            scrim = Color.Black.copy(alpha = if (isDark) 0.42f else 0.25f),
            blurRadiusDp = when {
                immersive -> 6f + normalized * 18f
                balanced -> 4f + normalized * 10f
                else -> 0f
            },
            backdropDetail = 1f - ((normalized - 0.28f) / 0.6f).coerceIn(0f, 1f),
            usesBackdropBlur = !steady,
            contactTint = if (isDark) Color(0xFFDCE8DF) else Color(0xFF46544B),
            contactHighlightAlpha = if (isDark) {
                if (immersive) 0.14f else 0.10f
            } else {
                if (immersive) 0.14f else 0.10f
            },
            contactShadeAlpha = if (isDark) {
                if (immersive) 0.07f else 0.05f
            } else {
                if (immersive) 0.06f else 0.04f
            },
            pressScale = when (effectTier) {
                GlassEffectTier.IMMERSIVE -> 0.992f
                GlassEffectTier.BALANCED -> 0.994f
                GlassEffectTier.STEADY -> 0.996f
            },
            contactRadiusDp = when (effectTier) {
                GlassEffectTier.IMMERSIVE -> 76f
                GlassEffectTier.BALANCED -> 60f
                GlassEffectTier.STEADY -> 46f
            },
            edgeResponseAlpha = when (effectTier) {
                GlassEffectTier.IMMERSIVE -> 0.09f
                GlassEffectTier.BALANCED -> 0.06f
                GlassEffectTier.STEADY -> 0.035f
            },
            releaseDurationMillis = when (effectTier) {
                GlassEffectTier.IMMERSIVE -> 220
                GlassEffectTier.BALANCED -> 200
                GlassEffectTier.STEADY -> 180
            }
        )
    }

    private fun statusColors(isDark: Boolean): VisualStatusColors = if (isDark) {
        VisualStatusColors(
            success = Color(0xFF7CE2A7), onSuccess = Color(0xFF00391C), successContainer = Color(0xFF005227),
            warning = Color(0xFFFFD37D), onWarning = Color(0xFF3D2900), warningContainer = Color(0xFF5A3D00),
            info = Color(0xFFA6C8FF), onInfo = Color(0xFF00315E), infoContainer = Color(0xFF004A77),
            importance = Color(0xFFFFD27A), importanceContainer = Color(0xFF5B4100)
        )
    } else {
        VisualStatusColors(
            success = Color(0xFF006D36), onSuccess = Color.White, successContainer = Color(0xFF8FF6B1),
            warning = Color(0xFF805600), onWarning = Color.White, warningContainer = Color(0xFFFFDEA6),
            info = Color(0xFF005E99), onInfo = Color.White, infoContainer = Color(0xFFCDE5FF),
            importance = Color(0xFF795900), importanceContainer = Color(0xFFFFE08B)
        )
    }

    private fun chartColors(isDark: Boolean): VisualChartColors = if (isDark) {
        VisualChartColors(
            trend = Color(0xFFB8B1FF), affection = Color(0xFFFFB1C8), trust = Color(0xFFB8B1FF),
            interest = Color(0xFFFFD27A), positive = Color(0xFF7CE2A7), neutral = Color(0xFFA6C8FF), negative = Color(0xFFFFB4AB)
        )
    } else {
        VisualChartColors(
            trend = Color(0xFF625CD2), affection = Color(0xFFC85277), trust = Color(0xFF625CD2),
            interest = Color(0xFF8A6200), positive = Color(0xFF006D36), neutral = Color(0xFF005E99), negative = Color(0xFFBA1A1A)
        )
    }

    private fun builtInAccent(companionId: String?, isDark: Boolean): Color = when (companionId) {
        "xiaocan" -> if (isDark) Color(0xFFFFA9C1) else Color(0xFFD96B8B)
        "muse" -> if (isDark) Color(0xFFB7A7FF) else Color(0xFF6657C8)
        else -> if (isDark) Color(0xFFB8B1FF) else Color(0xFF625CD2)
    }

    private fun foregroundFor(color: Color): Color =
        if (color.luminance() > 0.42f) DarkInk else Color.White

    private fun blend(foreground: Color, background: Color, foregroundWeight: Float): Color {
        val weight = foregroundWeight.coerceIn(0f, 1f)
        return Color(
            red = foreground.red * weight + background.red * (1f - weight),
            green = foreground.green * weight + background.green * (1f - weight),
            blue = foreground.blue * weight + background.blue * (1f - weight),
            alpha = foreground.alpha * weight + background.alpha * (1f - weight)
        )
    }

    private fun String?.toColorOrNull(): Color? {
        val compact = this?.trim()?.removePrefix("#") ?: return null
        val expanded = when (compact.length) {
            3 -> compact.map { "$it$it" }.joinToString(separator = "")
            6 -> compact
            else -> return null
        }
        if (!expanded.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
            return null
        }
        return Color(0xFF000000 or expanded.toLong(16))
    }

    private val LightCanvas = Color(0xFFFAF8FF)
    private val LightSurface = Color(0xFFFFFBFF)
    private val LightSurfaceVariant = Color(0xFFF0EDF6)
    private val DarkCanvas = Color(0xFF11121A)
    private val DarkSurface = Color(0xFF1A1C26)
    private val DarkInk = Color(0xFF1D1B23)
}
