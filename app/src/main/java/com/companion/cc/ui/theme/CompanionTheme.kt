package com.companion.cc.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * 缪斯主题配色 - 深邃冷静
 */
object MuseTheme {
    // 背景渐变
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0A0E27),  // 深蓝紫
            Color(0xFF1A1F3A)   // 略浅的深蓝
        )
    )

    // AI消息气泡
    val aiBubbleBackground = Color(0x14FFFFFF)  // 半透明白色 rgba(255,255,255,0.08)
    val aiBubbleBorder = Color(0x1FFFFFFF)      // rgba(255,255,255,0.12)
    val aiBubbleText = Color(0xFFE8E9F3)

    // 用户消息气泡
    val userBubbleGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF5B6EFF),  // 紫蓝
            Color(0xFF4FACFE)   // 冰蓝
        )
    )
    val userBubbleText = Color.White

    // 输入框
    val inputBackground = Color(0x14FFFFFF)
    val inputBorder = Color(0x1AFFFFFF)
    val inputText = Color(0xFFE8E9F3)
    val inputHint = Color(0x80E8E9F3)

    // 发送按钮
    val sendButtonGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF5B6EFF),
            Color(0xFF4FACFE)
        )
    )

    // 头像背景
    val avatarBackground = Color(0x1AFFFFFF)

    // 文字
    val primaryText = Color(0xFFE8E9F3)
    val secondaryText = Color(0x99E8E9F3)
    val tertiaryText = Color(0x66E8E9F3)
}

/**
 * 小璨主题配色 - 温暖阳光
 */
object XiaoCanTheme {
    // 背景渐变
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFF5F0),  // 浅暖色
            Color(0xFFFFE8E0)   // 淡粉橙
        )
    )

    // AI消息气泡
    val aiBubbleBackground = Color.White
    val aiBubbleBorder = Color(0x33FFA07A)      // 淡粉色边框
    val aiBubbleText = Color(0xFF2C2C2C)
    val aiBubbleShadow = Color(0x0F000000)

    // 用户消息气泡
    val userBubbleGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFB88C),  // 橙粉
            Color(0xFFFFA574)   // 活力橙
        )
    )
    val userBubbleText = Color.White

    // 输入框
    val inputBackground = Color(0xF2FFFFFF)     // 半透明白
    val inputBorder = Color.Transparent
    val inputText = Color(0xFF2C2C2C)
    val inputHint = Color(0x802C2C2C)
    val inputShadow = Color(0x14000000)

    // 发送按钮
    val sendButtonGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFB88C),
            Color(0xFFFFA574)
        )
    )

    // 头像背景
    val avatarBackground = Color.White
    val avatarShadow = Color(0x4DFFA07A)

    // 文字
    val primaryText = Color(0xFF2C2C2C)
    val secondaryText = Color(0x992C2C2C)
    val tertiaryText = Color(0x662C2C2C)
}

/**
 * 根据角色ID获取主题
 */
fun getCompanionTheme(companionId: String): CompanionTheme {
    return when (companionId) {
        "muse" -> CompanionTheme(
            name = "缪斯",
            emoji = "🎭",
            backgroundGradient = MuseTheme.backgroundGradient,
            aiBubbleBackground = MuseTheme.aiBubbleBackground,
            aiBubbleBorder = MuseTheme.aiBubbleBorder,
            aiBubbleText = MuseTheme.aiBubbleText,
            aiBubbleShadow = null,
            userBubbleGradient = MuseTheme.userBubbleGradient,
            userBubbleText = MuseTheme.userBubbleText,
            inputBackground = MuseTheme.inputBackground,
            inputBorder = MuseTheme.inputBorder,
            inputText = MuseTheme.inputText,
            inputShadow = null,
            sendButtonGradient = MuseTheme.sendButtonGradient,
            avatarBackground = MuseTheme.avatarBackground,
            avatarShadow = null,
            primaryText = MuseTheme.primaryText,
            secondaryText = MuseTheme.secondaryText,
            tertiaryText = MuseTheme.tertiaryText,
            bubbleRadiusAi = 16f,    // 硬朗
            bubbleRadiusUser = 16f
        )
        "xiaocan" -> CompanionTheme(
            name = "小璨",
            emoji = "💕",
            backgroundGradient = XiaoCanTheme.backgroundGradient,
            aiBubbleBackground = XiaoCanTheme.aiBubbleBackground,
            aiBubbleBorder = XiaoCanTheme.aiBubbleBorder,
            aiBubbleText = XiaoCanTheme.aiBubbleText,
            aiBubbleShadow = XiaoCanTheme.aiBubbleShadow,
            userBubbleGradient = XiaoCanTheme.userBubbleGradient,
            userBubbleText = XiaoCanTheme.userBubbleText,
            inputBackground = XiaoCanTheme.inputBackground,
            inputBorder = XiaoCanTheme.inputBorder,
            inputText = XiaoCanTheme.inputText,
            inputShadow = XiaoCanTheme.inputShadow,
            sendButtonGradient = XiaoCanTheme.sendButtonGradient,
            avatarBackground = XiaoCanTheme.avatarBackground,
            avatarShadow = XiaoCanTheme.avatarShadow,
            primaryText = XiaoCanTheme.primaryText,
            secondaryText = XiaoCanTheme.secondaryText,
            tertiaryText = XiaoCanTheme.tertiaryText,
            bubbleRadiusAi = 20f,    // 圆润
            bubbleRadiusUser = 20f
        )
        else -> getCompanionTheme("xiaocan")
    }
}

/**
 * 主题数据类
 */
data class CompanionTheme(
    val name: String,
    val emoji: String,
    val backgroundGradient: Brush,
    val aiBubbleBackground: Color,
    val aiBubbleBorder: Color,
    val aiBubbleText: Color,
    val aiBubbleShadow: Color?,
    val userBubbleGradient: Brush,
    val userBubbleText: Color,
    val inputBackground: Color,
    val inputBorder: Color,
    val inputText: Color,
    val inputShadow: Color?,
    val sendButtonGradient: Brush,
    val avatarBackground: Color,
    val avatarShadow: Color?,
    val primaryText: Color,
    val secondaryText: Color,
    val tertiaryText: Color,
    val bubbleRadiusAi: Float,
    val bubbleRadiusUser: Float
)
