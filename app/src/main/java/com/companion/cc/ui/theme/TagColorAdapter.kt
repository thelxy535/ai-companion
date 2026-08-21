package com.companion.cc.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Safe display adapter for user-persisted tag colors. It never rewrites data. */
object TagColorAdapter {
    fun parse(value: String?, fallback: Color): Color {
        val compact = value?.trim()?.removePrefix("#") ?: return fallback
        val expanded = when (compact.length) {
            3 -> "FF" + compact.map { "$it$it" }.joinToString(separator = "")
            6 -> "FF$compact"
            8 -> compact
            else -> return fallback
        }
        if (!expanded.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
            return fallback
        }
        return runCatching { Color(expanded.toLong(16)) }.getOrDefault(fallback)
    }

    fun contentColor(background: Color): Color =
        if (background.luminance() > 0.42f) Color.Black else Color.White
}
