package com.companion.cc.ui.chat.components

import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import android.net.Uri
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size as GSize
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.companion.cc.domain.model.ChatCharacterDisplay
import com.companion.cc.domain.model.EmotionalState
import com.companion.cc.domain.model.Message
import com.companion.cc.domain.model.MessageRole
import com.companion.cc.domain.model.Mood
import com.companion.cc.ui.chat.MarkdownText
import com.companion.cc.ui.chat.StreamingBlurText
import com.companion.cc.ui.designsystem.AuroraChatTokens
import com.companion.cc.ui.designsystem.AuroraColors
import com.companion.cc.ui.designsystem.AuroraCurves
import com.companion.cc.ui.designsystem.AuroraDay
import com.companion.cc.ui.designsystem.AuroraDuration
import com.companion.cc.ui.designsystem.AuroraAvatar
import com.companion.cc.ui.designsystem.AuroraNight
import com.companion.cc.ui.designsystem.AuroraType
import com.companion.cc.ui.designsystem.GlassTierV3
import com.companion.cc.ui.designsystem.auroraGlassV3
import com.companion.cc.ui.designsystem.materialSurface
import com.companion.cc.ui.designsystem.smoothCorner
import com.companion.cc.ui.designsystem.bubbleLongPress
import com.companion.cc.ui.designsystem.pressableV5
import com.companion.cc.ui.components.CompanionAvatar
import com.companion.cc.ui.components.UserAvatar
import com.companion.cc.ui.theme.LocalVisualTheme
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun AuroraChatTopBar(
    companion: ChatCharacterDisplay,
    companionAvatar: String?,
    title: String,
    isBusy: Boolean,
    isTyping: Boolean,
    isOffline: Boolean,
    onBack: () -> Unit,
    onAvatarClick: () -> Unit,
    onTimelineClick: () -> Unit,
    onMenuClick: () -> Unit,
) {
    val visual = LocalVisualTheme.current
    val colors = if (visual.tokens.backdrop.isDark) AuroraNight else AuroraDay
    // V7 顶栏：L2 G-Thick 玻璃承载 + 底部 hairline 分隔
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(if (visual.tokens.backdrop.isDark) Color(0x66141A2E) else Color(0x8CFFFFFF))
            .statusBarsPadding()
            // V7 状态胶囊区约 44dp：顶栏下移避开
            // V7 状态胶囊下移 4dp 后避让联动：44→48dp
            .padding(top = 62.dp)
            .height(AuroraChatTokens.TopBarHeight)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // V7 ghost-btn：42dp 圆形玻璃钮（gt 底 + hairline 内描边）
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (visual.tokens.backdrop.isDark) Color(0x59202844) else Color(0x47FFFFFF))
                .border(1.dp, if (visual.tokens.backdrop.isDark) Color(0x29FFFFFF) else Color(0xB3FFFFFF), CircleShape)
                .pressableV5(onBack, isNight = colors == AuroraNight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.ArrowBack, "返回", modifier = Modifier.size(20.dp), tint = colors.ink)
        }
        // V7 presence：名字+状态 居中
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                title,
                style = AuroraType.NavTitle.copy(fontSize = 16.sp),
                fontWeight = FontWeight.SemiBold,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val status = when {
                isBusy -> "正在思考..."
                isTyping -> "正在回复..."
                isOffline -> "离线模式"
                else -> "在线"
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(6.dp).background(if (isOffline) colors.inkFaint else colors.success, CircleShape))
                Text(status, style = AuroraType.CaptionTime, color = colors.inkMuted)
            }
        }
        // V7 菜单钮：唯一右侧按钮（设计稿三点菜单）
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (visual.tokens.backdrop.isDark) Color(0x59202844) else Color(0x47FFFFFF))
                .border(1.dp, if (visual.tokens.backdrop.isDark) Color(0x29FFFFFF) else Color(0xB3FFFFFF), CircleShape)
                .pressableV5(onMenuClick, isNight = colors == AuroraNight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.MoreVert, "菜单", modifier = Modifier.size(20.dp), tint = colors.ink)
        }
    }
}

