// ============================================================
// AuroraMaterialV7.kt · CC-Switch 五种材质系统 · V9 Pro Max
// 静态层 + 生命层 + 交互层（材质性格化）
// 性能红线：生命层全部 drawBehind/infiniteTransition，禁 RenderEffect
// 磨砂/织物噪点与织纹为 remember 一次性预生成，运行时只变 alpha
// ============================================================
package com.companion.cc.ui.designsystem

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Shader as AndroidShader
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import kotlin.math.cos
import kotlin.math.sin

/* ═══════════════════════════════════════════════
 * 五种材质枚举 — 用户可在设置页切换
 * V9 Pro Max：材质性格化——玻璃亮/雾面暖/液态流/织物陷/磨砂颗粒立
 * ═══════════════════════════════════════════════ */
enum class MaterialStyle(val label: String) {
    GLASS("玻璃 Glass"),
    MATTE("雾面 Matte"),
    LIQUID("液态 Liquid"),
    FABRIC("织物 Fabric"),
    SANDBLASTED("磨砂 Sandblasted"),
}

/* ═══ 按压内部收集（调用方无感） ═══
 * pointerInput 捕获按下/抬起，发射进私有 interactionSource → collectIsPressedAsState。
 */
private fun Modifier.collectPressInto(source: MutableInteractionSource): Modifier =
    pointerInput(source) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val press = PressInteraction.Press(down.position)
            source.tryEmit(press)
            val up = waitForUpOrCancellation()
            if (up == null) source.tryEmit(PressInteraction.Cancel(press))
            else source.tryEmit(PressInteraction.Release(press))
        }
    }

/** 材质级按压 spring scale（容器反馈，各材质性格不同） */
@Composable
private fun materialPressScale(isPressed: Boolean, pressedScale: Float): Float =
    animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "materialPressScale",
    ).value

/* ═══ MaterialStyle 切换 Modifier ═══
 * 用法: Modifier.materialSurface(style, isNight, shape)
 * shape 接受 SmoothCornerShape（V9PM 连续大圆角），映像/描边跟随形状。
 */
@Composable
fun Modifier.materialSurface(
    style: MaterialStyle,
    isNight: Boolean,
    shape: Shape,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val core = when (style) {
        MaterialStyle.GLASS -> realGlassSurface(isNight, shape, isPressed)
        MaterialStyle.MATTE -> matteSurface(isNight, shape, isPressed)
        MaterialStyle.LIQUID -> liquidSurface(isNight, shape, isPressed)
        MaterialStyle.FABRIC -> fabricSurface(isNight, shape, isPressed)
        MaterialStyle.SANDBLASTED -> sandblastedSurface(isNight, shape, isPressed)
    }
    return this
        .collectPressInto(interactionSource)
        .then(core)
}

/* ═══════════════════════════════════════════════
 * M1 玻璃 MAX ——「活的光」
 * 静态层：底色+极光映像重绘（滚动视差）+白纱+sheen+hairline+内阴影
 * 生命层：映像呼吸（蓝雾 6s 0.90↔0.98，紫雾反相）
 * 交互层：按压映像 alpha 瞬时 +0.06 + spring scale 0.96
 * 性能：纯 drawBehind，无 RenderEffect、无位图
 * ═══════════════════════════════════════════════ */
