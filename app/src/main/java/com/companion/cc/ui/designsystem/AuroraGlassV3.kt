// ============================================================
// AuroraGlassV3.kt 路 閫氶€忕幓鐠冮厤鏂?V3(鍙惤鍦?
// V3 浼樺寲: 鈶犲～鍏?伪 鍏ㄦ。涓嬭皟 鈶ackdrop-filter+saturate 瀵瑰簲 ColorMatrix
//          鈶㈡瀬鍏夋枒澧炶壋 鈶ｆ枃瀛?text-shadow 瀵瑰簲缁樺埗灞?// 鍏ㄩ儴鍙敤 Compose RenderEffect / ColorMatrix 鍘熺敓瀹炵幇
// ============================================================
package com.companion.cc.ui.designsystem

import android.graphics.ColorMatrix
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Canvas
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing

/* 鈺愨晲鈺?浜旀。鐜荤拑 V3 路 閫氶€忛厤鏂?瀵圭収琛?8-1 + V3 浼樺寲) 鈺愨晲鈺?*/
enum class GlassTierV3(
    val fillDay: Color,
    val fillNight: Color,
    val blurRadius: Int,
    val saturation: Float,     // 鈽?V3 鏂板: 鎶樺皠澧炶壋绯绘暟
    val brightness: Float,     // 鈽?V3 鏂板: 浜害琛ュ伩
    val realTime: Boolean,
) {
    UltraThin(Color(0x52FFFFFF), Color(0x522C3652), 40, 1.35f, 1.02f, false),
    Thin(     Color(0x47FFFFFF), Color(0x4D1E2840), 28, 1.45f, 1.03f, false),  // 伪.28/伪.30
    Regular(  Color(0x61FFFFFF), Color(0x6B1A223A), 20, 1.55f, 1.04f, true ),  // 伪.38/伪.42
    Thick(    Color(0x94FFFFFF), Color(0x94141C30), 16, 1.55f, 1.04f, true ),  // 伪.58/伪.58
    Solid(    Color(0xE0FFFFFF), Color(0xD6101626), 8,  1.30f, 1.00f, true ),  // 伪.88/伪.84
}

/* 鈺愨晲鈺?ColorMatrix 楗卞拰搴?浜害鐭╅樀(V3 鏍稿績鍙惤鍦伴厤鏂? 鈺愨晲鈺? * 瀵瑰簲 CSS backdrop-filter: blur(Npx) saturate(S) brightness(B)
 * Compose: RenderEffect.createBlurEffect().asComposeRenderEffect() 鏃犳硶鐩存帴涓茶仈
 *          ColorMatrix, 浣嗗彲浠reateColorMatrixEffect 涓€灞傚涓€灞?(閾惧紡 RenderEffect)
 */
private fun saturationBrightnessMatrix(saturation: Float, brightness: Float): ColorMatrix {
    val matrix = ColorMatrix()
    matrix.setSaturation(saturation)
    val brightnessMatrix = ColorMatrix(floatArrayOf(
        brightness, 0f, 0f, 0f, 0f,
        0f, brightness, 0f, 0f, 0f,
        0f, 0f, brightness, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    ))
    matrix.postConcat(brightnessMatrix)
    return matrix
}

/* 鈺愨晲鈺?Modifier.auroraGlassV3(鍙惤鍦扮増) 鈺愨晲鈺? * 鐢ㄦ硶: Modifier.auroraGlassV3(GlassTierV3.Regular, isNight = false, cornerRadius = 20)
 * 鑷姩鍐崇瓥: 瀹炴椂妯＄硦+saturate(31+) / 闈欐€佸揩鐓?<31) / 绾?alpha(鍏滃簳)
 * 鎬ц兘瀹堝崼: 鍚屽睆瀹炴椂妯＄硦 鈮? (LocalBlurBudget)
 */
