package com.companion.cc.ui.memory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.ui.components.UtilityDivider
import com.companion.cc.ui.components.UtilitySection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryRetrievalTraceScreen(
    companionId: String,
    traceId: String,
    onNavigateBack: () -> Unit,
    viewModel: MemoryRetrievalTraceViewModel = hiltViewModel()
) {
    LaunchedEffect(companionId, traceId) { viewModel.load(companionId, traceId) }
    val state = viewModel.state
    Scaffold(topBar = {
        TopAppBar(
            modifier = Modifier.padding(top = 44.dp),
            title = { Text("记忆召回记录", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, "返回")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                UtilitySection(title = "本次召回") {
                    if (state.traceId == null) {
                        Text("召回记录不存在或不属于当前角色")
                    } else {
                        Text("查询：${state.query}")
                        Text("耗时：${state.durationMs} ms")
                    }
                }
            }
            if (state.traceId != null) {
                item {
                    UtilitySection(title = "入选记忆") {
                        if (state.selections.isEmpty()) {
                            Text("本次没有使用长期记忆")
                        }
                    }
                }
                items(state.selections, key = { it.nodeId }) { selection ->
                    UtilitySection(title = selection.nodeId) {
                        if (selection.explanation.isNotBlank()) {
                            Text(selection.explanation)
                            UtilityDivider()
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                viewModel.submitFeedback(selection.nodeId, "positive")
                            }) { Text("有帮助") }
                            OutlinedButton(onClick = {
                                viewModel.submitFeedback(selection.nodeId, "negative")
                            }) { Text("无帮助") }
                        }
                    }
                }
            }
            if (viewModel.actionError != null) {
                item {
                    Text(
                        viewModel.actionError.orEmpty(),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