@Composable
fun Modifier.realGlassSurface(isNight: Boolean, shape: Shape, isPressed: Boolean): Modifier {
    var posInWindow by remember { mutableStateOf(Offset.Zero) }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWpx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHpx = with(density) { configuration.screenHeightDp.dp.toPx() }

    // 生命层：映像呼吸（蓝雾 6s 周期，紫雾反相位）
    val breath by rememberInfiniteTransition(label = "glassBreath").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse),
        label = "glassBreathPhase",
    )
    val blueFactor = 0.90f + 0.08f * breath
    val purpleFactor = 0.90f + 0.08f * (1f - breath)
    // 交互层：按住玻璃，光更亮（瞬时 +0.06）
    val pressBoost = if (isPressed) 0.06f else 0f
    val pressScale = materialPressScale(isPressed, 0.96f)

    // 静态层（V9PM 材质参数过渡 400ms）
    val tint by animateColorAsState(if (isNight) Color(0x4D1E2842) else Color(0x2EFFFFFF), tween(400, easing = AuroraCurves.M3Emphasized), label = "glassTint")
    val base by animateColorAsState(if (isNight) Color(0xFF13141B) else Color(0xFFF9F8FC), tween(400, easing = AuroraCurves.M3Emphasized), label = "glassBase")
    val blobA by animateColorAsState(if (isNight) Color(0xFF5B74C4) else Color(0xFF93BCEE), tween(400, easing = AuroraCurves.M3Emphasized), label = "glassBlobA")
    val blobB by animateColorAsState(if (isNight) Color(0xFF645BB8) else Color(0xFFE2C2EC), tween(400, easing = AuroraCurves.M3Emphasized), label = "glassBlobB")
    val blobC by animateColorAsState(if (isNight) Color(0xFF20364C) else Color(0xFFD5E6E2), tween(400, easing = AuroraCurves.M3Emphasized), label = "glassBlobC")
    val hairline by animateColorAsState(if (isNight) Color(0x29FFFFFF) else Color(0xCCFFFFFF), tween(400, easing = AuroraCurves.M3Emphasized), label = "glassHairline")
    val innerDepth by animateColorAsState(if (isNight) Color(0x33000000) else Color(0x147A94CC), tween(400, easing = AuroraCurves.M3Emphasized), label = "glassInnerDepth")
    val sheenColor by animateColorAsState(if (isNight) Color(0x1AFFFFFF) else Color(0x4DFFFFFF), tween(400, easing = AuroraCurves.M3Emphasized), label = "glassSheen")

    return this
        .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
        .onGloballyPositioned { coords -> posInWindow = coords.positionInWindow() }
        .clip(shape)
        .drawBehind {
            drawRect(base)
            fun auroraBlob(cx: Float, cy: Float, radius: Float, color: Color, alpha: Float) {
                val local = Offset(cx - posInWindow.x, cy - posInWindow.y)
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to color.copy(alpha = (color.alpha * alpha).coerceIn(0f, 1f)),
                            0.85f to Color.Transparent,
                            1f to Color.Transparent
                        ),
                        center = local,
                        radius = radius
                    ),
                    radius = radius,
                    center = local
                )
            }
            auroraBlob(screenWpx * 0.18f, screenHpx * 0.05f, screenWpx * 0.68f * 1.3f, blobA, blueFactor + pressBoost)
            auroraBlob(screenWpx * 0.90f, screenHpx * 0.10f, screenWpx * 0.58f * 1.3f, blobB, purpleFactor + pressBoost)
            auroraBlob(screenWpx * 0.50f, screenHpx * 1.05f, screenWpx * 0.95f * 1.3f, blobC, 0.75f + pressBoost)
            drawRect(tint)
            drawRect(
                brush = Brush.linearGradient(
                    colorStops = arrayOf(0f to sheenColor, 0.38f to Color.Transparent),
                    start = Offset(0f, 0f),
                    end = Offset(size.width * 0.55f, size.height)
                )
            )
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, innerDepth),
                    startY = size.height * 0.70f,
                    endY = size.height
                )
            )
            drawOutline(shape.createOutline(size, layoutDirection, this), hairline, style = Stroke(1.dp.toPx()))
        }
}

/* ═══ M2 雾面 MAX ——「哑光的温度」
 * 静态层：实底 + 噪点 2% + 顶部 4% 微渐变
 * 生命层：按压底色向 accent 偏移 6%（夜间提亮 4%）160ms——漆面被指尖温热
 * 交互层：scale 0.97，无描边亮起
 * ═══════════════════════════════════════════════ */
@Composable
fun Modifier.matteSurface(isNight: Boolean, shape: Shape, isPressed: Boolean): Modifier {
    val fillBase = if (isNight) Color(0xFF161C2E).copy(alpha = 0.92f) else Color(0xFFF0F4FA).copy(alpha = 0.92f)
    val accent = MaterialTheme.colorScheme.primary
    val fill by animateColorAsState(
        when {
            !isPressed -> fillBase
            isNight -> lerp(fillBase, Color.White, 0.04f)   // 夜里的"温度"是变亮
            else -> lerp(fillBase, accent, 0.06f)
        },
        tween(160, easing = AuroraCurves.M3Emphasized),
        label = "matteWarm",
    )
    val pressScale = materialPressScale(isPressed, 0.97f)
    val noisePaint = remember(isNight) {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            alpha = (0.02f * 255f).toInt()
            shader = createNoiseShader(128, 0.30f, 2, seed = 0x4D415454)
        }
    }
    return this
        .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
        .clip(shape)
        .background(fill, shape)
        .drawBehind {
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, noisePaint)
            }
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.04f), Color.Transparent),
                    startY = 0f,
                    endY = size.height * 0.35f
                )
            )
        }
}

