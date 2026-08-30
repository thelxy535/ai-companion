package com.companion.cc.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class MetricGridItem(
    val label: String,
    val value: String,
    val icon: ImageVector
)

/** Responsive metric band that emphasizes values without creating tile-shaped cards. */
@Composable
fun AdaptiveMetricGrid(
    items: List<MetricGridItem>,
    modifier: Modifier = Modifier,
    minCellWidth: Dp = 148.dp
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= minCellWidth * 2) 2 else 1
        Column {
            items.chunked(columns).forEachIndexed { index, rowItems ->
                if (index > 0) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowItems.forEachIndexed { itemIndex, item ->
                        if (itemIndex > 0) {
                            Divider(
                                modifier = Modifier
                                    .padding(vertical = 12.dp)
                                    .weight(0.002f),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                            )
                        }
                        MetricBandCell(item = item, modifier = Modifier.weight(1f))
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBandCell(item: MetricGridItem, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = "${item.label}，${item.value}"
            }
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 2.dp)
            )
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = item.value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