@Composable
private fun TopBarIcon(icon: ImageVector, description: String, onClick: () -> Unit, colors: AuroraColors) {
    Icon(icon, description, modifier = Modifier.size(AuroraChatTokens.TouchTarget).pressableV5(onClick, isNight = colors == AuroraNight), tint = colors.inkMuted)
}

/** The six displayed values are derived presentation metrics; they are not persisted domain fields. */
fun deriveEmotionMetrics(state: EmotionalState): List<Pair<String, Float>> {
    val moodScore = if (state.mood.isPositive()) 1f else 0.35f
    val trust = ((state.affection + (1f - state.stress)) / 2f).coerceIn(0f, 1f)
    val interest = ((state.energy + moodScore) / 2f).coerceIn(0f, 1f)
    val mood = ((moodScore + state.energy + (1f - state.stress)) / 3f).coerceIn(0f, 1f)
    return listOf(
        "好感" to state.affection.coerceIn(0f, 1f),
        "信任" to trust,
        "兴趣" to interest,
        "压力" to state.stress.coerceIn(0f, 1f),
        "精力" to state.energy.coerceIn(0f, 1f),
        "心情" to mood,
    )
}

@Composable
fun AuroraEmotionStrip(
    state: EmotionalState,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visual = LocalVisualTheme.current
    val colors = if (visual.tokens.backdrop.isDark) AuroraNight else AuroraDay
    val emotionColors = if (visual.tokens.backdrop.isDark) com.companion.cc.ui.designsystem.EmotionNight else com.companion.cc.ui.designsystem.EmotionDay
    Column(modifier = modifier.fillMaxWidth().pressableV5(onClick, isNight = visual.tokens.backdrop.isDark)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
                .height(AuroraChatTokens.EmotionStripHeight).clip(RoundedCornerShape(99.dp))
                // V7 情绪条描边：hairline 细环（aura 同色相时退化为中性分隔——此处用 hairline 保持通用）
                .border(1.dp, colors.hairline.copy(alpha = 0.7f), RoundedCornerShape(99.dp)),
        ) {
            listOf(emotionColors.affection, emotionColors.trust, emotionColors.interest, emotionColors.stress, emotionColors.energy, emotionColors.mood).forEach { color ->
                Box(Modifier.weight(1f).fillMaxHeight().background(color))
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(AuroraDuration.Traverse, easing = AuroraCurves.M3Emphasized)) + fadeIn(tween(AuroraDuration.Fade)),
            exit = shrinkVertically(tween(AuroraDuration.Traverse, easing = AuroraCurves.M3Emphasized)) + fadeOut(tween(AuroraDuration.Fade)),
        ) {
            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 4.dp).fillMaxWidth()
                    .auroraGlassV3(GlassTierV3.Thin, visual.tokens.backdrop.isDark, 16, allowRenderEffect = false),
            ) {
                deriveEmotionMetrics(state).forEach { (label, value) ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, style = AuroraType.Label, color = colors.inkMuted, modifier = Modifier.width(42.dp))
                        Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(colors.inkFaint.copy(alpha = 0.18f))) {
                            Box(Modifier.fillMaxWidth(value).fillMaxHeight().background(colors.accent, CircleShape))
                        }
                        Text(" ${(value * 100).toInt()}", style = AuroraType.CaptionTime, color = colors.inkMuted)
                    }
                }
            }
        }
    }
}

