package com.companion.cc.ui.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.companion.cc.ui.theme.GlassEffectTier
import com.companion.cc.ui.theme.LocalVisualTheme
import com.companion.cc.R
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
    val visualTheme = LocalVisualTheme.current

    LaunchedEffect(Unit) {
        if (splashDurationMillis(visualTheme.effectTier) == 0L) {
            onNavigateToHome()
            return@LaunchedEffect
        }
        val duration = splashDurationMillis(visualTheme.effectTier)
        // 阶段1: 核心出现
        animationPhase = 1
        delay((duration * 0.34f).toLong())

        // 阶段2: 光环稳定
        animationPhase = 2
        delay((duration * 0.28f).toLong())

        // 阶段3: 轻微转场
        animationPhase = 3
        delay((duration * 0.38f).toLong())

        // 进入首页
        onNavigateToHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Let AppBackdropHost remain visible so Splash and Home share one canvas.
            .background(androidx.compose.ui.graphics.Color.Transparent),
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
    val transitionDuration = when (phase) {
        1 -> 260
        2 -> 180
        else -> 280
    }
    val scale by animateFloatAsState(
        targetValue = when (phase) {
            0 -> 0.82f
            1 -> 1.0f
            2 -> 1.035f
            else -> 1.08f
        },
        animationSpec = tween(
            durationMillis = transitionDuration,
            easing = FastOutSlowInEasing
        ),
        label = "icon_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = when (phase) {
            0 -> 0f        // 初始透明
            1, 2 -> 1f     // 显示
            else -> 0f     // 淡出
        },
        animationSpec = tween(
            durationMillis = transitionDuration,
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
        // Use the actual launcher mark so the opening moment and home screen share one identity.
        Image(
            // Compose cannot load an adaptive mipmap as a Painter; use its vector
            // foreground while the system surfaces keep the full adaptive icon.
            painter = painterResource(R.drawable.ic_launcher_sylora_blue_foreground),
            contentDescription = null,
            modifier = Modifier
                .size(112.dp)
                .graphicsLayer {
                    this.alpha = alpha
                    translationY = when (phase) {
                        0 -> 18f
                        3 -> -10f
                        else -> 0f
                    }
                }
        )
    }
}
