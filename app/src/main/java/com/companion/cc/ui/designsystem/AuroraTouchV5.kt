// ============================================================
// AuroraTouchV5.kt · CC-Switch 触感系统 V5 终极版
// 在 AuroraTouch.kt 基础上升级: 更弹的 spring + 双色 pressed + accent 描边
// 全部参数可从 HTML V5 直接对照——差异处已标 V5
// ============================================================
package com.companion.cc.ui.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

/* ═══ V5 spring 参数: 更弹(过冲 1.5%) ═══ */
object AuroraTouchSpecV5 {
    // 对应 CSS @keyframes spring-up 500ms cubic-bezier(.28,1.5,.5,1)
    fun <T> releaseSpring(): SpringSpec<T> = spring(
        dampingRatio = 0.55f,           // V5: .6→.55 (更弹)
        stiffness = Spring.StiffnessMedium, // V5: MediumLow→Medium (回弹更快)
    )
    val pressDuration = 90               // M-Press 90ms 不变
    val pressedScale = 0.96f
    val pressedSink = 1.dp               // 下沉 1dp
    val pressedTint = 0.07f              // V5: .06→.07 (accent tint 微提升)
    val pressedStrokeAlpha = 0.18f       // V5 新增: accent 描边亮起
    val pressedStrokeWidth = 1.5.dp
}

/* ═══ V5 pressable: 升级版 ═══ */
fun Modifier.pressableV5(
    onClick: () -> Unit,
    haptic: Boolean = true,
    isNight: Boolean = false,
    outlineShape: Shape = RoundedCornerShape(18.dp),
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val hapticFeedback = LocalHapticFeedback.current
    // V5.1：触感强度设置真正生效——读取全局偏好，经 policy 调幅（OFF 时完全静默）
    val tactilePref = com.companion.cc.ui.theme.LocalTactileIntensityPreference.current
    val appContext = androidx.compose.ui.platform.LocalContext.current
    val tactileController = remember(appContext) {
        com.companion.cc.ui.theme.TactileFeedbackController(appContext)
    }
    val effectTier = com.companion.cc.ui.theme.GlassEffectTier.BALANCED
    val accent = if (isNight) Color(0xFF7AA2F2) else Color(0xFF4A7CE8)
    val accentDeep = if (isNight) Color(0xFF5A85E0) else Color(0xFF3560C0)
    val pressedAccent = if (isPressed) accentDeep else accent

    val springSpec = AuroraTouchSpecV5.releaseSpring<Float>()
    val dpSpringSpec = AuroraTouchSpecV5.releaseSpring<androidx.compose.ui.unit.Dp>()
    val scale by animateFloatAsState(
        if (isPressed) AuroraTouchSpecV5.pressedScale else 1f, springSpec, label = "scale",
    )
    val sink by animateDpAsState(
        if (isPressed) AuroraTouchSpecV5.pressedSink else 0.dp, dpSpringSpec, label = "sink",
    )
    // V5: pressed tint + accent 描边(双色)
    val tint by animateFloatAsState(
        if (isPressed) AuroraTouchSpecV5.pressedTint else 0f, tween(90), label = "tint",
    )
    // V5.2：stroke 动画已随描边移除
    // V5: voice-btn 按压时 accent→accent-deep 变色
    val bgBrightness by animateFloatAsState(
        if (isPressed) 0.88f else 1f, tween(90), label = "brightness",
    )

    this
        // Keep the pressed tint and scale within the visible control outline.
        .clip(outlineShape)
        .graphicsLayer {
            scaleX = scale; scaleY = scale
            translationY = sink.toPx()
        }
        .drawWithContent {
            drawContent()
            // V5: pressed accent tint
            // V5.1：方形色块改圆角（圆钮上不再突兀出方框）
            if (tint > 0f) drawRoundRect(
                color = pressedAccent.copy(alpha = tint),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
            )
            // V5: pressed accent 描边亮起
            // V5.2：描边已移除（画在边界上会伸出按键外）——按压视觉=缩放+色块+压暗，全部在形状内
        }
        // V5: pressed 时全组件亮度降低(对应 CSS filter:brightness(.88) — 仅 voice-btn 用)
        .brightnessV5(if (isPressed) bgBrightness else 1f)
        .clickable(interactionSource = interaction, indication = null) {
            if (haptic) {
                val ok = tactileController.perform(com.companion.cc.ui.theme.TactileGesture.CLICK, tactilePref, effectTier)
                if (!ok) hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) // policy 拒绝时兜底
            }
            onClick()
        }
}

/* V5: Modifier.brightness 扩展(对应 CSS filter:brightness) */
private fun Modifier.brightnessV5(brightness: Float): Modifier = then(
    Modifier.drawWithContent {
        drawContent()
        if (brightness < 1f) {
            // 落地: overlay 半透明黑
            drawRoundRect(Color.Black.copy(alpha = (1f - brightness) * 0.5f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()))
        }
    }
)

/* ═══ 长按气泡交互 ═══ */
fun Modifier.bubbleLongPress(onLongPress: () -> Unit): Modifier = composed {
    val hapticFeedback = LocalHapticFeedback.current
    pointerInput(onLongPress) {
        detectTapGestures(
            onLongPress = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongPress()
            },
        )
    }
}

/* ═══ Aurora 头像：径向渐变 + 内侧描边 ═══ */
@Composable
fun AuroraAvatar(
    modifier: Modifier = Modifier,
    coreColor: Color,
    haloColor: Color,
) {
    Canvas(modifier = modifier.clip(CircleShape)) {
        val radius = size.minDimension / 2f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(coreColor, haloColor, Color.Transparent),
                radius = radius,
            ),
            radius = radius,
        )
        drawCircle(
            color = haloColor,
            radius = (radius - 1.dp.toPx()).coerceAtLeast(0f),
            style = Stroke(1.dp.toPx()),
        )
    }
}


object AuroraPopupSpec {
    val enterDuration = 320   // M3-emphasized 入场
    val exitDuration = 180    // fadeOut 退场
    val enterEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)  // emphasized

    // Compose: AnimatedVisibility(visible, enter = fadeIn(tween(320)) + scaleIn(initialScale=.96f, animationSpec=tween(320, easing=enterEasing)), exit = fadeOut(tween(exitDuration)) + scaleOut(targetScale=.96f, animationSpec=tween(exitDuration)))
    // Scrim: fade 单独 280ms 入 / 180ms 出
}