@Composable
fun AuroraMessageBubble(
    message: Message,
    companionEmoji: String,
    companionAvatar: String?,
    userAvatar: String?,
    displayText: String,
    isStreaming: Boolean,
    onLongPress: () -> Unit,
) {
    val isUser = message.role == MessageRole.USER
    val visual = LocalVisualTheme.current
    val night = visual.tokens.backdrop.isDark
    val colors = if (night) AuroraNight else AuroraDay
    // V7 §5：精确尾巴形状（20dp 圆角 + 底角尖尾）
    val densityPx = androidx.compose.ui.platform.LocalDensity.current.density
    // V7 气泡形状：无尾巴，四角均匀大圆角 + 朝向侧小角（右下/左下 ~10dp）
    val shape = if (isUser) {
        RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomEnd = 10.dp, bottomStart = 22.dp)
    } else {
        RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 10.dp)
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!isUser) {
            CompanionAvatar(avatarUrl = companionAvatar, emoji = companionEmoji, size = 40.dp)
            Spacer(Modifier.width(8.dp))
        }
        Column(
            Modifier.widthIn(max = LocalConfiguration.current.screenWidthDp.dp * AuroraChatTokens.MessageWidthFraction),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            Column(
                Modifier.shadow(
                        if (isUser) 4.dp else 0.dp,
                        shape,
                        ambientColor = if (night) Color(0x59000000) else Color(0x121C2230),
                        spotColor = if (night) Color(0x59000000) else Color(0x121C2230)
                    )
                    .clip(shape)
                    .then(if (isUser) Modifier.drawBehind {
                        // V7 --bubble-me: linear-gradient(135deg) 左上→右下
                        drawRect(Brush.linearGradient(
                            colors = listOf(colors.bubbleMeStart, colors.bubbleMeEnd),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(size.width, size.height)
                        ))
                    }
                    else Modifier.auroraGlassV3(GlassTierV3.Regular, night, AuroraChatTokens.MessageRadius.value.toInt(), scrolling = isStreaming, allowRenderEffect = false))
                    // V7 hairline 描边：AI 玻璃气泡在任何背景区都保持"浮起"
                    .then(if (!isUser) Modifier.border(1.dp, colors.hairline.copy(alpha = 0.55f), shape) else Modifier)
                    .pressableV5({}, isNight = night)
                    .bubbleLongPress(onLongPress)
                    .padding(horizontal = AuroraChatTokens.MessagePaddingHorizontal, vertical = AuroraChatTokens.MessagePaddingVertical),
                verticalArrangement = Arrangement.spacedBy(AuroraChatTokens.MessageGap),
            ) {
                if (isUser && !message.imageUrl.isNullOrBlank()) {
                    AsyncImage(message.imageUrl, "用户发送的图片", Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                }
                if (displayText.isNotBlank()) {
                    if (isStreaming) {
                    StreamingBlurText(displayText, isStreaming, if (isUser) Color.White else colors.ink)
                } else {
                    MarkdownText(displayText, color = if (isUser) Color.White else colors.ink)
                }
                }
                if (!isUser && !isStreaming && !message.action.isNullOrBlank()) {
                    AuroraActionCap(message.action, colors)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (message.isFavorited) Icon(Icons.Default.Favorite, "已收藏", Modifier.size(16.dp), tint = AuroraChatTokens.favoriteColor(night))
                    if (message.importance > AuroraChatTokens.ImportantThreshold) Icon(Icons.Default.Star, "重要", Modifier.size(16.dp), tint = visual.tokens.status.importance)
                }
            }
            // V7 设计稿：时间戳为分组分隔（列表层承担），不贴在每条气泡下
        }
        // V7 设计稿：用户侧无头像（只有 AI 侧有）
    }
}

