package com.companion.cc.ui.stats

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.ui.chat.ConversationStats

/**
 * 沉浸式统计页面 - 保留所有功能，Material 3 Expressive 美化
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmersiveStatsScreen(
    companionId: String,
    onNavigateBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val emotionalHistory by viewModel.emotionalHistory.collectAsState()
    val totalMemoryCount by viewModel.totalMemoryCount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(companionId) {
        viewModel.loadStats(companionId)
    }

    val backgroundColor = getBackgroundForCompanion(companionId)
    val accentColor = getAccentColor(companionId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundColor)
    ) {
        // 顶部栏
        TopAppBar(
            title = {
                Text(
                    text = "${getCompanionName(companionId)}的统计",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, "返回")
                }
            },
            actions = {
                IconButton(onClick = { viewModel.refreshStats(companionId) }) {
                    Icon(Icons.Default.Refresh, "刷新")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 基础统计卡片
                item {
                    BasicStatsCard(stats, accentColor)
                }

                // 对话轮次卡片
                item {
                    ConversationRoundsCard(stats, accentColor)
                }

                // 记忆统计卡片
                item {
                    MemoryStatsCard(stats, totalMemoryCount, accentColor)
                }

                // 当前话题卡片
                item {
                    CurrentTopicsCard(stats, accentColor)
                }

                // 主要特质卡片
                item {
                    TopTraitsCard(stats, accentColor)
                }

                // 情感评分卡片
                item {
                    EmotionalScoreCard(stats, accentColor)
                }

                // 底部间距
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * 基础统计卡片
 */
@Composable
private fun BasicStatsCard(stats: ConversationStats, accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "基础统计",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(
                    icon = Icons.Default.Message,
                    label = "总消息数",
                    value = stats.totalMessages.toString(),
                    accentColor = accentColor
                )
                StatItem(
                    icon = Icons.Default.SwapHoriz,
                    label = "对话轮次",
                    value = stats.conversationRounds.toString(),
                    accentColor = accentColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))

            Spacer(modifier = Modifier.height(16.dp))

            val duration = System.currentTimeMillis() - stats.sessionStartTime
            val hours = duration / (1000 * 60 * 60)
            val minutes = (duration % (1000 * 60 * 60)) / (1000 * 60)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "会话时长: ${hours}小时 ${minutes}分钟",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(accentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(28.dp)
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 对话轮次卡片
 */
@Composable
private fun ConversationRoundsCard(stats: ConversationStats, accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Forum,
                    contentDescription = null,
                    tint = accentColor
                )
                Text(
                    text = "对话轮次",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "完成了 ${stats.conversationRounds} 轮对话",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = (stats.conversationRounds.toFloat() / 100).coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.2f)
            )
        }
    }
}

/**
 * 记忆统计卡片
 */
@Composable
private fun MemoryStatsCard(
    stats: ConversationStats,
    totalMemoryCount: Int,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = accentColor
                )
                Text(
                    text = "记忆统计",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stats.vectorMemoryCount.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "向量记忆",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = totalMemoryCount.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "总记忆数",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * 当前话题卡片
 */
@Composable
private fun CurrentTopicsCard(stats: ConversationStats, accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Topic,
                    contentDescription = null,
                    tint = accentColor
                )
                Text(
                    text = "当前话题",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (stats.currentTopics.isEmpty()) {
                Text(
                    text = "暂无话题",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stats.currentTopics.forEach { topic ->
                        Surface(
                            color = accentColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Circle,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(8.dp)
                                )
                                Text(
                                    text = topic,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 主要特质卡片
 */
@Composable
private fun TopTraitsCard(stats: ConversationStats, accentColor: Color) {
    if (stats.topTraits.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107)
                )
                Text(
                    text = "主要特质",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                stats.topTraits.forEach { trait ->
                    AssistChip(
                        onClick = {},
                        label = { Text(trait) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = accentColor.copy(alpha = 0.15f),
                            labelColor = accentColor,
                            leadingIconContentColor = accentColor
                        )
                    )
                }
            }
        }
    }
}

/**
 * 情感评分卡片
 */
@Composable
private fun EmotionalScoreCard(stats: ConversationStats, accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mood,
                    contentDescription = null,
                    tint = accentColor
                )
                Text(
                    text = "情感评分",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format("%.1f", stats.emotionalScore),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        stats.emotionalScore > 0.5f -> Color(0xFF4CAF50)
                        stats.emotionalScore < -0.5f -> Color(0xFFF44336)
                        else -> Color(0xFFFF9800)
                    }
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            stats.emotionalScore > 0.5f -> "整体情绪积极"
                            stats.emotionalScore < -0.5f -> "整体情绪消极"
                            else -> "情绪中性"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    LinearProgressIndicator(
                        progress = ((stats.emotionalScore + 1f) / 2f).coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = when {
                            stats.emotionalScore > 0.5f -> Color(0xFF4CAF50)
                            stats.emotionalScore < -0.5f -> Color(0xFFF44336)
                            else -> Color(0xFFFF9800)
                        }
                    )
                }
            }
        }
    }
}

// 辅助函数
@Composable
private fun getBackgroundForCompanion(companionId: String): Brush {
    return when (companionId) {
        "muse" -> Brush.verticalGradient(
            listOf(Color(0xFFFAFAFA), Color(0xFFF3E5F5))
        )
        "xiaocan" -> Brush.verticalGradient(
            listOf(Color(0xFFFFFAFA), Color(0xFFFFF0F5))
        )
        else -> Brush.verticalGradient(
            listOf(Color(0xFFFFFFFF), Color(0xFFF5F5F5))
        )
    }
}

@Composable
private fun getAccentColor(companionId: String): Color {
    return when (companionId) {
        "muse" -> Color(0xFF9C88FF)
        "xiaocan" -> Color(0xFFFFB3D9)
        else -> MaterialTheme.colorScheme.primary
    }
}

private fun getCompanionName(companionId: String): String {
    return when (companionId) {
        "muse" -> "缪斯"
        "xiaocan" -> "小璨"
        else -> "伴侣"
    }
}
