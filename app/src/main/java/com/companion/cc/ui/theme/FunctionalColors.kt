package com.companion.cc.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 功能色定义
 * 参考: Ant Design
 */
object FunctionalColors {
    // 成功色
    val success = Color(0xFF52C41A)
    val successLight = Color(0xFFF6FFED)
    val successDark = Color(0xFF389E0D)

    // 警告色
    val warning = Color(0xFFFAAD14)
    val warningLight = Color(0xFFFFFBE6)
    val warningDark = Color(0xFFD48806)

    // 信息色
    val info = Color(0xFF1890FF)
    val infoLight = Color(0xFFE6F7FF)
    val infoDark = Color(0xFF096DD9)

    // 重要标记色
    val important = Color(0xFFFFC107)
    val importantBg = Color(0xFFFFC107).copy(alpha = 0.2f)
    val importantText = Color(0xFFF57C00)
}

/**
 * 伴侣主题色
 */
object CompanionColors {
    val muse = Color(0xFF6750A4)
    val xiaocan = Color(0xFFFF6B6B)
}
