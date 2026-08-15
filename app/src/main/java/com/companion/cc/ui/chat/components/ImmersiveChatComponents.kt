package com.companion.cc.ui.chat.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.companion.cc.domain.model.Message
import com.companion.cc.domain.model.MessageRole

/**
 * 沉浸式聊天气泡 - Material 3 Expressive 风格
 *
 * 设计特点：
 * - 更大的圆角（24dp）营造柔和感
 - 渐变背景增加视觉深度
 * - 进入动画营造对话感
 * - 根据角色定制视觉风格
 */
@Composable
fun ImmersiveChatBubble(
    message: Message,
    companionId: String,
    isSequential: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == MessageRole.USER

    // 进入动画
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(message.id) {
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ), label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300), label = "alpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = if (isSequential) 2.dp else 8.dp
            )
            .scale(scale)
            .alpha(alpha),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // 对方的头像
            com.companion.cc.ui.chat.CompanionAvatar(
                companionId = companionId,
                size = 40.dp,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        // 消息气泡
        MessageBubbleContent(
            message = message,
            isUser = isUser,
            companionId = companionId
        )
    }
}

@Composable
private fun MessageBubbleContent(
    message: Message,
    isUser: Boolean,
    companionId: String
) {
    val bubbleColors = getBubbleColors(isUser, companionId)

    Box(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .clip(
                RoundedCornerShape(
                    topStart = if (isUser) 24.dp else 8.dp,
                    topEnd = if (isUser) 8.dp else 24.dp,
                    bottomStart = 24.dp,
                    bottomEnd = 24.dp
                )
            )
            .background(
                brush = Brush.verticalGradient(bubbleColors)
            )
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp
            )
    ) {
        Column {
            // 消息内容
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    letterSpacing = 0.15.sp
                ),
                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
            )

            // 时间戳
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatTime(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = if (isUser)
                    Color.White.copy(alpha = 0.7f)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * 根据角色返回独特的气泡颜色
 */
@Composable
private fun getBubbleColors(isUser: Boolean, companionId: String): List<Color> {
    return if (isUser) {
        // 用户消息：Material You Primary 渐变
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer
        )
    } else {
        // 根据角色定制颜色
        when (companionId) {
            "muse" -> {
                // 缪斯：优雅的紫灰渐变（高冷感）
                listOf(
                    Color(0xFFF5F5F7),
                    Color(0xFFEEEEF0)
                )
            }
            "xiaocan" -> {
                // 小璨：温暖的粉白渐变（温暖感）
                listOf(
                    Color(0xFFFFF5F8),
                    Color(0xFFFFF0F3)
                )
            }
            else -> {
                listOf(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.surface
                )
            }
        }
    }
}

/**
 * 伴侣头像 - 带呼吸动画（私有，由 ImmersiveChatScreen 中的公共版本替代）
 */
/*
@Composable
private fun CompanionAvatar(
    companionId: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    // 已移至 ImmersiveChatScreen.kt 作为公共函数
}
*/

/**
 * 打字指示器 - 更生动的动画
 */
@Composable
fun ImmersiveTypingIndicator(
    companionId: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    Row(
        modifier = modifier
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        com.companion.cc.ui.chat.CompanionAvatar(
            companionId = companionId,
            size = 40.dp
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(
                    when (companionId) {
                        "muse" -> Color(0xFFF5F5F7)
                        "xiaocan" -> Color(0xFFFFF5F8)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val offsetY by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = -8f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 600,
                                delayMillis = index * 150,
                                easing = FastOutSlowInEasing
                            ),
                            repeatMode = RepeatMode.Reverse
                        ), label = "dot$index"
                    )

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .offset(y = offsetY.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when (companionId) {
                                    "muse" -> Color(0xFF9C88FF)
                                    "xiaocan" -> Color(0xFFFFB3D9)
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                    )
                }
            }
        }
    }
}

/**
 * 消息输入框 - 沉浸式设计
 */
@Composable
fun ImmersiveMessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    companionId: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 输入框
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        when (companionId) {
                            "muse" -> "有什么想说的..."
                            "xiaocan" -> "跟我说说吧～"
                            else -> "输入消息..."
                        }
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = when (companionId) {
                        "muse" -> Color(0xFF9C88FF)
                        "xiaocan" -> Color(0xFFFFB3D9)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                maxLines = 5
            )

            // 发送按钮
            AnimatedSendButton(
                enabled = value.isNotBlank(),
                onClick = onSend,
                companionId = companionId
            )
        }
    }
}

@Composable
private fun AnimatedSendButton(
    enabled: Boolean,
    onClick: () -> Unit,
    companionId: String
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        ), label = "send_scale"
    )

    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier
            .size(56.dp)
            .scale(scale),
        containerColor = when (companionId) {
            "muse" -> Color(0xFF9C88FF)
            "xiaocan" -> Color(0xFFFFB3D9)
            else -> MaterialTheme.colorScheme.primary
        },
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = if (enabled) 6.dp else 2.dp
        )
    ) {
        Icon(
            imageVector = Icons.Default.Send,
            contentDescription = "发送",
            tint = Color.White
        )
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
