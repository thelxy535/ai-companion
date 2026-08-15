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
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
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
            val maxValue = dailyData.maxOfOrNull { it.count }?.toFloat() ?: 1f
            val drawableHeight = height - padding * 2

            // 绘制网格线
            val gridColor = Color.LightGray.copy(alpha = 0.3f)
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
                    color = Color(0xFF2196F3),
                    style = Stroke(width = 4f)
                )

                // 绘制数据点
                dailyData.forEachIndexed { index, data ->
                    val x = padding + index * pointSpacing
                    val normalizedValue = (data.count / maxValue) * animationProgress
                    val y = height - padding - (normalizedValue * drawableHeight)

                    drawCircle(
                        color = Color(0xFF2196F3),
                        radius = 6f,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = Color.White,
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
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
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
                val total = emotionData.values.sum().toFloat()
                var startAngle = -90f

                val colors = listOf(
                    Color(0xFFFF6B6B), // 红色
                    Color(0xFF4ECDC4), // 青色
                    Color(0xFFFFE66D), // 黄色
                    Color(0xFF95E1D3), // 绿色
                    Color(0xFFC7CEEA)  // 紫色
                )

                emotionData.entries.forEachIndexed { index, (_, count) ->
                    val sweepAngle = (count / total) * 360f * animationProgress
                    drawArc(
                        color = colors[index % colors.size],
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
                val colors = listOf(
                    Color(0xFFFF6B6B),
                    Color(0xFF4ECDC4),
                    Color(0xFFFFE66D),
                    Color(0xFF95E1D3),
                    Color(0xFFC7CEEA)
                )

                emotionData.entries.forEachIndexed { index, (emotion, count) ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Canvas(modifier = Modifier.size(16.dp)) {
                            drawCircle(color = colors[index % colors.size])
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

/**
 * 每日消息数据
 */
data class DailyMessageData(
    val date: String,
    val count: Int
)
