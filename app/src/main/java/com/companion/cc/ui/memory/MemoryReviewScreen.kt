package com.companion.cc.ui.memory

import com.companion.cc.ui.designsystem.auroraScreenBackground
import com.companion.cc.ui.components.V9PMActionButton
import com.companion.cc.ui.components.V9PMTopBar
import com.companion.cc.ui.theme.LocalVisualTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.ui.theme.TactileGesture
import com.companion.cc.ui.theme.rememberTactileAction
import com.companion.cc.domain.memory.ReviewInboxPolicy

/** 候选类型的中文标签（V9PM）。 */
private fun kindLabel(kind: String): String = when (kind) {
    "fact" -> "事实"
    "event" -> "事件"
    "emotion" -> "情绪"
    "preference" -> "偏好"
    "relationship_narrative" -> "关系叙事"
    "observation" -> "观察"
    "commitment" -> "承诺"
    else -> kind
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryReviewScreen(
    companionId: String,
    onNavigateBack: () -> Unit,
    viewModel: MemoryReviewViewModel = hiltViewModel()
) {
    LaunchedEffect(companionId) { viewModel.setCompanion(companionId) }
    val state by viewModel.state.collectAsState()

    Scaffold(
        modifier = Modifier.auroraScreenBackground(LocalVisualTheme.current.tokens.backdrop.isDark),
        topBar = {
            V9PMTopBar(
                title = { Text("记忆收件箱", fontWeight = FontWeight.SemiBold) },
                onNavigateBack = onNavigateBack,
                modifier = Modifier.padding(top = 44.dp)
            )
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { padding ->
        when (val current = state) {
            MemoryReviewState.Loading -> Text(
                "正在加载…",
                modifier = Modifier
                    .padding(padding)
                    .navigationBarsPadding()
                    .padding(24.dp)
            )
            is MemoryReviewState.Error -> ReviewList(
                reviews = current.previous,
                deferred = emptyList(),
                padding = padding,
                viewModel = viewModel,
                errorMessage = current.message
            )
            is MemoryReviewState.Content -> ReviewList(
                reviews = current.reviews,
                deferred = current.deferred,
                padding = padding,
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun ReviewList(
    reviews: List<com.companion.cc.data.local.entity.MemoryReviewEntity>,
    deferred: List<com.companion.cc.data.local.entity.MemoryReviewEntity>,
    padding: PaddingValues,
    viewModel: MemoryReviewViewModel,
    errorMessage: String? = null
) {
    val groups = ReviewInboxPolicy.group(reviews)
    val regularGroups = groups.filter { group -> group.items.all(ReviewInboxPolicy::isBatchable) }
    val deliberateGroups = groups.filterNot { group -> group.items.all(ReviewInboxPolicy::isBatchable) }
    if (reviews.isEmpty() && deferred.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (errorMessage == null) {
                    Icons.Default.CheckCircle
                } else {
                    Icons.Default.ErrorOutline
                },
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (errorMessage == null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
            Text(
                text = if (errorMessage == null) "记忆收件箱是空的" else "暂时无法加载",
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = errorMessage ?: "之后有值得留下的片段，会在这里安静出现",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (errorMessage == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (errorMessage != null) {
            item {
                Text(
                    text = "操作未完成：$errorMessage",
                    modifier = Modifier.padding(vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }

        if (regularGroups.isNotEmpty()) {
            item {
                Text(
                    text = "普通片段 · ${regularGroups.size} 组",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    V9PMActionButton(
                        label = "批量保留普通片段",
                        icon = Icons.Default.DoneAll,
                        onClick = rememberTactileAction { viewModel.acceptAll(regularGroups) },
                        modifier = Modifier.weight(1f),
                        height = 40.dp
                    )
                    V9PMActionButton(
                        label = "批量稍后",
                        icon = Icons.Default.Schedule,
                        onClick = rememberTactileAction(gesture = TactileGesture.CANCEL) {
                            viewModel.deferAll(regularGroups)
                        },
                        modifier = Modifier.weight(1f),
                        height = 40.dp
                    )
                    V9PMActionButton(
                        label = "批量不保存",
                        icon = Icons.Default.Close,
                        onClick = rememberTactileAction(gesture = TactileGesture.DESTRUCTIVE_CONFIRM) {
                            viewModel.rejectAll(regularGroups)
                        },
                        modifier = Modifier.weight(1f),
                        height = 40.dp,
                        destructive = true
                    )
                }
                Text(
                    text = "普通片段可以批量处理；承诺、关系变化和敏感内容请逐组决定。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
        }

        items(regularGroups, key = { it.key }) { group ->
            val review = group.representative
            var showEvidence by remember(group.key) { mutableStateOf(false) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = review.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(review.content, style = MaterialTheme.typography.bodyMedium)
                if (review.status == "conflict" && review.resolutionNote.isNotBlank()) {
                    Text(
                        review.resolutionNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    text = buildString {
                        if (review.status == "conflict") append("需要确认 · ")
                        append(kindLabel(review.kind))
                        if (group.items.size > 1) append(" · ${group.items.size} 条相近片段")
                        append(" · 平均可信度 ${(group.confidence * 100).toInt()}%")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (showEvidence) "收起依据" else "查看依据${if (group.items.size > 1) "（${group.items.size} 条）" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { showEvidence = !showEvidence }
                )
                if (showEvidence) {
                    group.items.sortedByDescending { it.createdAt }.forEach { item ->
                        Text(
                            text = "${item.title} · 可信度 ${(item.confidence * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    V9PMActionButton(
                        label = "稍后",
                        icon = Icons.Default.Schedule,
                        onClick = rememberTactileAction(gesture = TactileGesture.CANCEL) {
                        viewModel.deferGroup(group)
                        },
                        modifier = Modifier.weight(1f),
                        height = 40.dp
                    )
                    V9PMActionButton(
                        label = "拒绝",
                        icon = Icons.Default.Close,
                        onClick = rememberTactileAction(gesture = TactileGesture.DESTRUCTIVE_CONFIRM) {
                        viewModel.rejectGroup(group)
                        },
                        modifier = Modifier.weight(1f),
                        height = 40.dp,
                        destructive = true
                    )
                    V9PMActionButton(
                        label = "接受",
                        icon = Icons.Default.Check,
                        onClick = rememberTactileAction { viewModel.acceptGroup(group) },
                        modifier = Modifier.weight(1f),
                        height = 40.dp
                    )
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }

        if (deliberateGroups.isNotEmpty()) {
            item {
                Text(
                    text = "需要你确认 · ${deliberateGroups.size} 组",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
                Text(
                    text = "承诺、关系变化和敏感内容不会混入批量操作。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }

        items(deliberateGroups, key = { "deliberate-${it.key}" }) { group ->
            val review = group.representative
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(review.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(review.content, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${if (review.status == "conflict") "发现可能冲突 · " else ""}${kindLabel(review.kind)} · 平均可信度 ${(group.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    V9PMActionButton(
                        label = "稍后", icon = Icons.Default.Schedule,
                        onClick = rememberTactileAction(gesture = TactileGesture.CANCEL) { viewModel.deferGroup(group) },
                        modifier = Modifier.weight(1f), height = 40.dp
                    )
                    V9PMActionButton(
                        label = "拒绝", icon = Icons.Default.Close,
                        onClick = rememberTactileAction(gesture = TactileGesture.DESTRUCTIVE_CONFIRM) { viewModel.rejectGroup(group) },
                        modifier = Modifier.weight(1f), height = 40.dp, destructive = true
                    )
                    V9PMActionButton(
                        label = "接受", icon = Icons.Default.Check,
                        onClick = rememberTactileAction { viewModel.acceptGroup(group) },
                        modifier = Modifier.weight(1f), height = 40.dp
                    )
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }

        if (deferred.isNotEmpty()) {
            item {
                Text(
                    text = "已延后 · ${deferred.size}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }
            items(deferred, key = { "deferred-${it.id}" }) { review ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = review.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        review.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    V9PMActionButton(
                        label = "恢复待审核",
                        icon = Icons.Default.Refresh,
                        onClick = rememberTactileAction { viewModel.restore(review) },
                        modifier = Modifier.fillMaxWidth(),
                        height = 40.dp
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}
