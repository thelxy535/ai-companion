// ============================================================
// AuroraMotion.kt · 晨雾琉璃 动效层(§7.1 总表 + §13.4 V3 新四条)
// 来源:《AI伴侣-设计语言规范.md》—— 全部曲线/弹簧参数一一对应
// ============================================================
package com.companion.cc.ui.designsystem

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

// ---------- 曲线 ----------
object AuroraCurves {
    // M3 emphasized ≈ cubic-bezier(.2,0,0,1) —— Compose 内置
    val M3Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    // M-Bubble-in 曲线 bezier(.34,1.3,.5,1)（V8 动效包 ②⑦⑧）
    val BubbleEmphasized: Easing = CubicBezierEasing(0.34f, 1.3f, 0.5f, 1f)
    val FastOutSlowIn: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
}

// ---------- 时长(ms) ----------
object AuroraDuration {
    const val Press = 90
    const val Fade = 150
    const val BubbleIn = 260
    const val TypeRevealChar = 28      // 逐字浮现 每字 stagger
    const val TypeRevealUnit = 200     // 逐字浮现 单字动画
    const val Rise = 280
    const val Sheet = 320
    const val Traverse = 340
    const val Envelope = 420           // 信封拆封(唯一 >340ms 特批)
    const val Energy = 700             // 能量脉冲飞行
    const val HaloCrossfade = 800      // 状态色迁移
}

// ---------- 弹簧 ----------
object AuroraSprings {
    // M-Bubble-in: ζ.78 k420 —— 气泡弹入
    fun <T> bubbleIn(): SpringSpec<T> = spring(dampingRatio = 0.78f, stiffness = 420f)
    // M-Release: ζ.62 k520 —— 按压回弹
    fun <T> release(): SpringSpec<T> = spring(dampingRatio = 0.62f, stiffness = 520f)
}

// ---------- M-Energy 能量脉冲: 二阶贝塞尔轨迹采样 ----------
// from: 我方消息位置(px) / to: 光核位置(px) / apexLift: 顶点上抬(px, 规范 -70dp)
fun energyPulsePath(from: Offset, to: Offset, apexLift: Float): (Float) -> Offset {
    val apex = Offset((from.x + to.x) / 2f, minOf(from.y, to.y) - apexLift)
    return { t ->
        val u = 1f - t
        // quadratic bezier: (1-t)²P0 + 2(1-t)tP1 + t²P2
        Offset(
            x = u * u * from.x + 2f * u * t * apex.x + t * t * to.x,
            y = u * u * from.y + 2f * u * t * apex.y + t * t * to.y,
        )
    }
}

// ---------- 打字指示器三点波形(§6.3): stagger 160ms, 单循环 1.2s ----------
// 返回给定点序号(0/1/2)在时间 t(ms) 的 translateY(dp) 与 alpha
fun typingDotState(index: Int, timeMs: Long): Pair<Float, Float> {
    val phase = ((timeMs + index * 160L) % 1200L) / 1200f          // 0..1
    val wave = sin(phase * 2.0 * PI).toFloat()                      // -1..1, easeInOutSine 近似
    val y = -3f * (wave * 0.5f + 0.5f)                              // 0..-3dp
    val alpha = 0.35f + 0.45f * (wave * 0.5f + 0.5f)               // .35...80
    return y to alpha
}

// ---------- 光核状态枚举(§6.2 / §13.3) ----------
enum class OrbState { Dormant, Idle, Predictive, Listening, Thinking, Speaking, Empathize, Celebrate }

// ---------- 光核呼吸参数表 ----------
data class BreathSpec(val cycleMs: Int, val scaleRange: ClosedFloatingPointRange<Float>, val haloAlpha: ClosedFloatingPointRange<Float>)

object OrbSpecs {
    val Dormant = BreathSpec(6000, 1f..1.03f, 0.20f..0.30f)
    val Idle = BreathSpec(4000, 1f..1.05f, 0.28f..0.40f)
    val Predictive = BreathSpec(2600, 1f..1.05f, 0.28f..0.50f)
    val Empathize = BreathSpec(5000, 1f..1.07f, 0.30f..0.45f)
}

// ---------- 玻璃五档 blur 半径(§5.1, RenderEffect 用) ----------
object GlassBlur {
    const val UltraThin = 40
    const val Thin = 28
    const val Regular = 22
    const val Thick = 18
    const val Solid = 8
}

// ---------- dp→px 便捷 ----------
@Composable
fun Dp.toPxFloat(): Float = with(LocalDensity.current) { this@toPxFloat.toPx() }

// ---------- V8 动效升级包 ①⑥：stagger 逐项进场（设计稿 .stagger 40ms 递进） ----------
// 每屏幕进程内只播一次（切 Tab 返回不重播）；验收③：连续快速切 Tab 不叠加（切走即销毁组合，回来直接终态）。
private val staggerPlayedScreens = mutableSetOf<String>()

/** 在屏幕顶层调用：首个组合返回 true，之后（含切 Tab 返回重建组合）返回 false。 */
@Composable
fun rememberStaggerFirstPlay(routeKey: String): Boolean {
    val first = remember { !staggerPlayedScreens.contains(routeKey) }
    if (first) SideEffect { staggerPlayedScreens.add(routeKey) }
    return first
}

/** 单项进场：m-rise 280ms bezier(.2,0,0,1)（opacity 0→1 + translateY 10dp→0），delay = index*intervalMs，达 cap 不播。 */
@Composable
fun Modifier.staggerRise(
    played: Boolean,
    index: Int,
    intervalMs: Int = 40,
    cap: Int = 8,
    durationMillis: Int = AuroraDuration.Rise
): Modifier {
    if (!played || index >= cap) return this
    val risePx = with(LocalDensity.current) { 10.dp.toPx() }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis, delayMillis = index * intervalMs, easing = AuroraCurves.M3Emphasized))
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = risePx * (1f - progress.value)
    }
}
