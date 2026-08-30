package com.companion.cc.ui.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Semantic geometry and colors shared by Aurora chat surfaces. */
object AuroraChatTokens {
    const val MessageWidthFraction = 0.78f
    const val ImportantThreshold = 80
    const val RevealMaxAnimatedChars = 80
    const val TypingStaggerMillis = 150L

    val TopBarHeight: Dp = 60.dp
    val EmotionStripHeight: Dp = 4.dp
    val MessageTailRadius: Dp = 6.dp
    val MessageRadius: Dp = 26.dp
    val MessagePaddingHorizontal: Dp = 16.dp
    val MessagePaddingVertical: Dp = 11.dp
    val MessageGap: Dp = 8.dp
    val DockRadius: Dp = 30.dp
    val DockMarginHorizontal: Dp = 12.dp
    val DockMarginBottom: Dp = 12.dp
    val DockPadding: Dp = 10.dp
    val TouchTarget: Dp = 48.dp
    val StickerHeight: Dp = 60.dp
    val ActionDividerWidth: Dp = 1.dp
    val ActionTopSpacing: Dp = 6.dp
    val ActionText: TextStyle = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontStyle = FontStyle.Italic)
    val BadgeSize: Dp = 20.dp

    fun favoriteColor(isNight: Boolean): Color =
        if (isNight) Color(0xFFFF9DBB) else Color(0xFFD94F78)
}
