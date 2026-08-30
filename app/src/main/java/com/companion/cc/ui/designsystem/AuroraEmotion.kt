// ============================================================
// AuroraEmotion.kt · CC-Switch 情绪六维 token + 语义态色扩展
// 来源:《CC-Switch-全功能视觉设计规格-V2》§四 表4-1/4-2
// 与 AuroraGlassTokens.kt 配套使用
// ============================================================
package com.companion.cc.ui.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class EmotionColors(
    val affection: Color,   // 好感
    val trust: Color,       // 信任
    val interest: Color,    // 兴趣
    val stress: Color,      // 压力(偏高时向 danger 插值)
    val energy: Color,      // 精力
    val mood: Color,        // 心情
)

val EmotionDay = EmotionColors(
    affection = Color(0xFFE0728F),
    trust = Color(0xFF5B9BD5),
    interest = Color(0xFFB08FD8),
    stress = Color(0xFFE0906B),
    energy = Color(0xFF57B894),
    mood = Color(0xFF4F80E8),
)

val EmotionNight = EmotionColors(
    affection = Color(0xFFE88AA4),
    trust = Color(0xFF7FB2E0),
    interest = Color(0xFFC4A8E8),
    stress = Color(0xFFE8A885),
    energy = Color(0xFF6FCBA8),
    mood = Color(0xFF6D9BF1),
)

/* 语义态扩展(补 AuroraGlassTokens) */
@Immutable
data class SemanticColors(
    val success: Color,
    val warning: Color,
    val danger: Color,
    val offline: Color,
)

val SemanticDay = SemanticColors(Color(0xFF2EA985), Color(0xFFD99A3D), Color(0xFFC0392B), Color(0xFF98A1B3))
val SemanticNight = SemanticColors(Color(0xFF4FC49B), Color(0xFFE5B05C), Color(0xFFE56B62), Color(0xFF5B6579))

/* 压力偏高时向 danger 插值(规格 §四) */
fun EmotionColors.stressColor(value: Int, danger: Color): Color =
    if (value >= 70) lerp(stress, danger, ((value - 70) / 30f).coerceIn(0f, 1f)) else stress

private fun lerp(a: Color, b: Color, t: Float): Color =
    Color(a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, a.alpha)
