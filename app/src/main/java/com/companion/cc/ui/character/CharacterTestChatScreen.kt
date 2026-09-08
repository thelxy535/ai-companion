package com.companion.cc.ui.character

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.companion.cc.domain.model.Attitude
import com.companion.cc.domain.model.MessageRole
import com.companion.cc.ui.chat.MarkdownText
import com.companion.cc.ui.components.CompanionAvatar
import com.companion.cc.ui.components.V9PMIconButton
import com.companion.cc.ui.components.V9PMTextField
import com.companion.cc.ui.theme.GlassBottomDock
import com.companion.cc.ui.designsystem.AuroraChatTokens
import com.companion.cc.ui.designsystem.AuroraColors
import com.companion.cc.ui.designsystem.AuroraDay
import com.companion.cc.ui.designsystem.AuroraNight
import com.companion.cc.ui.designsystem.GlassTierV3
import com.companion.cc.ui.designsystem.auroraGlassV3
import com.companion.cc.ui.theme.LocalVisualTheme

/**
 * V9PM 试聊屏：创建前测试对话。
 * 未入库草稿直接对话——顶栏态度徽章实时反映脾气状态机，返回向导可继续调整。
 */
@Composable
fun CharacterTestChatScreen(
    onNavigateBack: () -> Unit,
    viewModel: CharacterPreviewViewModel = hiltViewModel(),
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isSending by viewModel.isSending.collectAsStateWithLifecycle()
    val attitude by viewModel.attitude.collectAsStateWithLifecycle()
    val characterName by viewModel.characterName.collectAsStateWithLifecycle()
    val draft by CharacterPreviewStore.draft.collectAsStateWithLifecycle()

    val night = LocalVisualTheme.current.tokens.backdrop.isDark
    val auroraColors = if (night) AuroraNight else AuroraDay
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }

    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 44.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            V9PMIconButton(
                icon = Icons.Default.ArrowBack,
                contentDescription = "返回",
                onClick = onNavigateBack,
                size = 48.dp,
                tint = MaterialTheme.colorScheme.onSurface
            )
            Column(Modifier.weight(1f)) {
                Text(
                    "试聊 · $characterName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "感受 TA 的脾气，返回可继续调整",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val attLabel = when (attitude) {
                Attitude.WARM -> "亲昵"
                Attitude.NEUTRAL -> "正常"
                Attitude.UPSET -> "闹别扭"
                Attitude.COLD -> "冷战"
            }
            val attColor = when (attitude) {
                Attitude.WARM -> Color(0xFF3FA76C)
                Attitude.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
                Attitude.UPSET -> Color(0xFFD69A4F)
                Attitude.COLD -> Color(0xFF5B8DEF)
            }
            Text(
                text = attLabel,
                color = attColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .border(1.dp, attColor.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp)
        ) {
            items(messages) { msg ->
                PreviewBubble(
                    role = msg.role,
                    content = msg.content,
                    isStreaming = msg.isStreaming,
                    night = night,
                    auroraColors = auroraColors,
                    avatarEmoji = draft?.avatar ?: (draft?.name?.take(1) ?: "🎭"),
                    avatarUrl = draft?.avatar?.takeIf { it.startsWith("http") },
                    action = msg.action,
                )
            }
            if (isSending && messages.lastOrNull()?.isStreaming != true) {
                item {
                    Text(
                        "…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                }
            }
        }

        GlassBottomDock(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                V9PMTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = "跟 TA 说点什么…",
                    modifier = Modifier.weight(1f),
                    singleLine = false,
                    maxLines = 4
                )
                V9PMIconButton(
                    icon = Icons.Default.Send,
                    contentDescription = "发送",
                    onClick = {
                        viewModel.send(input)
                        input = ""
                    },
                    enabled = input.isNotBlank() && !isSending,
                    size = 48.dp,
                    tint = if (input.isNotBlank() && !isSending) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** V9PM 试聊气泡：AI=固定玻璃 / 用户=135° 渐变；非对称圆角；m-bubble-in 进场 */
@Composable
private fun PreviewBubble(
    role: MessageRole,
    content: String,
    isStreaming: Boolean,
    night: Boolean,
    auroraColors: AuroraColors,
    avatarEmoji: String,
    avatarUrl: String?,
    action: String? = null,
) {
    val isUser = role == MessageRole.USER
    val bubbleShape = if (isUser)
        RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomEnd = 10.dp, bottomStart = 22.dp)
    else
        RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 10.dp)

    val entrance = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(
            1f,
            androidx.compose.animation.core.tween(
                260,
                easing = androidx.compose.animation.core.CubicBezierEasing(0.34f, 1.3f, 0.5f, 1f)
            )
        )
    }
    val risePx = with(LocalDensity.current) { 16.dp.toPx() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .graphicsLayer {
                alpha = entrance.value
                val s = 0.92f + 0.08f * entrance.value
                scaleX = s; scaleY = s
                translationY = risePx * (1f - entrance.value)
            },
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            CompanionAvatar(avatarUrl = avatarUrl, emoji = avatarEmoji, size = 36.dp)
            Spacer(Modifier.width(8.dp))
        }
        Column(
            modifier = Modifier
                .widthIn(max = LocalConfiguration.current.screenWidthDp.dp * 0.75f)
                .shadow(if (isUser) 4.dp else 0.dp, bubbleShape)
                .clip(bubbleShape)
                .then(
                    if (isUser) Modifier.drawBehind {
                        drawRect(
                            Brush.linearGradient(
                                colors = listOf(auroraColors.bubbleMeStart, auroraColors.bubbleMeEnd),
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end = androidx.compose.ui.geometry.Offset(size.width, size.height)
                            )
                        )
                    } else Modifier.auroraGlassV3(
                        GlassTierV3.Regular,
                        night,
                        AuroraChatTokens.MessageRadius.value.toInt(),
                        scrolling = isStreaming,
                        allowRenderEffect = false
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (content.isNotBlank()) {
                MarkdownText(
                    text = content,
                    color = if (isUser) Color.White else auroraColors.ink
                )
            }
            if (!isUser && !isStreaming && !action.isNullOrBlank()) {
                Text(
                    text = action,
                    color = auroraColors.inkMuted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}
