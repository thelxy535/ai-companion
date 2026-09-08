package com.companion.cc.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * V9PM 对话框统一进场（m-rise）：fade + 24dp 上浮 + scale 0.95→1，280ms Standard。
 * 用法：包在 Dialog 内容根元素外。
 */
@Composable
fun DialogEnterMotion(content: @Composable () -> Unit) {
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        t.animateTo(1f, tween(280, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)))
    }
    val density = LocalDensity.current
    Box(modifier = Modifier.graphicsLayer {
        alpha = t.value
        val sc = 0.95f + 0.05f * t.value
        scaleX = sc; scaleY = sc
        translationY = with(density) { 24.dp.toPx() } * (1f - t.value)
    }) { content() }
}
