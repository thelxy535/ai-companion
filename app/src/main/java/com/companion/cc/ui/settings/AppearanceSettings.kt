package com.companion.cc.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.runtime.Composable
import com.companion.cc.ui.components.UtilityDivider
import com.companion.cc.ui.components.UtilitySection

@Composable
internal fun ThemeSettingsSection(
    currentTheme: String,
    onThemeChange: (String) -> Unit
) {
    UtilitySection(title = "主题", icon = Icons.Default.Palette) {
        ThemeChoice("跟随系统", "根据系统设置自动切换", "system", currentTheme, onThemeChange)
        UtilityDivider()
        ThemeChoice("明亮模式", "始终使用明亮主题", "light", currentTheme, onThemeChange)
        UtilityDivider()
        ThemeChoice("暗黑模式", "始终使用暗黑主题", "dark", currentTheme, onThemeChange)
    }
}

@Composable
private fun ThemeChoice(
    title: String,
    description: String,
    value: String,
    currentTheme: String,
    onThemeChange: (String) -> Unit
) {
    SettingsSelectionRow(
        title = title,
        description = description,
        selected = currentTheme == value,
        onClick = { onThemeChange(value) }
    )
}

@Composable
internal fun FontSizeSettingsSection(
    currentSize: String,
    onSizeChange: (String) -> Unit
) {
    UtilitySection(title = "字体大小", icon = Icons.Default.TextFields) {
        FontSizeChoice("小", "紧凑显示，适合小屏幕", "small", currentSize, onSizeChange)
        UtilityDivider()
        FontSizeChoice("中（推荐）", "默认大小，平衡舒适", "medium", currentSize, onSizeChange)
        UtilityDivider()
        FontSizeChoice("大", "更易阅读", "large", currentSize, onSizeChange)
        UtilityDivider()
        FontSizeChoice("超大", "最大字体，视力辅助", "xlarge", currentSize, onSizeChange)
    }
}

@Composable
private fun FontSizeChoice(
    title: String,
    description: String,
    value: String,
    currentSize: String,
    onSizeChange: (String) -> Unit
) {
    SettingsSelectionRow(
        title = title,
        description = description,
        selected = currentSize == value,
        onClick = { onSizeChange(value) }
    )
}