/* ═══ M3 液态 MAX ——「活的液体」
 * 静态层：深底 + 三团 14s 流动
 * 生命层：8s 扫光带（20% 宽斜向亮带 alpha 0.10）
 * 交互层：按压亮度 +8% + 流动加速一拍（turbulence 1.0→1.6 弹回），scale 0.965
 * ═══════════════════════════════════════════════ */
@Composable
fun Modifier.liquidSurface(isNight: Boolean, shape: Shape, isPressed: Boolean): Modifier {
    val transition = rememberInfiniteTransition(label = "liquid")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing)),
        label = "phase",
    )
    val sweep by transition.animateFloat(
        initialValue = -0.25f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "liquidSweep",
    )
    val turbulence by animateFloatAsState(if (isPressed) 1.6f else 1f, spring(dampingRatio = 0.55f, stiffness = 400f), label = "liquidTurbulence")
    val pressGlow by animateFloatAsState(if (isPressed) 0.08f else 0f, tween(160), label = "liquidGlow")
    val pressScale = materialPressScale(isPressed, 0.965f)

    val colorA = if (isNight) Color(0xFF9CC4FF) else Color(0xFF8FB8FF)
    val colorB = if (isNight) Color(0xFF7FA0E0) else Color(0xFFC9D9FF)
    val colorC = if (isNight) Color(0xFF12202B) else Color(0xFFE4F0EF)
    val base = if (isNight) Color(0xFF0D1120) else Color(0xFFE8EEF8)

    return this
        .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
        .clip(shape)
        .background(base, shape)
        .drawWithContent {
            drawContent()
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(colorA.copy(alpha = 0.30f), Color.Transparent),
                    center = Offset(
                        size.width * (0.3f + 0.05f * cos(phase) * turbulence),
                        size.height * (0.3f + 0.04f * sin(phase * 0.7f) * turbulence),
                    ),
                    radius = size.width * 0.55f,
                ),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(colorB.copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(
                        size.width * (0.7f + 0.04f * cos(phase + 2f) * turbulence),
                        size.height * (0.65f + 0.05f * sin(phase * 0.5f + 1f) * turbulence),
                    ),
                    radius = size.width * 0.50f,
                ),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(colorC.copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(
                        size.width * (0.45f + 0.03f * cos(phase * 0.3f + 4f) * turbulence),
                        size.height * (0.8f + 0.03f * sin(phase * 0.4f + 2f) * turbulence),
                    ),
                    radius = size.width * 0.60f,
                ),
            )
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.06f)),
                    startY = size.height * 0.6f,
                    endY = size.height
                )
            )
            val c = sweep.coerceIn(0.02f, 0.98f)
            drawRect(
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        (c - 0.10f).coerceAtLeast(0f) to Color.Transparent,
                        c to Color.White.copy(alpha = 0.10f),
                        (c + 0.10f).coerceIn(0f, 1f) to Color.Transparent,
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )
            )
            if (pressGlow > 0f) drawRect(Color.White.copy(alpha = pressGlow))
        }
}

/* ═══ M4 织物 MAX ——「编织的下陷」
 * 静态层：±45° 交叉纹 + 白底 58% + 顶部高光 + 底部内阴影 + 1dp hairline
 * 生命层：按压"下陷"——高光减半 + 内阴影加深 50%（160ms）——布被按下去
 * 交互层：scale 0.98（与玻璃相反的按压方向：玻璃变亮、织物变暗）
 * ═══════════════════════════════════════════════ */
@Composable
fun Modifier.fabricSurface(isNight: Boolean, shape: Shape, isPressed: Boolean): Modifier {
    val base = if (isNight) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.58f)
    val lineColor = if (isNight) Color.White.copy(alpha = 0.06f) else Color(0xFF1C2230).copy(alpha = 0.06f)
    val density = LocalDensity.current
    val fabricPaint = remember(isNight, density, lineColor) {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            shader = createFabricShader(lineColor, density)
        }
    }
    val sink by animateFloatAsState(if (isPressed) 1f else 0f, tween(160), label = "fabricSink")
    val highlightAlpha = 0.10f * (1f - sink * 0.5f)          // 顶部高光减半
    val shadowAlpha = 0.06f * (1f + sink * 0.5f)             // 底部内阴影加深 50%
    val pressScale = materialPressScale(isPressed, 0.98f)

    return this
        .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
        .clip(shape)
        .background(base, shape)
        .drawBehind {
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, fabricPaint)
            }
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = highlightAlpha), Color.Transparent),
                    startY = 0f,
                    endY = size.height * 0.3f
                )
            )
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = shadowAlpha)),
                    startY = size.height * 0.72f,
                    endY = size.height
                )
            )
            drawOutline(shape.createOutline(size, layoutDirection, this), if (isNight) Color(0x29FFFFFF) else Color(0xCCFFFFFF), style = Stroke(1.dp.toPx()))
        }
}

