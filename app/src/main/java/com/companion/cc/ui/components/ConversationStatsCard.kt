package com.companion.cc.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.companion.cc.domain.model.CompanionConfig
import com.companion.cc.ui.chat.ConversationStats

/**
 * 对话统计卡片
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConversationStatsCard(
    stats: ConversationStats,
    companionConfig: CompanionConfig?,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题
            Text(
                text = "对话统计",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // 消息数
            StatRow(
                label = "消息总数",
                value = "${stats.totalMessages} 条"
            )

            // 对话轮次
            StatRow(
                label = "对话轮次",
                value = "第 ${stats.conversationRounds} 轮"
            )

            // 向量记忆
            StatRow(
                label = "长期记忆",
                value = "${stats.vectorMemoryCount} 条"
            )

            // 情感评分
            StatRow(
                label = "情感评分",
                value = when {
                    stats.emotionalScore > 0.5f -> "😊 ${String.format("%.1f", stats.emotionalScore)}"
                    stats.emotionalScore > 0f -> "😌 ${String.format("%.1f", stats.emotionalScore)}"
                    stats.emotionalScore > -0.5f -> "😔 ${String.format("%.1f", stats.emotionalScore)}"
                    else -> "😢 ${String.format("%.1f", stats.emotionalScore)}"
                }
            )

            // 当前主题
            if (stats.currentTopics.isNotEmpty()) {
                Divider()

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "当前主题",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        stats.currentTopics.forEach { topic ->
                            SuggestionChip(
                                onClick = { },
                                label = { Text(topic) },
                                enabled = false
                            )
                        }
                    }
                }
            }

            // 伴侣特质
            companionConfig?.let { config ->
                Divider()

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "核心特质",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = config.personality.coreTraits.take(3).joinToString(" • "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 统计行
 */
@Composable
private fun StatRow(
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
