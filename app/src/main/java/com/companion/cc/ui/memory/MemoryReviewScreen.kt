package com.companion.cc.ui.memory

import com.companion.cc.ui.designsystem.auroraScreenBackground
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.companion.cc.ui.theme.TactileGesture
import com.companion.cc.ui.theme.rememberTactileAction

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
            TopAppBar(
                modifier = Modifier.padding(top = 44.dp),
                title = { Text("记忆审核", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
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
                padding = padding,
                viewModel = viewModel,
                errorMessage = current.message
            )
            is MemoryReviewState.Content -> ReviewList(
                reviews = current.reviews,
                padding = padding,
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun ReviewList(
    reviews: List<com.companion.cc.data.local.entity.MemoryReviewEntity>,
    padding: PaddingValues,
    viewModel: MemoryReviewViewModel,
    errorMessage: String? = null
) {
    if (reviews.isEmpty()) {
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
                text = if (errorMessage == null) "审核已完成" else "暂时无法加载",
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = errorMessage ?: "目前没有等待处理的记忆",
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

        items(reviews, key = { it.id }) { review ->
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
                Text(
                    text = "${review.kind} · 可信度 ${(review.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = rememberTactileAction(gesture = TactileGesture.CANCEL) {
                            viewModel.defer(review)
                        }
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                        Text("稍后")
                    }
                    TextButton(
                        onClick = rememberTactileAction(gesture = TactileGesture.DESTRUCTIVE_CONFIRM) {
                            viewModel.reject(review)
                        }
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text("拒绝", color = MaterialTheme.colorScheme.error)
                    }
                    Button(onClick = rememberTactileAction { viewModel.accept(review) }) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Text("接受")
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