/* ═══ M5 磨砂 MAX ——「颗粒的闪烁」
 * 静态层：半透白 48% + 高密度噪点（固定种子 remember 一次）
 * 生命层：双噪点层 3s crossfade（A 0.12↔0.06，B 反相）= 微光闪烁
 * 交互层：按压颗粒 alpha +4% + scale 0.96
 * 性能：运行时只变 alpha，无 RenderEffect
 * ═══════════════════════════════════════════════ */
@Composable
fun Modifier.sandblastedSurface(isNight: Boolean, shape: Shape, isPressed: Boolean): Modifier {
    val fill by animateColorAsState(if (isNight) Color(0xFF28345A).copy(alpha = 0.48f) else Color.White.copy(alpha = 0.48f), tween(400, easing = AuroraCurves.M3Emphasized), label = "sandFill")
    val noiseA = remember(isNight) {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply { shader = createNoiseShader(128, 0.90f, 2, seed = 0x53414E44) }
    }
    val noiseB = remember(isNight) {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply { shader = createNoiseShader(128, 0.90f, 2, seed = 0x53414E45) }
    }
    val flicker by rememberInfiniteTransition(label = "sandFlicker").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
        label = "sandFlickerPhase",
    )
    val pressNoise by animateFloatAsState(if (isPressed) 0.04f else 0f, tween(160), label = "sandPress")
    val aAlpha = 0.12f - 0.06f * flicker + pressNoise
    val bAlpha = 0.06f + 0.06f * flicker + pressNoise
    val pressScale = materialPressScale(isPressed, 0.96f)

    return this
        .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
        .clip(shape)
        .background(fill, shape)
        .drawBehind {
            noiseA.alpha = (aAlpha.coerceIn(0f, 1f) * 255f).toInt()
            noiseB.alpha = (bAlpha.coerceIn(0f, 1f) * 255f).toInt()
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, noiseA)
                canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, noiseB)
            }
            drawOutline(shape.createOutline(size, layoutDirection, this), if (isNight) Color(0x14FFFFFF) else Color(0x8CFFFFFF), style = Stroke(1.dp.toPx()))
        }
}

/** Create a deterministic, cached grayscale noise texture as a repeatable shader. */
private fun createNoiseShader(
    size: Int,
    baseFrequency: Float,
    octaves: Int,
    seed: Int,
): BitmapShader {
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        for (x in 0 until size) {
            var total = 0f
            var amplitude = 1f
            var frequency = baseFrequency
            var amplitudeTotal = 0f
            repeat(octaves) { octave ->
                val sampleX = kotlin.math.floor(x * frequency).toInt()
                val sampleY = kotlin.math.floor(y * frequency).toInt()
                val hash = ((sampleX * 374761393) xor (sampleY * 668265263) xor (octave * 1442695041) xor seed)
                val value = ((hash xor (hash ushr 13)) * 1274126177) ushr 24
                total += (value / 255f) * amplitude
                amplitudeTotal += amplitude
                amplitude *= 0.5f
                frequency *= 2f
            }
            val gray = ((total / amplitudeTotal).coerceIn(0f, 1f) * 255f).toInt()
            pixels[y * size + x] = android.graphics.Color.argb(255, gray, gray, gray)
        }
    }
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
    return BitmapShader(bitmap, AndroidShader.TileMode.REPEAT, AndroidShader.TileMode.REPEAT)
}

/** Create the 64×64 cross-weave texture once; runtime drawing only tiles its shader. */
private fun createFabricShader(lineColor: Color, density: Density): BitmapShader {
    val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val step = with(density) { 6.dp.toPx() }
    val strokeWidth = with(density) { 1.dp.toPx() }
    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(
            (lineColor.alpha * 255f).toInt(),
            (lineColor.red * 255f).toInt(),
            (lineColor.green * 255f).toInt(),
            (lineColor.blue * 255f).toInt(),
        )
        this.strokeWidth = strokeWidth
        style = AndroidPaint.Style.STROKE
    }
    var offset = -64f
    while (offset < 128f) {
        canvas.drawLine(offset, 0f, offset + 64f, 64f, paint)
        offset += step
    }
    offset = 0f
    while (offset < 128f) {
        canvas.drawLine(offset, 0f, offset - 64f, 64f, paint)
        offset += step
    }
    return BitmapShader(bitmap, AndroidShader.TileMode.REPEAT, AndroidShader.TileMode.REPEAT)
}
