// ============================================================
// AuroraSurface.kt · CC-Switch 玻璃体系组件(§八·补 工程保证协议)
// 性能守卫 + 对比度门禁 + 降级链 —— 旧实现失败根因的正面解法
// ============================================================
package com.companion.cc.ui.designsystem

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/* ── 性能守卫(§八·补 表8-2 第一行) ── */
object GlassPerformanceGuard {
    const val MAX_REALTIME_BLUR = 3          // 同屏实时模糊面上限
    const val SCROLL_RESUME_MS = 250L        // 滚动停止后恢复延迟

    @Composable
    fun deviceSupportsRealtime(): Boolean {
        return androidx.compose.runtime.remember {
            Build.VERSION.SDK_INT >= 31
        }
    }
}

/* 全局实时模糊预算计数(LocalComposition) */
val LocalBlurBudget = compositionLocalOf { mutableIntStateOf(0) }

/* ── 对比度门禁(§八·补 表8-2 第二行) ──
 * maskAlpha = clamp(1 - contrast(ink,bg)/4.5, 0.55, 0.85)
 * 用于背景图上的可读性遮罩
 */
fun readabilityMaskAlpha(inkLum: Float, bgLum: Float): Float {
    val contrast = (inkLum + 0.05f) / (bgLum + 0.05f)
    val needed = 1f - contrast / 4.5f
    return needed.coerceIn(0.55f, 0.85f)
}

/* ── 情绪条迷你形态(4dp 六维分段) ──
 * 规格 §13.3: 收起为 4dp 六维色分段条, 点开展开
 */
@Composable
fun EmotionMiniBar(emotions: Map<String, Int>, e: EmotionColors, modifier: Modifier = Modifier) {
    Row(modifier) {
        val pairs = listOf(
            "aff" to e.affection, "tru" to e.trust, "int" to e.interest,
            "str" to e.stress, "eng" to e.energy, "mood" to e.mood
        )
        // 按六维顺序等权分段; 详细进度在展开面板显示
        pairs.forEach { (_, color) ->
            Box(
                Modifier.weight(1f).height(4.dp).background(color)
            )
        }
    }
}

/* ── 气泡尾角(规格 §五 AppShapes) ── */
object AppShapes {
    val bubbleAI = androidx.compose.foundation.shape.RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)   // AI 左尾 6
    val bubbleMe = androidx.compose.foundation.shape.RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)   // 用户右尾 6
    val card = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
    val sheet = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val dialog = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
}

/* ── 触达(§五) ── */
object AppTouchTargets { val min = 48.dp }
