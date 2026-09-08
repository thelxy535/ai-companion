package com.companion.cc.ui.chat.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.companion.cc.domain.model.Mood
import com.companion.cc.ui.theme.LocalVisualTheme
import java.text.SimpleDateFormat
import java.util.*
import com.companion.cc.ui.components.V9PMDialogSurface
import com.companion.cc.ui.components.V9PMIconButton

/**
 * 情感历史时间线对话框
 * 展示好感度、心情等随时间的变化曲线
 */
@Composable
fun EmotionalTimelineDialog(
    timelineData: List<EmotionalTimelinePoint>,
    onDismiss: () -> Unit
) {
    val chart = LocalVisualTheme.current.tokens.chart
    V9PMDialogSurface(onDismissRequest = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 标题
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "情感历史",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    V9PMIconButton(Icons.Default.Close, "关闭", onDismiss, size = 42.dp, iconSize = 18.dp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 滚动内容
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (timelineData.isEmpty()) {
                        // 空状态
                        EmptyTimelineState()
                    } else {
                        // 好感度曲线
                        TimelineChart(
                            title = "好感度",
                            icon = Icons.Default.Favorite,
                            data = timelineData,
                            valueExtractor = { it.affection },
                            color = chart.affection
                        )

                        Divider()

                        // 信任度曲线
                        TimelineChart(
                            title = "信任度",
                            icon = Icons.Default.Security,
                            data = timelineData,
                            valueExtractor = { it.trust },
                            color = chart.trust
                        )

                        Divider()

                        // 兴趣度曲线
                        TimelineChart(
                            title = "兴趣度",
                            icon = Icons.Default.Star,
                            data = timelineData,
                            valueExtractor = { it.interest },
                            color = chart.interest
                        )

                        Divider()

                        // 心情时间线
                        MoodTimeline(timelineData)
                    }
                }
            }
        }
}

@Composable
private fun EmptyTimelineState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.TrendingUp,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "还没有足够的数据",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "多聊几句就能看到情感变化啦",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun TimelineChart(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    data: List<EmotionalTimelinePoint>,
    valueExtractor: (EmotionalTimelinePoint) -> Int,
    color: Color
) {
    Column {
        // 图表标题
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // 当前值
            Spacer(modifier = Modifier.weight(1f))
            if (data.isNotEmpty()) {
                Text(
                    text = "${valueExtractor(data.last())}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }

        // 曲线图
        if (data.size >= 2) {
            AnimatedLineChart(
                data = data,
                valueExtractor = valueExtractor,
                color = color
            )
        } else {
            Text(
                text = "数据点不足，暂无趋势图",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun AnimatedLineChart(
    data: List<EmotionalTimelinePoint>,
    valueExtractor: (EmotionalTimelinePoint) -> Int,
    color: Color
) {
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label = "chart_animation"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(vertical = 8.dp)
    ) {
        val width = size.width
        val height = size.height
        val padding = 20f

        if (data.size < 2) return@Canvas

        val values = data.map { valueExtractor(it).toFloat() }
        val maxValue = values.maxOrNull() ?: 100f
        val minValue = values.minOrNull() ?: 0f
        val range = maxValue - minValue

        // 计算点的位置
        val stepX = (width - 2 * padding) / (data.size - 1)
        val points = data.mapIndexed { index, point ->
            val x = padding + index * stepX
            val normalizedValue = if (range > 0) {
                (valueExtractor(point) - minValue) / range
            } else 0.5f
            val y = height - padding - (normalizedValue * (height - 2 * padding))
            Offset(x, y)
        }

        // 绘制网格线（淡）
        val gridColor = color.copy(alpha = 0.1f)
        for (i in 0..4) {
            val y = padding + i * (height - 2 * padding) / 4
            drawLine(
                color = gridColor,
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
        }

        // 绘制曲线（动画）
        if (points.size >= 2) {
            val animatedPointCount = (points.size * animationProgress).toInt().coerceAtLeast(2)
            val animatedPoints = points.take(animatedPointCount)

            // 平滑曲线路径
            val path = Path().apply {
                moveTo(animatedPoints[0].x, animatedPoints[0].y)
                for (i in 1 until animatedPoints.size) {
                    val prev = animatedPoints[i - 1]
                    val curr = animatedPoints[i]
                    val controlX = (prev.x + curr.x) / 2
                    quadraticBezierTo(
                        controlX, prev.y,
                        (prev.x + curr.x) / 2, (prev.y + curr.y) / 2
                    )
                    if (i == animatedPoints.size - 1) {
                        lineTo(curr.x, curr.y)
                    }
                }
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )

            // 绘制点
            animatedPoints.forEach { point ->
                drawCircle(
                    color = color,
                    radius = 4f,
                    center = point
                )
            }
        }
    }
}

@Composable
private fun MoodTimeline(data: List<EmotionalTimelinePoint>) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Mood,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "心情变化",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        // 心情时间点列表
        val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        data.takeLast(10).forEach { point ->
            MoodTimelineItem(
                mood = point.mood,
                timestamp = point.timestamp,
                dateFormat = dateFormat
            )
        }
    }
}

@Composable
private fun MoodTimelineItem(
    mood: Mood,
    timestamp: Long,
    dateFormat: SimpleDateFormat
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 时间
        Text(
            text = dateFormat.format(Date(timestamp)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 心情图标
        val moodEmoji = when (mood) {
            Mood.HAPPY -> "😊"
            Mood.EXCITED -> "🤩"
            Mood.CALM -> "😌"
            Mood.SAD -> "😢"
            Mood.TIRED -> "😴"
            Mood.ANXIOUS -> "😰"
            Mood.CONTENT -> "😊"
        }

        Text(
            text = moodEmoji,
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 心情名称
        Text(
            text = mood.name,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * 情感时间线数据点
 */
data class EmotionalTimelinePoint(
    val timestamp: Long,
    val affection: Int,      // 好感度 0-100
    val trust: Int,          // 信任度 0-100
    val interest: Int,       // 兴趣度 0-100
    val mood: Mood           // 心情
)