@Composable
fun Modifier.auroraGlassV3(
    tier: GlassTierV3,
    isNight: Boolean,
    cornerRadius: Int,
    scrolling: Boolean = false,
    allowRenderEffect: Boolean = true,
): Modifier {
    val fill = if (isNight) tier.fillNight else tier.fillDay
    val colors = if (isNight) AuroraNight else AuroraDay
    val canRealtime = tier.realTime && GlassPerformanceGuard.deviceSupportsRealtime() && !scrolling
    val budget = LocalBlurBudget.current

    var useRealtime by remember(tier, isNight, scrolling) { mutableStateOf(false) }
    DisposableEffect(canRealtime, tier, isNight) {
        val acquired = canRealtime && budget.intValue < GlassPerformanceGuard.MAX_REALTIME_BLUR
        if (acquired) {
            budget.intValue++
            useRealtime = true
        }
        onDispose {
            if (acquired) {
                budget.intValue--
            }
        }
    }

    val shape = RoundedCornerShape(cornerRadius.dp)
    var modifier = this
        .clip(shape)
        .background(fill)

    // 瀹炴椂妯＄硦 + 楗卞拰搴?浜害 ColorMatrix (V3 鏍稿績鏂板)
    if (useRealtime && Build.VERSION.SDK_INT >= 31 && allowRenderEffect) {
        val blurEffect = RenderEffect.createBlurEffect(
            tier.blurRadius.toFloat(), tier.blurRadius.toFloat(),
            Shader.TileMode.CLAMP,
        )
        val colorMatrix = saturationBrightnessMatrix(tier.saturation, tier.brightness)
        val colorEffect = RenderEffect.createColorFilterEffect(
            android.graphics.ColorMatrixColorFilter(colorMatrix),
        )
        // 閾惧紡: blur 鈫?colorFilter
        val chained = RenderEffect.createChainEffect(blurEffect, colorEffect)
        modifier = modifier.graphicsLayer {
            renderEffect = chained.asComposeRenderEffect()
        }
    } else if (!tier.realTime) {
        // 闈欐€佸揩鐓ф。(Thin/UltraThin): 鍙敤 fill+鎻忚竟, 涓嶅姞 blur
        // 鐜荤拑鎰熺敱鏋佸厜閫忓嚭 + alpha 淇濇寔
    }
    // alpha 鍏滃簳: 閮戒笉婊¤冻鏃跺彧淇濈暀 fill(鐜荤拑鎰熸渶浣庣骇浣嗕笉娑堝け)

    return modifier
        .drawBehind {
            // 杈圭紭鍏夊(V3 閿愬寲): 椤朵寒 .42 / 搴曡壊鏁?/ 搴曠紭鍐呭奖 / 165掳 椤堕儴 sheen
            drawRect(brush = Brush.verticalGradient(
                listOf(Color.Transparent, colors.innerDepth),
                startY = size.height * 0.7f, endY = size.height,
            ))
            val stroke = 1.dp.toPx()
            drawRoundRect(
                color = colors.dispersion,
                cornerRadius = CornerRadius(cornerRadius.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
            )
            drawRoundRect(
                color = colors.hairline.copy(alpha = colors.hairline.alpha * 0.6f),
                cornerRadius = CornerRadius(cornerRadius.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
            )
            drawRect(brush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),   // V3: sheen 鍔犱寒鍒?.16
                startY = 0f, endY = size.height * 0.38f,
            ))
        }
}

/* 鈺愨晲鈺?鏋佸厜鑳屾櫙 V3(澧炶壋閫忓嚭婧? 鈺愨晲鈺? * V3: 涓夊潡鏋佸厜鏂戝悇鑷姞 saturate(1.18-1.25) 鈥?閫氳繃 Brush 鎴?ColorFilter 瀹炵幇
 * 钀藉湴: 鐢?Canvas + Brush.radialGradient, 鑹插€兼湰韬凡鎸夊鑹冲悗鍙栧€煎啓鍏?token
 * (鍗?--bg-a/b/c 宸叉槸澧炶壋鍚庣殑鏈€缁堣壊, 鏃犻渶杩愯鏃?filter 鈥斺€?鎬ц兘鏈€浼樿В)
 */
// 宸插湪 AuroraBackground 涓疄鐜? token 鑹插€煎嵆澧炶壋鍚庡€? 鐩存帴缁樺埗鍗冲彲

/* 鈺愨晲鈺?鏂囧瓧鍙鎬цˉ鍋?V3 鏂板) 鈺愨晲鈺? * 鐜荤拑涓婃枃瀛?1px text-shadow 瀵瑰簲 Compose: TextStyle.shadow
 */
// 鐢ㄦ硶: TextStyle(..., shadow = Shadow(color = Color.White.copy(alpha=.4f), offset=Offset(0f,1f), blurRadius=1f))


/* 鈺愨晲鈺?鏋佸厜鑳屾櫙 AuroraBackground锛埪у洓 涓夊眰鑳屾櫙锛?鈺愨晲鈺? * 涓夊潡寰勫悜娓愬彉鏋佸厜鏂?+ 鍩哄簳鑹诧紝缁欑幓鐠冩彁渚?閫忓嚭婧?
 * Day: #D8E4F4 / #EDE4F0 / #E0EEEC 路 Night: #162035 / #1C1A30 / #0F1E2A
 */
@Composable
fun AuroraBackground(
    isNight: Boolean,
    modifier: Modifier = Modifier,
) {
    // V7 设计稿：主题切换 background-color .8s ease——颜色全部走 800ms 缓动
    val animSpec = tween<Color>(800, easing = LinearEasing)
    val base by animateColorAsState(if (isNight) Color(0xFF0E1220) else Color(0xFFF0F3F8), animSpec, label = "abBase")
    val a by animateColorAsState(if (isNight) Color(0xFF162035) else Color(0xFFD8E4F4), animSpec, label = "abA")
    val b by animateColorAsState(if (isNight) Color(0xFF1C1A30) else Color(0xFFEDE4F0), animSpec, label = "abB")
    val c by animateColorAsState(if (isNight) Color(0xFF0F1E2A) else Color(0xFFE0EEEC), animSpec, label = "abC")

    Canvas(modifier.fillMaxSize().background(base)) {
        // 鏋佸厜鏂?1: 宸︿笂
        drawCircle(
            brush = Brush.radialGradient(
                listOf(a, Color.Transparent),
                center = Offset(size.width * 0.05f, size.height * 0.02f),
                radius = size.width * 0.9f,
            ),
        )
        // 鏋佸厜鏂?2: 鍙充笂
        drawCircle(
            brush = Brush.radialGradient(
                listOf(b, Color.Transparent),
                center = Offset(size.width * 0.95f, size.height * 0.10f),
                radius = size.width * 0.8f,
            ),
        )
        // 鏋佸厜鏂?3: 涓嬬紭
        drawCircle(
            brush = Brush.radialGradient(
                listOf(c, Color.Transparent),
                center = Offset(size.width * 0.5f, size.height * 1.05f),
                radius = size.width * 0.95f,
            ),
        )
    }
}