@Composable
private fun AuroraActionCap(action: String, colors: AuroraColors) {
    Column(Modifier.fillMaxWidth().padding(top = AuroraChatTokens.ActionTopSpacing)) {
        Box(Modifier.fillMaxWidth().height(AuroraChatTokens.ActionDividerWidth).drawBehind {
            val dash = 4.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(colors.hairline, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset((x + dash).coerceAtMost(size.width), 0f), strokeWidth = size.height)
                x += dash * 2
            }
        })
        Text(action, style = AuroraChatTokens.ActionText, color = colors.inkMuted, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
fun AuroraTypingIndicator(companionEmoji: String, companionAvatar: String?) {
    val visual = LocalVisualTheme.current
    val night = visual.tokens.backdrop.isDark
    val colors = if (night) AuroraNight else AuroraDay
    // V8 ⑦ 打字指示器进场：m-bubble-in 260ms（scale .92 + 16dp 上移 + 淡入）
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrance.animateTo(1f, tween(AuroraDuration.BubbleIn, easing = AuroraCurves.BubbleEmphasized)) }
    val risePx = with(LocalDensity.current) { 16.dp.toPx() }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .graphicsLayer {
                alpha = entrance.value
                val s = 0.92f + 0.08f * entrance.value
                scaleX = s; scaleY = s
                translationY = risePx * (1f - entrance.value)
            },
        verticalAlignment = Alignment.Bottom
    ) {
        CompanionAvatar(avatarUrl = companionAvatar, emoji = companionEmoji, size = 40.dp)
        Spacer(Modifier.width(8.dp))
        Row(
            Modifier.auroraGlassV3(GlassTierV3.Regular, night, AuroraChatTokens.MessageRadius.value.toInt(), allowRenderEffect = false)
                .padding(horizontal = 18.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            val transition = rememberInfiniteTransition(label = "auroraTyping")
            repeat(3) { index ->
                val y by transition.animateFloat(0f, -3f, infiniteRepeatable(tween(AuroraDuration.TypeRevealUnit, delayMillis = index * AuroraChatTokens.TypingStaggerMillis.toInt()), RepeatMode.Reverse), label = "typingDot$index")
                Box(Modifier.size(7.dp).offset(y = y.dp).background(colors.inkMuted, CircleShape))
            }
        }
    }
}

@Composable
fun AuroraChatInputDock(
    message: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onImageClick: () -> Unit,
    onVoiceClick: () -> Unit,
    selectedImageUri: Uri?,
    onClearImage: () -> Unit,
    isSending: Boolean,
    isListening: Boolean,
    materialStyle: String = "GLASS",
) {
    val visual = LocalVisualTheme.current
    val night = visual.tokens.backdrop.isDark
    val colors = if (night) AuroraNight else AuroraDay
    var focused by remember { mutableStateOf(false) }
    // V8 ⑤ Dock 聚焦光晕：描边 → accent alpha 0→0.30（160ms），失焦回落
    val dockGlow by animateFloatAsState(if (focused) 0.30f else 0f, tween(160), label = "dockGlow")
    val dockMaterial = try {
        com.companion.cc.ui.designsystem.MaterialStyle.valueOf(materialStyle)
    } catch (e: IllegalArgumentException) {
        com.companion.cc.ui.designsystem.MaterialStyle.GLASS
    }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = AuroraChatTokens.DockMarginHorizontal, vertical = 4.dp)
            .materialSurface(dockMaterial, night, smoothCorner(34.dp))
            // V7 gx：顶部 1.2px 白高光 + 内侧渐变暗缘（玻璃厚度感）
            .drawBehind {
                val hair = if (night) Color(0x29FFFFFF) else Color(0xCCFFFFFF)
                drawLine(hair, Offset(0f, 1f), Offset(size.width, 1f), 1.2f)
            }
            .border(1.dp, colors.accent.copy(alpha = dockGlow), RoundedCornerShape(AuroraChatTokens.DockRadius.value.toInt().dp))
            .padding(AuroraChatTokens.DockPadding),
    ) {
        AnimatedVisibility(
            visible = selectedImageUri != null,
            enter = fadeIn(tween(AuroraDuration.Fade)),
            exit = fadeOut(tween(AuroraDuration.Fade)),
        ) {
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(selectedImageUri, "图片预览", Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(8.dp)); Text("已选择图片", style = AuroraType.Label, color = colors.inkMuted)
                Spacer(Modifier.weight(1f)); Icon(Icons.Default.Close, "取消选择", Modifier.size(40.dp).pressableV5(onClearImage, isNight = night), tint = colors.inkMuted)
            }
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // V7 plus-btn：gt 玻璃圆底 44dp
            Box(
                modifier = Modifier
                    .size(AuroraChatTokens.TouchTarget)
                    .clip(CircleShape)
                    .background(if (night) Color(0x50323E5F) else Color(0x66FFFFFF))
                    .pressableV5(onImageClick, isNight = night),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Photo, "选择图片", Modifier.size(22.dp), tint = colors.ink)
            }
            BasicTextField(
                value = message,
                onValueChange = onMessageChange,
                modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused },
                textStyle = AuroraType.BodyChat.copy(color = colors.ink),
                maxLines = 5,
                decorationBox = { inner ->
                    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp)) {
                        if (message.isEmpty()) Text(if (selectedImageUri != null) "描述一下这张图片..." else "输入消息...", style = AuroraType.BodyChat, color = colors.inkFaint)
                        inner()
                    }
                },
            )
            // V7 voice-btn：accent 圆底白图标
            Box(
                modifier = Modifier
                    .size(AuroraChatTokens.TouchTarget)
                    .clip(CircleShape)
                    .background(if (isListening) colors.danger else colors.accent)
                    .pressableV5(onVoiceClick, isNight = night),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Mic, "语音输入", Modifier.size(22.dp), tint = Color.White)
            }
            val enabled = isSending || message.isNotBlank() || selectedImageUri != null
            // V7 发送钮 3 态：disabled 灰 icon / enabled accent 圆底白箭头 / sending 红色 Stop
            Box(
                modifier = Modifier
                    .size(AuroraChatTokens.TouchTarget)
                    .then(
                        when {
                            enabled -> Modifier.background(colors.accent, CircleShape)
                            isSending -> Modifier.background(colors.danger.copy(alpha = 0.16f), CircleShape)
                            else -> Modifier.background(colors.inkFaint.copy(alpha = 0.10f), CircleShape)
                        }
                    )
                    .pressableV5(if (enabled) onSend else if (isSending) onStop else ({}), isNight = night),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (enabled) Icons.Default.Send else if (isSending) Icons.Default.Stop else Icons.Default.Send,
                    contentDescription = if (enabled) "发送" else if (isSending) "停止生成" else "发送",
                    modifier = Modifier.size(22.dp),
                    tint = when {
                        enabled -> Color.White
                        isSending -> colors.danger
                        else -> colors.inkFaint
                    },
                )
            }
        }
    }
}

