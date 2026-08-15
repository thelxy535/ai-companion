package com.companion.cc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.companion.cc.domain.model.EmotionalState
import com.companion.cc.domain.model.Mood

/**
 * 情感状态显示卡片
 *
 * 显示 AI 伴侣的当前情感状态
 */
@Composable
fun EmotionalStateCard(
    emotionalState: EmotionalState,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = emotionalState.mood.emoji(),
                    fontSize = 24.sp
                )
                Text(
                    text = "情感状态",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // 心情
            StateItem(
                label = "心情",
                value = emotionalState.mood.displayName(),
                progress = if (emotionalState.mood.isPositive()) 0.8f else 0.4f,
                color = if (emotionalState.mood.isPositive())
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )

            // 精力值
            StateItem(
                label = "精力",
                value = "${(emotionalState.energy * 100).toInt()}%",
                progress = emotionalState.energy,
                color = MaterialTheme.colorScheme.tertiary
            )

            // 好感度
            StateItem(
                label = "好感度",
                value = "${(emotionalState.affection * 100).toInt()}%",
                progress = emotionalState.affection,
                color = MaterialTheme.colorScheme.primary
            )

            // 压力值
            StateItem(
                label = "压力",
                value = "${(emotionalState.stress * 100).toInt()}%",
                progress = emotionalState.stress,
                color = if (emotionalState.stress > 0.7f)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.secondary
            )

            // 总体评分
            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "整体状态",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                val score = emotionalState.overallScore()
                Text(
                    text = when {
                        score > 0.5f -> "😊 很好"
                        score > 0f -> "😌 平稳"
                        score > -0.5f -> "😔 一般"
                        else -> "😢 需要关心"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        score > 0.3f -> MaterialTheme.colorScheme.primary
                        score > -0.3f -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    }
}

/**
 * 状态项
 */
@Composable
private fun StateItem(
    label: String,
    value: String,
    progress: Float,
    color: androidx.compose.ui.graphics.Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

/**
 * 心情显示名称
 */
private fun Mood.displayName(): String {
    return when (this) {
        Mood.HAPPY -> "开心"
        Mood.SAD -> "难过"
        Mood.CALM -> "平静"
        Mood.EXCITED -> "兴奋"
        Mood.TIRED -> "疲惫"
        Mood.ANXIOUS -> "焦虑"
        Mood.CONTENT -> "满足"
    }
}
