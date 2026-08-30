package com.companion.cc.ui.designsystem

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * V9PM：iPhone 风连续大圆角（严格版）。
 *
 * 与普通圆角的本质区别：转角曲线沿边延伸 [EXTEND] × 半径（普通圆角=1×半径），
 * 控制点置于延伸长度的 0.55 处——曲线在 45° 方向比圆弧更贴近角点（更紧），
 * 但在边缘处切线完全水平/垂直且曲率归零（G2 视觉连续），衔接无折点。
 *
 * 这就是 iPhone 圆角"看起来更大更圆润"的原因：同样的名义半径，过渡区更长。
 */
class SmoothCornerShape(
    private val radius: Dp,
    private val stepsPerCorner: Int = 24,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return Outline.Rectangle(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
        val r = with(density) { radius.toPx() }.coerceAtMost(minOf(w, h) / 2f)
        // 过渡延伸：1.5×半径，钳制到边长（短边较小时避免两角曲线重叠）
        val e = minOf(r * 1.5f, w * 0.46f, h * 0.46f)
        // 控制点：位于延伸长度的 0.55 处（iOS 视觉校准系数）
        val cp = e * 0.55f
        val cp2 = e - cp

        val path = Path()
        // 顶边（左上转角终点 → 右上转角起点）
        path.moveTo(e, 0f)
        path.lineTo(w - e, 0f)
        // 右上：单三次贝塞尔（切线水平进、垂直出，无折点）
        path.cubicTo(w - cp, 0f, w, cp2, w, e)
        path.lineTo(w, h - e)
        // 右下
        path.cubicTo(w, h - cp2, w - cp, h, w - e, h)
        path.lineTo(e, h)
        // 左下
        path.cubicTo(cp, h, 0f, h - cp2, 0f, h - e)
        path.lineTo(0f, e)
        // 左上
        path.cubicTo(0f, cp2, cp, 0f, e, 0f)
        path.close()
        return Outline.Generic(path)
    }
}

fun smoothCorner(radius: Dp): Shape = SmoothCornerShape(radius)

/** 常用半径速查（V9PM 应用位置表） */
object SmoothCorners {
    val settingsCard = smoothCorner(28.dp)
    val dock = smoothCorner(34.dp)
    val memoryTile = smoothCorner(24.dp)
    val dialog = smoothCorner(28.dp)
    val fab = smoothCorner(24.dp)
}