private fun formatMessageTime(timestamp: Long): String = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(timestamp))




/**
 * V7 §5 气泡尾巴：圆角矩形 + 尾部小尖角（AI 左下 / 用户右下）
 * tailSpec: 尾巴宽 10dp 高 7dp，从底角向外弯出（CSS 近似为 6dp 圆角，此为精确形状）
 */
class BubbleTailShape(
    private val radius: Float,      // dp
    private val tailAtBottomRight: Boolean, // true=用户气泡（右下尾）
    private val density: Float,
) : androidx.compose.ui.graphics.Shape {
    private fun Float.px() = this * density
    override fun createOutline(size: GSize, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Outline {
        val r = radius.coerceAtMost(size.width / 2f).coerceAtMost(size.height / 2f)
        val tailW = 12f.px()
        val tailH = 8f.px()
        val path = androidx.compose.ui.graphics.Path()
        if (!tailAtBottomRight) {
            // AI 气泡：尾巴在左下，向下凸出
            path.moveTo(0f, r)
            path.quadraticBezierTo(0f, 0f, r, 0f)                          // 左上圆角
            path.lineTo(size.width - r, 0f)
            path.quadraticBezierTo(size.width, 0f, size.width, r)          // 右上圆角
            path.lineTo(size.width, size.height - r)
            path.quadraticBezierTo(size.width, size.height, size.width - r, size.height) // 右下圆角
            path.lineTo(tailW, size.height)                                // 底边向左
            // 尾巴：从底边向下再收回左边缘
            path.quadraticBezierTo(tailW * 0.35f, size.height + tailH, 0f, size.height - tailH * 0.2f)
            path.close()
        } else {
            // 用户气泡：尾巴在右下，向下凸出
            path.moveTo(size.width - r, 0f)
            path.quadraticBezierTo(size.width, 0f, size.width, r)
            path.lineTo(size.width, size.height - tailH * 0.2f)
            // 尾巴：从右边缘向下凸出再收回底边
            path.quadraticBezierTo(size.width - tailW * 0.35f, size.height + tailH, size.width - tailW, size.height)
            path.lineTo(r, size.height)
            path.quadraticBezierTo(0f, size.height, 0f, size.height - r)   // 左下圆角
            path.lineTo(0f, r)
            path.quadraticBezierTo(0f, 0f, r, 0f)
            path.close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}
