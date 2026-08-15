package com.companion.cc.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/**
 * 主题扩展 - 提供便捷访问语义化颜色的方式
 * 使用方式: CCTheme.colors.textPrimary
 */
object CCTheme {
    val colors: SemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSemanticColors.current
}

/**
 * MaterialTheme的扩展属性，直接访问语义化颜色
 * 使用方式: MaterialTheme.ccColors.textPrimary
 */
val MaterialTheme.ccColors: SemanticColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSemanticColors.current
