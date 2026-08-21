package com.companion.cc.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.companion.cc.domain.model.VisualBackdrop

/** Active route families used to select a backdrop target and optical density. */
enum class VisualRoute {
    HOME,
    CHAT,
    UTILITY
}

@Immutable
data class VisualAccentPreset(
    val label: String,
    val hex: String,
    val color: Color
)

val VisualAccentPresets = listOf(
    VisualAccentPreset("暮紫", "#625CD2", Color(0xFF625CD2)),
    VisualAccentPreset("暖恋", "#D96B8B", Color(0xFFD96B8B)),
    VisualAccentPreset("雾蓝", "#4F7FC8", Color(0xFF4F7FC8)),
    VisualAccentPreset("森林", "#2D7A62", Color(0xFF2D7A62)),
    VisualAccentPreset("琥珀", "#9B6900", Color(0xFF9B6900))
)

@Immutable
data class GlassSurfaceSpec(
    val fill: Color,
    val strongFill: Color,
    val compactFill: Color,
    val border: Color,
    val highlight: Color,
    val shadow: Color,
    val scrim: Color,
    val blurRadiusDp: Float,
    val backdropDetail: Float,
    val usesBackdropBlur: Boolean,
    val contactTint: Color,
    val contactHighlightAlpha: Float,
    val contactShadeAlpha: Float,
    val pressScale: Float,
    val contactRadiusDp: Float,
    val edgeResponseAlpha: Float,
    val releaseDurationMillis: Int
)

@Immutable
data class VisualStatusColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val importance: Color,
    val importanceContainer: Color
)

@Immutable
data class VisualChartColors(
    val trend: Color,
    val affection: Color,
    val trust: Color,
    val interest: Color,
    val positive: Color,
    val neutral: Color,
    val negative: Color
)

@Immutable
data class BackdropSpec(
    val baseStart: Color,
    val baseEnd: Color,
    val primaryBloom: Color,
    val secondaryBloom: Color,
    val userBackdrop: VisualBackdrop,
    val isDark: Boolean,
    val motionScale: Float
)

@Immutable
data class VisualTokens(
    val accent: Color,
    val contentPrimary: Color,
    val contentSecondary: Color,
    val contentMuted: Color,
    val glass: GlassSurfaceSpec,
    val status: VisualStatusColors,
    val chart: VisualChartColors,
    val backdrop: BackdropSpec
)

@Immutable
data class VisualTheme(
    val materialColors: ColorScheme,
    val tokens: VisualTokens,
    val effectTier: GlassEffectTier
)
