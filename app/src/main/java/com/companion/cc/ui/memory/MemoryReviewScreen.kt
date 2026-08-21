package com.companion.cc.ui.memory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.companion.cc.ui.theme.GlassSurface

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
        topBar = {
            GlassSurface(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp), useStrongFill = true) {
                TopAppBar(
                    title = { Text("记忆审核", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "返回") } }
                )
            }
        }
    ) { padding ->
        when (val current = state) {
            MemoryReviewState.Loading -> Text("正在加载…", modifier = Modifier.padding(padding).padding(24.dp))
            is MemoryReviewState.Error -> ReviewList(current.previous, padding, viewModel)
            is MemoryReviewState.Content -> ReviewList(current.reviews, padding, viewModel)
        }
    }
}

@Composable
private fun ReviewList(
    reviews: List<com.companion.cc.data.local.entity.MemoryReviewEntity>,
    padding: PaddingValues,
    viewModel: MemoryReviewViewModel
) {
    if (reviews.isEmpty()) {
        Text("没有待审核记忆", modifier = Modifier.padding(padding).padding(24.dp), style = MaterialTheme.typography.bodyLarge)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(reviews, key = { it.id }) { review ->
            Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(review.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(review.content, style = MaterialTheme.typography.bodyMedium)
                    Text("${review.kind} · 可信度 ${(review.confidence * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = { viewModel.defer(review) }) { Icon(Icons.Default.Schedule, "稍后审核") }
                        IconButton(onClick = { viewModel.reject(review) }) { Icon(Icons.Default.Close, "拒绝") }
                        IconButton(onClick = { viewModel.accept(review) }) { Icon(Icons.Default.Check, "接受") }
                    }
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}
