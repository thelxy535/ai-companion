package com.companion.cc.ui.memory

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.companion.cc.domain.memory.GraphSnapshot

internal enum class MemoryTreeMode(val label: String) {
    GRAPH("脑图"), TIMELINE("时间线"), RECORDS("原始记录")
}

@Composable
internal fun MemoryGraphView(graph: GraphSnapshot?, modifier: Modifier = Modifier) {
    if (graph == null || graph.nodes.isEmpty()) {
        EmptyMemoryGraph(modifier)
        return
    }

    val nodes = graph.nodes
    val edgeColor = MaterialTheme.colorScheme.outlineVariant
    val edgeWidth = with(androidx.compose.ui.platform.LocalDensity.current) { 2.dp.toPx() }
    val scrollState = rememberScrollState()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = remember(nodes.size, maxWidth) {
            MemoryGraphLayout.columnsFor(nodes.size, maxWidth.value)
        }
        val rows = remember(nodes.size, columns) { MemoryGraphLayout.rowCount(nodes.size, columns) }
        val cardGap = 12.dp
        val horizontalPadding = 16.dp
        val verticalPadding = 18.dp
        val cardHeight = 84.dp
        val rowGap = 16.dp
        val cardWidth = graphCardWidth(maxWidth, columns, horizontalPadding, cardGap)
        val graphContentWidth = cardWidth * columns + cardGap * (columns - 1)
        val horizontalInset = ((maxWidth - graphContentWidth) / 2).coerceAtLeast(horizontalPadding)
        val requiredHeight = verticalPadding * 2 + cardHeight * rows + rowGap * (rows - 1)
        val contentHeight = maxOf(maxHeight, requiredHeight)

        Box(Modifier.fillMaxSize().verticalScroll(scrollState)) {
            Box(Modifier.fillMaxWidth().height(contentHeight)) {
                fun nodeTopLeft(index: Int): Pair<Dp, Dp> {
                    val position = MemoryGraphLayout.positionFor(index, nodes.size, columns)
                    return Pair(
                        horizontalInset + (cardWidth + cardGap) * position.column,
                        verticalPadding + (cardHeight + rowGap) * position.row,
                    )
                }

                Canvas(Modifier.fillMaxSize()) {
                    graph.edges.forEach { edge ->
                        val from = nodes.indexOfFirst { it.id == edge.fromNodeId }
                        val to = nodes.indexOfFirst { it.id == edge.toNodeId }
                        if (from >= 0 && to >= 0) {
                            val (fromX, fromY) = nodeTopLeft(from)
                            val (toX, toY) = nodeTopLeft(to)
                            drawLine(
                                color = edgeColor,
                                start = Offset((fromX + cardWidth / 2).toPx(), (fromY + cardHeight / 2).toPx()),
                                end = Offset((toX + cardWidth / 2).toPx(), (toY + cardHeight / 2).toPx()),
                                strokeWidth = edgeWidth,
                            )
                        }
                    }
                }

                nodes.forEachIndexed { index, node ->
                    val (x, y) = nodeTopLeft(index)
                    MemoryGraphNodeCard(
                        title = node.title,
                        subtitle = "${node.kind} · ${node.confidence.formatPercent()}",
                        width = cardWidth,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(
                                x = x,
                                y = y,
                            ),
                    )
                }
            }
        }
    }
}

private fun graphCardWidth(
    maxWidth: Dp,
    columns: Int,
    horizontalPadding: Dp,
    cardGap: Dp,
): Dp = ((maxWidth - horizontalPadding * 2 - cardGap * (columns - 1)) / columns)
    .coerceAtMost(176.dp)

@Composable
internal fun MemoryTimelineView(graph: GraphSnapshot?, modifier: Modifier = Modifier) {
    if (graph == null || graph.nodes.isEmpty()) {
        EmptyMemoryGraph(modifier)
        return
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(graph.nodes, key = { it.id }) { node ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary).padding(5.dp))
                Column {
                    Text(node.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${node.kind} · ${node.confidence.formatPercent()} · ${node.content}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyMemoryGraph(modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.Icon(Icons.Default.Hub, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("还没有形成长期记忆", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("聊天中的重要内容会在后台整理到这里", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MemoryGraphNodeCard(title: String, subtitle: String, width: Dp, modifier: Modifier) {
    Column(
        modifier = modifier
            .width(width)
            .height(84.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = title.ifBlank { "未命名记忆" },
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            softWrap = true,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun Double.formatPercent(): String = "${(this * 100).toInt().coerceIn(0, 100)}%"
