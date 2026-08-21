package com.companion.cc.ui.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.companion.cc.ui.theme.GlassEffectTier
import com.companion.cc.ui.theme.LocalVisualTheme
import kotlinx.coroutines.delay

/**
 * 优雅的开屏动画
 *
 * 设计理念：
 * - 简洁的品牌符号
 * - 温暖的渐变背景
 * - 柔和的呼吸动画
 * - 快速过渡（0.8秒）
 *
 * 参考：Locket、Replika 的情感化设计
 */
@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit
) {
    var animationPhase by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        // 阶段1: 淡入 (0-300ms)
        animationPhase = 1
        delay(300)

        // 阶段2: 呼吸停留 (300-500ms)
        animationPhase = 2
        delay(200)

        // 阶段3: 淡出 (500-800ms)
        animationPhase = 3
        delay(300)

        // 进入首页
        onNavigateToHome()
    }

    val visualTheme = LocalVisualTheme.current
    val backgroundColors = listOf(
        visualTheme.tokens.backdrop.baseStart,
        visualTheme.tokens.backdrop.baseEnd
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(backgroundColors)
            ),
        contentAlignment = Alignment.Center
    ) {
        // 品牌符号动画
        AnimatedBrandIcon(
            phase = animationPhase,
        allowDecorativeMotion = visualTheme.effectTier != GlassEffectTier.STEADY
        )
    }
}

/**
 * 品牌符号动画
 */
@Composable
private fun AnimatedBrandIcon(
    phase: Int,
    allowDecorativeMotion: Boolean
) {
    // 缩放动画
    val scale by animateFloatAsState(
        targetValue = when (phase) {
            0 -> 0.8f      // 初始缩小
            1 -> 1.0f      // 淡入到正常
            2 -> 1.05f     // 呼吸放大
            else -> 1.1f   // 淡出略放大
        },
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "icon_scale"
    )

    // 透明度动画
    val alpha by animateFloatAsState(
        targetValue = when (phase) {
            0 -> 0f        // 初始透明
            1, 2 -> 1f     // 显示
            else -> 0f     // 淡出
        },
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "icon_alpha"
    )

    val breatheScale = if (allowDecorativeMotion) {
        rememberInfiniteTransition(label = "breathe").animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breathe_scale"
        ).value
    } else {
        1f
    }

    Box(
        modifier = Modifier
            .scale(scale * if (phase == 2) breatheScale else 1f),
        contentAlignment = Alignment.Center
    ) {
        // 简洁的品牌符号
        Text(
            text = "💬",
            fontSize = 72.sp,
            modifier = Modifier.graphicsLayer(alpha = alpha)
        )
    }
}
