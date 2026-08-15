package com.companion.cc.ui.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * 字体缩放比例的 CompositionLocal
 */
val LocalFontScale = compositionLocalOf { 1f }

/**
 * 根据字体大小设置名称获取缩放比例
 */
fun getFontScale(fontSize: String): Float {
    return when (fontSize) {
        "small" -> 0.875f   // 87.5%
        "medium" -> 1f      // 100%
        "large" -> 1.125f   // 112.5%
        "xlarge" -> 1.25f   // 125%
        else -> 1f
    }
}
