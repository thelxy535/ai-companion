package com.companion.cc.ui.chat.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.companion.cc.domain.model.EmotionalState
import com.companion.cc.domain.model.Mood

/**
 * 情感状态栏（紧凑显示）
 */
@Composable
fun EmotionalStatusBar(
    emotionalState: EmotionalState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 心情
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = getMoodEmoji(emotionalState.mood),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = getMoodText(emotionalState.mood),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 精力、好感度、压力（小图标）
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 精力
            MiniStatusIndicator(
                icon = Icons.Default.BatteryChargingFull,
                value = emotionalState.energy,
                color = MaterialTheme.colorScheme.primary
            )

            // 好感度
            MiniStatusIndicator(
                icon = Icons.Default.Favorite,
                value = emotionalState.affection,
                color = MaterialTheme.colorScheme.error
            )

            // 压力
            MiniStatusIndicator(
                icon = Icons.Default.Warning,
                value = emotionalState.stress,
                color = MaterialTheme.colorScheme.tertiary
            )

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "展开详情",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 迷你状态指示器
 */
@Composable
private fun MiniStatusIndicator(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Float,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = color
        )
        Text(
            text = "${(value * 100).toInt()}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 语音监听指示器
 */
@Composable
fun VoiceListeningIndicator(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "正在监听...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/**
 * TTS 播放指示器
 */
@Composable
fun TTSSpeakingIndicator(
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.VolumeUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "正在播放...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        IconButton(onClick = onStop) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = "停止播放",
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
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
