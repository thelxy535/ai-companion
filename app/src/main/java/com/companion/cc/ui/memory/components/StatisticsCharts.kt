package com.companion.cc.ui.memory.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.companion.cc.ui.theme.LocalVisualTheme
import com.companion.cc.ui.theme.VisualChartColors
import kotlin.math.max

/**
 * 消息趋势折线图
 * 显示每日消息数量趋势
 */
@Composable
fun MessageTrendChart(
    dailyData: List<DailyMessageData>,
    modifier: Modifier = Modifier
) {
    val visualTheme = LocalVisualTheme.current
    val trendColor = visualTheme.tokens.chart.trend
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val pointCenterColor = MaterialTheme.colorScheme.surface
    val chartMotionEnabled = chartAnimationEnabled(visualTheme.effectTier)
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = if (chartMotionEnabled) 1000 else 0,
            easing = FastOutSlowInEasing
        ),
        label = "chart_animation"
    )

    Column(modifier = modifier) {
        // 标题
        Text(
            text = "消息趋势",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (dailyData.isEmpty()) {
            Text(
                text = "暂无数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        // 图表
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(vertical = 16.dp)
        ) {
            val width = size.width
            val height = size.height
            val padding = 40f

            // 计算最大值
            // Room flows can briefly expose a date group before its messages are
            // loaded. Keep Canvas coordinates finite during that transition.
            val maxValue = dailyData.maxOfOrNull { it.count.coerceAtLeast(0) }
                ?.toFloat()
                ?.coerceAtLeast(1f)
                ?: 1f
            val drawableHeight = height - padding * 2

            // 绘制网格线
            for (i in 0..4) {
                val y = padding + (drawableHeight / 4) * i
                drawLine(
                    color = gridColor,
                    start = Offset(padding, y),
                    end = Offset(width - padding, y),
                    strokeWidth = 1f
                )
            }

            // 绘制折线
            if (dailyData.size > 1) {
                val path = Path()
                val pointSpacing = (width - padding * 2) / (dailyData.size - 1)

                dailyData.forEachIndexed { index, data ->
                    val x = padding + index * pointSpacing
                    val normalizedValue = (data.count / maxValue) * animationProgress
                    val y = height - padding - (normalizedValue * drawableHeight)

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                // 绘制线条
                drawPath(
                    path = path,
                    color = trendColor,
                    style = Stroke(width = 4f)
                )

                // 绘制数据点
                dailyData.forEachIndexed { index, data ->
                    val x = padding + index * pointSpacing
                    val normalizedValue = (data.count / maxValue) * animationProgress
                    val y = height - padding - (normalizedValue * drawableHeight)

                    drawCircle(
                        color = trendColor,
                        radius = 6f,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = pointCenterColor,
                        radius = 3f,
                        center = Offset(x, y)
                    )
                }
            }
        }

        // 图例
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (dailyData.isNotEmpty()) {
                Text(
                    text = dailyData.first().date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = dailyData.last().date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 情感分布饼图
 */
@Composable
fun EmotionPieChart(
    emotionData: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val chartColors = LocalVisualTheme.current.tokens.chart
    val emotionColors = remember(emotionData.keys, chartColors) {
        emotionData.keys.associateWith { emotion -> emotionColor(emotion, chartColors) }
    }
    val chartMotionEnabled = chartAnimationEnabled(LocalVisualTheme.current.effectTier)
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = if (chartMotionEnabled) 1000 else 0,
            easing = FastOutSlowInEasing
        ),
        label = "pie_animation"
    )

    Column(modifier = modifier) {
        // 标题
        Text(
            text = "情感分布",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (emotionData.isEmpty()) {
            Text(
                text = "暂无数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            // 饼图
            Canvas(
                modifier = Modifier
                    .size(120.dp)
                    .padding(8.dp)
            ) {
                val total = emotionData.values.sumOf { it.coerceAtLeast(0) }
                    .toFloat()
                    .coerceAtLeast(1f)
                var startAngle = -90f

                emotionData.entries
                    .filter { it.value > 0 }
                    .forEach { (emotion, count) ->
                    val sweepAngle = (count.toFloat() / total) * 360f * animationProgress
                    drawArc(
                        color = emotionColors.getValue(emotion),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        size = Size(size.width, size.height)
                    )
                    startAngle += sweepAngle
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 图例
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                emotionData.entries.forEach { (emotion, count) ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Canvas(modifier = Modifier.size(16.dp)) {
                            drawCircle(color = emotionColors.getValue(emotion))
                        }
                        Text(
                            text = "$emotion: $count",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

private fun emotionColor(emotion: String, colors: VisualChartColors): Color {
    val normalized = emotion.trim().lowercase()
    return when {
        normalized.contains("爱") || normalized.contains("喜欢") || normalized.contains("affection") -> colors.affection
        normalized.contains("信任") || normalized.contains("trust") -> colors.trust
        normalized.contains("兴趣") || normalized.contains("interest") -> colors.interest
        normalized.contains("开心") || normalized.contains("积极") || normalized.contains("happy") || normalized.contains("positive") -> colors.positive
        normalized.contains("难过") || normalized.contains("消极") || normalized.contains("悲") || normalized.contains("sad") || normalized.contains("negative") -> colors.negative
        else -> colors.neutral
    }
}

/**
 * 每日消息数据
 */
data class DailyMessageData(
    val date: String,
    val count: Int
)
