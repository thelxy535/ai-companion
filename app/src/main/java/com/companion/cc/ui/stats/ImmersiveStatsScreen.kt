package com.companion.cc.ui.stats

import com.companion.cc.ui.designsystem.auroraScreenBackground
import com.companion.cc.ui.theme.LocalVisualTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.ui.components.AdaptiveMetricGrid
import com.companion.cc.ui.components.MetricGridItem
import com.companion.cc.ui.components.SceneSection
import com.companion.cc.ui.components.SceneTopBar
import com.companion.cc.ui.components.SceneTopBarAction
import com.companion.cc.ui.theme.LocalVisualTheme

@Composable
fun ImmersiveStatsScreen(
    companionId: String,
    onNavigateBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val totalMemoryCount by viewModel.totalMemoryCount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val usage by viewModel.usage.collectAsState()

    LaunchedEffect(companionId) {
        viewModel.loadStats(companionId)
    }

    Scaffold(
        modifier = Modifier.auroraScreenBackground(LocalVisualTheme.current.tokens.backdrop.isDark),
        topBar = {
            SceneTopBar(
                title = { Text("${getCompanionName(companionId)}的统计", fontWeight = FontWeight.Bold) },
                onNavigateBack = onNavigateBack,
                actions = listOf(
                    SceneTopBarAction(
                        icon = Icons.Default.Refresh,
                        contentDescription = "刷新",
                        onClick = { viewModel.refreshStats(companionId) }
                    )
                )
            )
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = LocalVisualTheme.current.tokens.accent) }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                item {
                    SceneSection(title = "概览", icon = Icons.Default.Insights) {
                        AdaptiveMetricGrid(listOf(
                            MetricGridItem("总消息数", stats.totalMessages.toString(), Icons.Default.Insights),
                            MetricGridItem("对话轮次", stats.conversationRounds.toString(), Icons.Default.Mood)
                        ))
                    }
                }
                item {
                    SceneSection(title = "模型用量", icon = Icons.Default.Insights) {
                        AdaptiveMetricGrid(listOf(
                            MetricGridItem("输入 Token", usage.inputTokens.toString(), Icons.Default.Insights),
                            MetricGridItem("输出 Token", usage.outputTokens.toString(), Icons.Default.Insights),
                            MetricGridItem("估算费用", String.format("%.6f", usage.estimatedCost), Icons.Default.Star)
                        ))
                    }
                }
                item {
                    SceneSection(title = "记忆", icon = Icons.Default.Psychology) {
                        AdaptiveMetricGrid(listOf(
                            MetricGridItem("活动记忆", totalMemoryCount.toString(), Icons.Default.Psychology),
                            MetricGridItem("向量记忆", stats.vectorMemoryCount.toString(), Icons.Default.Psychology)
                        ))
                    }
                }
                item {
                    SceneSection(title = "话题", icon = Icons.Default.Topic) {
                        if (stats.currentTopics.isEmpty()) {
                            Text("尚未形成可统计的话题", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            stats.currentTopics.forEach { topic ->
                                Text("•  $topic", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 5.dp))
                            }
                        }
                    }
                }
                item {
                    SceneSection(title = "情绪", icon = Icons.Default.Mood) {
                        Text(String.format("%.1f", stats.emotionalScore), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            when {
                                stats.emotionalScore > 0.5f -> "整体情绪积极"
                                stats.emotionalScore < -0.5f -> "整体情绪消极"
                                else -> "情绪中性"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item {
                    SceneSection(title = "主要特质", icon = Icons.Default.Star) {
                        if (stats.topTraits.isEmpty()) {
                            Text("尚无足够对话生成特质摘要", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            stats.topTraits.forEach { trait ->
                                Text("•  $trait", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 5.dp))
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

private fun getCompanionName(companionId: String): String = when (companionId) {
    "muse" -> "缪斯"
    "xiaocan" -> "小璨"
    else -> "伴侣"
}
