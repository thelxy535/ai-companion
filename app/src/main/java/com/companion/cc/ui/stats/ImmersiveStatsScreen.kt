package com.companion.cc.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.ui.components.UtilityDivider
import com.companion.cc.ui.components.UtilitySection
import com.companion.cc.ui.theme.GlassSurface
import com.companion.cc.ui.theme.LocalVisualTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmersiveStatsScreen(
    companionId: String,
    onNavigateBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val totalMemoryCount by viewModel.totalMemoryCount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(companionId) {
        viewModel.loadStats(companionId)
    }

    val accentColor = LocalVisualTheme.current.tokens.accent

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp),
            useStrongFill = true
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = "${getCompanionName(companionId)}的统计",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshStats(companionId) }) {
                        Icon(Icons.Default.Refresh, "刷新")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    UtilitySection(title = "概览", icon = Icons.Default.Insights) {
                        MetricRow("总消息数", stats.totalMessages.toString())
                        UtilityDivider()
                        MetricRow("对话轮次", stats.conversationRounds.toString())
                    }
                }
                item {
                    UtilitySection(title = "记忆", icon = Icons.Default.Psychology) {
                        MetricRow("活动记忆", totalMemoryCount.toString())
                        UtilityDivider()
                        MetricRow("向量记忆", stats.vectorMemoryCount.toString())
                    }
                }
                item {
                    UtilitySection(title = "话题", icon = Icons.Default.Topic) {
                        if (stats.currentTopics.isEmpty()) {
                            Text("暂无话题", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        stats.currentTopics.forEachIndexed { index, topic ->
                            if (index > 0) UtilityDivider()
                            Text(topic, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                item {
                    UtilitySection(title = "情绪", icon = Icons.Default.Mood) {
                        MetricRow("评分", String.format("%.1f", stats.emotionalScore))
                        Text(
                            text = when {
                                stats.emotionalScore > 0.5f -> "整体情绪积极"
                                stats.emotionalScore < -0.5f -> "整体情绪消极"
                                else -> "情绪中性"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (stats.topTraits.isNotEmpty()) {
                    item {
                        UtilitySection(title = "主要特质", icon = Icons.Default.Star) {
                            stats.topTraits.forEachIndexed { index, trait ->
                                if (index > 0) UtilityDivider()
                                Text(trait, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

private fun getCompanionName(companionId: String): String {
    return when (companionId) {
        "muse" -> "缪斯"
        "xiaocan" -> "小璨"
        else -> "伴侣"
    }
}
