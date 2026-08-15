package com.companion.cc.ui.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.ui.chat.ConversationStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("对话统计") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshStats(companionId) }) {
                        Icon(Icons.Default.Refresh, "刷新")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 基础统计
                item {
                    BasicStatsCard(stats)
                }

                // 对话轮次
                item {
                    ConversationRoundsCard(stats)
                }

                // 主题分布
                item {
                    TopicsCard(stats)
                }

                // 情感评分
                item {
                    EmotionalScoreCard(stats)
                }

                // 记忆统计
                item {
                    MemoryStatsCard(stats, totalMemoryCount)
                }

                // 特质展示
                if (stats.topTraits.isNotEmpty()) {
                    item {
                        TopTraitsCard(stats.topTraits)
                    }
                }
            }
        }
    }
}

@Composable
private fun BasicStatsCard(stats: ConversationStats) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "基础数据",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(
                    icon = Icons.Default.Message,
                    label = "总消息数",
                    value = stats.totalMessages.toString()
                )
                StatItem(
                    icon = Icons.Default.SwapHoriz,
                    label = "对话轮次",
                    value = stats.conversationRounds.toString()
                )
            }

            Divider()

            val duration = System.currentTimeMillis() - stats.sessionStartTime
            val hours = duration / (1000 * 60 * 60)
            val minutes = (duration % (1000 * 60 * 60)) / (1000 * 60)

            Text(
                text = "会话时长: ${hours}小时 ${minutes}分钟",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ConversationRoundsCard(stats: ConversationStats) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "对话详情",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            LinearProgressIndicator(
                progress = if (stats.totalMessages > 0) stats.conversationRounds.toFloat() / stats.totalMessages else 0f,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "完成对话轮次: ${stats.conversationRounds} / ${stats.totalMessages}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TopicsCard(stats: ConversationStats) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "对话主题",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (stats.currentTopics.isEmpty()) {
                Text(
                    text = "暂无主题数据",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                stats.currentTopics.forEach { topic ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Topic,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
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

@Composable
private fun EmotionalScoreCard(stats: ConversationStats) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "情感评分",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            val scorePercent = ((stats.emotionalScore + 1f) / 2f) // 从 [-1, 1] 转换到 [0, 1]
            LinearProgressIndicator(
                progress = scorePercent,
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    stats.emotionalScore > 0.3f -> MaterialTheme.colorScheme.tertiary
                    stats.emotionalScore < -0.3f -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }
            )

            Text(
                text = when {
                    stats.emotionalScore > 0.3f -> "整体情绪偏积极 😊"
                    stats.emotionalScore < -0.3f -> "整体情绪偏消极 😔"
                    else -> "整体情绪较平稳 😌"
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun MemoryStatsCard(stats: ConversationStats, totalMemoryCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "记忆统计",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(
                    icon = Icons.Default.Memory,
                    label = "向量记忆",
                    value = stats.vectorMemoryCount.toString()
                )
                StatItem(
                    icon = Icons.Default.Storage,
                    label = "总记忆数",
                    value = totalMemoryCount.toString()
                )
            }
        }
    }
}

@Composable
private fun TopTraitsCard(traits: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "主要特质",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            traits.forEach { trait ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = trait,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
