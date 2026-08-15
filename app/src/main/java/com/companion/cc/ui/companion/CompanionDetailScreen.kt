package com.companion.cc.ui.companion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.domain.model.companions
import com.companion.cc.ui.chat.ChatViewModel
import com.companion.cc.ui.components.CompanionAvatar
import java.util.concurrent.TimeUnit

/**
 * 角色详情页
 *
 * 设计原则：
 * - 像朋友的个人主页，不是数据面板
 * - 情感化表达，不是数字统计
 * - 温暖自然，不是冰冷工具
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionDetailScreen(
    companionId: String,
    onNavigateBack: () -> Unit,
    onEditAvatar: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val companion = remember(companionId) {
        companions.find { it.id == companionId } ?: companions[0]
    }

    // 从ViewModel获取真实数据
    val messages by viewModel.messages.collectAsState()
    val conversationStats by viewModel.conversationStats.collectAsState()
    val emotionalState by viewModel.emotionalState.collectAsState()
    val companionAvatar by viewModel.companionAvatar.collectAsState()

    // 加载消息
    LaunchedEffect(companionId) {
        viewModel.loadMessages(companionId)
    }

    // 计算真实数据
    val messageCount = messages.size
    val firstMetDate = remember(messages) {
        messages.firstOrNull()?.timestamp ?: System.currentTimeMillis()
    }
    val recentMood = remember(emotionalState) {
        when {
            emotionalState.affection > 0.7f -> "最近好像心情不错"
            emotionalState.affection > 0.4f -> "最近还挺平静的"
            else -> "最近可能有点累"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于她") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 头像（大图）
            Box(
                contentAlignment = Alignment.BottomEnd
            ) {
                CompanionAvatar(
                    avatarUrl = companionAvatar,
                    emoji = companion.emoji,
                    size = 120.dp
                )

                // 编辑按钮
                SmallFloatingActionButton(
                    onClick = onEditAvatar,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "更换头像",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 名字
            Text(
                text = companion.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 简单描述
            Text(
                text = companion.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 信息卡片（情感化表达）
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 认识时间
                    InfoRow(
                        label = "相识",
                        value = formatTimeSince(firstMetDate)
                    )

                    Divider()

                    // 聊天情况（简化表达）
                    InfoRow(
                        label = "聊天",
                        value = when {
                            messageCount < 50 -> "刚开始认识"
                            messageCount < 200 -> "聊了不少"
                            messageCount < 500 -> "聊了好久了"
                            else -> "老朋友了"
                        }
                    )

                    Divider()

                    // 最近状态
                    InfoRow(
                        label = "最近",
                        value = recentMood
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 性格特点卡片
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "关于 ${companion.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = companion.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 24.sp
                    )
                }
            }

            // 底部留白
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * 信息行组件
 */
@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 格式化时间差（人性化表达）
 */
private fun formatTimeSince(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    val days = TimeUnit.MILLISECONDS.toDays(diff)
    val months = days / 30
    val years = days / 365

    return when {
        days < 7 -> "刚认识"
        days < 30 -> "认识${days}天了"
        months < 12 -> "认识${months}个月了"
        else -> "认识${years}年了"
    }
}
