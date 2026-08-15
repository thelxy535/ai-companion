package com.companion.cc.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.domain.model.Message
import com.companion.cc.ui.chat.components.*
import kotlinx.coroutines.launch

/**
 * 沉浸式聊天屏幕 - 极致体验设计
 */
@Composable
fun ImmersiveChatScreen(
    companionId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val emotionalState by viewModel.emotionalState.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // 自动滚动到最新消息
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    // 动态背景
    val backgroundColor = getBackgroundForCompanion(companionId, emotionalState.mood.name)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundColor)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 顶部栏
            ImmersiveTopBar(
                companionId = companionId,
                emotionalState = emotionalState.mood.name,
                onNavigateBack = onNavigateBack,
                onShowTimeline = { /* TODO: 实现情感历史时间线 */ }
            )

            // 消息列表
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { message ->
                        val index = messages.indexOf(message)
                        val isSequential = index > 0 &&
                                messages[index - 1].role == message.role

                        ImmersiveChatBubble(
                            message = message,
                            companionId = companionId,
                            isSequential = isSequential
                        )
                    }

                    if (isLoading) {
                        item {
                            ImmersiveTypingIndicator(companionId = companionId)
                        }
                    }
                }

                // 滚动到底部按钮
                AnimatedScrollToBottomButton(
                    visible = listState.firstVisibleItemIndex > 5,
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                )
            }

            // 输入框
            ImmersiveMessageInput(
                value = inputText,
                onValueChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(companionId, inputText.trim())
                        inputText = ""
                        focusManager.clearFocus()
                    }
                },
                companionId = companionId
            )
        }
    }
}

/**
 * 沉浸式顶部栏
 */
@Composable
private fun ImmersiveTopBar(
    companionId: String,
    emotionalState: String,
    onNavigateBack: () -> Unit,
    onShowTimeline: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CompanionAvatar(
                    companionId = companionId,
                    size = 40.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = getCompanionName(companionId),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OnlineIndicator()
                        Text(
                            text = getEmotionText(emotionalState),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(onClick = onShowTimeline) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = "情感历史",
                    tint = getEmotionColor(emotionalState)
                )
            }
        }
    }
}

/**
 * 在线指示器
 */
@Composable
private fun OnlineIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "online")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .background(
                color = Color(0xFF4CAF50).copy(alpha = alpha),
                shape = CircleShape
            )
    )
}

/**
 * 滚动到底部按钮
 */
@Composable
private fun BoxScope.AnimatedScrollToBottomButton(
    visible: Boolean,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp),
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 4.dp
            )
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "滚动到底部",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * 根据伴侣和情绪返回动态背景
 */
@Composable
private fun getBackgroundForCompanion(companionId: String, mood: String): Brush {
    return when (companionId) {
        "muse" -> {
            when (mood.lowercase()) {
                "happy", "content" -> Brush.verticalGradient(
                    listOf(Color(0xFFF8F9FA), Color(0xFFF3E5F5))
                )
                else -> Brush.verticalGradient(
                    listOf(Color(0xFFFAFAFA), Color(0xFFF5F5F5))
                )
            }
        }
        "xiaocan" -> {
            when (mood.lowercase()) {
                "happy", "excited" -> Brush.verticalGradient(
                    listOf(Color(0xFFFFF9FB), Color(0xFFFFF0F5))
                )
                "sad" -> Brush.verticalGradient(
                    listOf(Color(0xFFF5F5F5), Color(0xFFEEEEEE))
                )
                else -> Brush.verticalGradient(
                    listOf(Color(0xFFFFFAFA), Color(0xFFFFF5F7))
                )
            }
        }
        else -> Brush.verticalGradient(
            listOf(Color(0xFFFFFFFF), Color(0xFFF5F5F5))
        )
    }
}

private fun getCompanionName(companionId: String): String {
    return when (companionId) {
        "muse" -> "缪斯"
        "xiaocan" -> "小璨"
        else -> "伴侣"
    }
}

private fun getEmotionText(mood: String): String {
    return when (mood.lowercase()) {
        "happy" -> "开心"
        "excited" -> "兴奋"
        "content" -> "满足"
        "calm" -> "平静"
        "sad" -> "有点难过"
        "anxious" -> "焦虑"
        "tired" -> "累了"
        else -> "在线"
    }
}

@Composable
private fun getEmotionColor(mood: String): Color {
    return when (mood.lowercase()) {
        "happy", "excited" -> Color(0xFFFFB74D)
        "content", "calm" -> Color(0xFF81C784)
        "sad" -> Color(0xFF64B5F6)
        "anxious" -> Color(0xFFBA68C8)
        "tired" -> Color(0xFF90A4AE)
        else -> MaterialTheme.colorScheme.primary
    }
}

/**
 * 伴侣头像 - 带呼吸动画
 */
@Composable
fun CompanionAvatar(
    companionId: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "breathe"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = when (companionId) {
                    "muse" -> Brush.radialGradient(
                        listOf(Color(0xFF9C88FF), Color(0xFF6C5CE7))
                    )
                    "xiaocan" -> Brush.radialGradient(
                        listOf(Color(0xFFFFB3D9), Color(0xFFFF7EB3))
                    )
                    else -> Brush.radialGradient(
                        listOf(Color(0xFF90CAF9), Color(0xFF42A5F5))
                    )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (companionId) {
                "muse" -> "🎭"
                "xiaocan" -> "🌸"
                else -> "💬"
            },
            fontSize = (size.value * 0.5).sp
        )
    }
}
