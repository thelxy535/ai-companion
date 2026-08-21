package com.companion.cc.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.companion.cc.domain.model.EmotionalState
import com.companion.cc.domain.model.Mood
import com.companion.cc.ui.chat.ConversationStats
import com.companion.cc.ui.theme.GlassDialogSurface

/**
 * 情感状态弹出对话框
 */
@Composable
fun EmotionalStatsDialog(
    emotionalState: EmotionalState,
    conversationStats: ConversationStats,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassDialogSurface(
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 标题
                Text(
                    text = "当前状态",
                    style = MaterialTheme.typography.titleLarge
                )

                Divider()

                // 心情
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = getMoodEmoji(emotionalState.mood),
                        style = MaterialTheme.typography.displaySmall
                    )
                    Column {
                        Text(
                            text = "心情",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = getMoodText(emotionalState.mood),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Divider()

                // 状态指标
                StatusIndicator(
                    icon = Icons.Default.BatteryChargingFull,
                    label = "精力",
                    value = emotionalState.energy,
                    color = MaterialTheme.colorScheme.primary
                )

                StatusIndicator(
                    icon = Icons.Default.Favorite,
                    label = "好感度",
                    value = emotionalState.affection,
                    color = MaterialTheme.colorScheme.error
                )

                StatusIndicator(
                    icon = Icons.Default.Warning,
                    label = "压力",
                    value = emotionalState.stress,
                    color = MaterialTheme.colorScheme.tertiary
                )

                Divider()

                // 对话统计
                Text(
                    text = "对话统计",
                    style = MaterialTheme.typography.titleSmall
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem("轮次", conversationStats.conversationRounds.toString())
                    StatItem("主题", conversationStats.currentTopics.size.toString())
                }

                // 关闭按钮
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("关闭")
                }
            }
        }
    }
}

@Composable
private fun StatusIndicator(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: Float,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LinearProgressIndicator(
            progress = value,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getMoodEmoji(mood: Mood): String {
    return when (mood) {
        Mood.HAPPY -> "😊"
        Mood.SAD -> "😔"
        Mood.CALM -> "😌"
        Mood.EXCITED -> "🤩"
        Mood.TIRED -> "😴"
        Mood.ANXIOUS -> "😰"
        Mood.CONTENT -> "😊"
    }
}

private fun getMoodText(mood: Mood): String {
    return when (mood) {
        Mood.HAPPY -> "开心"
        Mood.SAD -> "难过"
        Mood.CALM -> "平静"
        Mood.EXCITED -> "兴奋"
        Mood.TIRED -> "疲倦"
        Mood.ANXIOUS -> "焦虑"
        Mood.CONTENT -> "满足"
    }
}
