package com.companion.cc.ui.memory

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.companion.cc.domain.model.Message
import com.companion.cc.domain.model.MessageRole
import com.companion.cc.domain.model.MessagesByDate
import com.companion.cc.ui.theme.CompactGlassSurface
import java.text.SimpleDateFormat
import java.util.*

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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messagesByDate) { dateGroup ->
            TreeDateNode(dateGroup = dateGroup)
        }
    }
}

@Composable
private fun TreeDateNode(dateGroup: MessagesByDate) {
    var isExpanded by remember { mutableStateOf(true) }

    Column {
        // 日期根节点
        TreeNodeCard(
            icon = Icons.Default.CalendarMonth,
            title = formatDateChinese(dateGroup.date),
            subtitle = "${dateGroup.count} 条记忆",
            isExpanded = isExpanded,
            onToggle = { isExpanded = !isExpanded },
            color = MaterialTheme.colorScheme.primary
        )

        // 展开后显示消息子节点
        if (isExpanded) {
            Column {
                dateGroup.messages.forEachIndexed { index, message ->
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

@Composable
private fun TreeMessageNode(
    message: Message,
    isLast: Boolean,
    level: Int
) {
    var isExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 树状连接线
        TreeConnector(level = level, isLast = isLast)

        // 消息节点
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            TreeNodeCard(
                icon = if (message.role == MessageRole.USER) Icons.Default.Person else Icons.Default.SmartToy,
                title = if (message.role == MessageRole.USER) "用户" else "AI助手",
                subtitle = message.content.take(50) + if (message.content.length > 50) "..." else "",
                isExpanded = isExpanded,
                onToggle = { isExpanded = !isExpanded },
                color = if (message.role == MessageRole.USER)
                    MaterialTheme.colorScheme.tertiary
                else
                    MaterialTheme.colorScheme.secondary,
                importance = message.importance
            )

            // 展开后显示完整内容和详细信息
            if (isExpanded) {
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
            val startY = 0f
            val endY = if (isLast) 32.dp.toPx() else size.height

            // 垂直线
            drawLine(
                color = lineColor,
                start = Offset(startX, startY),
                end = Offset(startX, endY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
            )

            // 水平连接线
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
    CompactGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 内容
            Column(
                modifier = Modifier.weight(1f)
            ) {
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 重要度标记
            if (importance > 70) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "重要",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // 展开/折叠图标
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "折叠" else "展开",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MessageDetailCard(message: Message) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 完整内容
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Divider()

            // 元数据
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 时间
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

                // 情绪
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

                // 重要度
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
