package com.companion.cc.ui.memory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.companion.cc.domain.model.Message
import com.companion.cc.domain.model.MessageRole
import com.companion.cc.domain.model.MessagesByDate
import com.companion.cc.ui.theme.GlassEffectTier
import com.companion.cc.ui.theme.LocalVisualTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 树状可视化记忆树组件
 */
@Composable
fun TreeVisualizedMemoryList(
    messagesByDate: List<MessagesByDate>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(
            items = messagesByDate,
            key = { it.date }
        ) { dateGroup ->
            TreeDateNode(dateGroup = dateGroup)
        }
    }
}

@Composable
private fun TreeDateNode(dateGroup: MessagesByDate) {
    var isExpanded by remember { mutableStateOf(true) }
    val expansionMotionEnabled = LocalVisualTheme.current.effectTier != GlassEffectTier.STEADY

    Column {
        TreeNodeCard(
            icon = Icons.Default.CalendarMonth,
            title = formatDateChinese(dateGroup.date),
            subtitle = "${dateGroup.count} 条记忆",
            isExpanded = isExpanded,
            onToggle = { isExpanded = !isExpanded },
            color = MaterialTheme.colorScheme.primary
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = if (expansionMotionEnabled) expandVertically() + fadeIn() else EnterTransition.None,
            exit = if (expansionMotionEnabled) shrinkVertically() + fadeOut() else ExitTransition.None
        ) {
            Column {
                dateGroup.messages.forEachIndexed { index, message ->
                    key(message.id) {
                        TreeMessageNode(
                            message = message,
                            isLast = index == dateGroup.messages.size - 1,
                            level = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TreeMessageNode(
    message: Message,
    isLast: Boolean,
    level: Int
) {
    var isExpanded by remember { mutableStateOf(false) }
    val expansionMotionEnabled = LocalVisualTheme.current.effectTier != GlassEffectTier.STEADY

    Row(modifier = Modifier.fillMaxWidth()) {
        TreeConnector(level = level, isLast = isLast)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            TreeNodeCard(
                icon = if (message.role == MessageRole.USER) Icons.Default.Person else Icons.Default.SmartToy,
                title = if (message.role == MessageRole.USER) "用户" else "AI 助手",
                subtitle = message.content,
                isExpanded = isExpanded,
                onToggle = { isExpanded = !isExpanded },
                color = if (message.role == MessageRole.USER) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.secondary
                },
                importance = message.importance
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = if (expansionMotionEnabled) expandVertically() + fadeIn() else EnterTransition.None,
                exit = if (expansionMotionEnabled) shrinkVertically() + fadeOut() else ExitTransition.None
            ) {
                MessageDetailCard(message = message)
            }
        }
    }
}

@Composable
private fun TreeConnector(level: Int, isLast: Boolean) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = Modifier
            .width((level * 24).dp)
            .height(if (isLast) 32.dp else 64.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val indent = (level - 1) * 24.dp.toPx()
            val startX = indent + 12.dp.toPx()
            val endY = if (isLast) 32.dp.toPx() else size.height

            drawLine(
                color = lineColor,
                start = Offset(startX, 0f),
                end = Offset(startX, endY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
            )
            drawLine(
                color = lineColor,
                start = Offset(startX, 32.dp.toPx()),
                end = Offset(startX + 24.dp.toPx(), 32.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

@Composable
private fun TreeNodeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    color: Color,
    importance: Int = 50
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 4.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(19.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (isExpanded) 3 else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (importance > 70) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "重要",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "折叠" else "展开",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Divider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun MessageDetailCard(message: Message) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 6.dp, end = 4.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = message.content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!message.emotion.isNullOrBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Mood,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = message.emotion,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${message.importance}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDateChinese(date: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("M月d日 (E)", Locale.CHINA)
        val parsedDate = inputFormat.parse(date)
        parsedDate?.let { outputFormat.format(it) } ?: date
    } catch (e: Exception) {
        date
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
