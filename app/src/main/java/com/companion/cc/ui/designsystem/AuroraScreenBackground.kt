package com.companion.cc.ui.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * V7 页面底色 + 极光三光雾（子路由通用版）。
 * HOME/CHAT 路由底色差异由 VisualThemeResolver 的 BackdropSpec 决定；
 * 这里给非聊天页一个统一的极光氛围（较聊天页收敛：只留左上/右上两雾）。
 */
fun Modifier.auroraScreenBackground(isNight: Boolean): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "screenAurora")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "screenAuroraPhase"
    )
    // V7 丝滑主题：底色/雾色 300ms 渐变（themeMorph）
    val animBase by androidx.compose.animation.animateColorAsState(
        if (isNight) Color(0xFF13141B) else Color(0xFFF9F8FC),
        androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.LinearEasing), label = "auroraBase"
    )
    val animA by androidx.compose.animation.animateColorAsState(
        if (isNight) Color(0xFF5B74C4) else Color(0xFF93BCEE),
        androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.LinearEasing), label = "auroraA"
    )
    val animB by androidx.compose.animation.animateColorAsState(
        if (isNight) Color(0xFF645BB8) else Color(0xFFE2C2EC),
        androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.LinearEasing), label = "auroraB"
    )
    drawBehind {
        val W = size.width
        val H = size.height
        drawRect(animBase)
        // 左上蓝雾
        drawOval(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to animA,
                    0.80f to Color.Transparent,
                    1f to Color.Transparent
                ),
                center = Offset(W * (0.18f + 0.018f * phase), H * 0.05f),
                radius = W * 0.68f
            ),
            topLeft = Offset(W * (0.18f + 0.018f * phase) - W * 0.68f, H * 0.05f - W * 0.68f * 1.3f),
            size = Size(W * 1.36f, W * 1.36f * 1.3f)
        )
        // 右上紫雾
        drawOval(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to animB,
                    0.78f to Color.Transparent,
                    1f to Color.Transparent
                ),
                center = Offset(W * 0.90f, H * 0.10f),
                radius = W * 0.58f
            ),
            topLeft = Offset(W * 0.90f - W * 0.58f, H * 0.10f - W * 0.58f * 1.3f),
            size = Size(W * 1.16f, W * 1.16f * 1.3f)
        )
    }
}
