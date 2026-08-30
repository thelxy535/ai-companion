package com.companion.cc.ui.memory

import com.companion.cc.ui.designsystem.auroraScreenBackground
import com.companion.cc.ui.theme.LocalVisualTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.data.local.entity.MemoryNodeEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryLibraryScreen(
    companionId: String,
    onNavigateBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    viewModel: MemoryLibraryViewModel = hiltViewModel()
) {
    LaunchedEffect(companionId) { viewModel.setCompanion(companionId) }
    val nodes by viewModel.nodes.collectAsState()
    val query by viewModel.queryText.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    Scaffold(
        modifier = Modifier.auroraScreenBackground(LocalVisualTheme.current.tokens.backdrop.isDark),
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 44.dp),
                title = { Text("记忆库", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                placeholder = { Text("搜索正式记忆") },
                leadingIcon = { Icon(Icons.Default.Search, "搜索") },
                singleLine = true
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                listOf("正常" to "active", "已禁止召回" to "do_not_recall", "已删除" to "deleted").forEach { (label, status) ->
                    MemoryStatusFilterRow(label, status, selectedStatus, viewModel::setStatus)
                }
            }
            LazyColumn(contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(nodes, key = { it.id }) { node -> MemoryNodeRow(node, onClick = { onOpenDetail(node.id) }) }
            }
        }
    }
}

@Composable
private fun MemoryStatusFilterRow(
    label: String,
    status: String,
    selectedStatus: String,
    onStatusSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onStatusSelected(memoryFilterValue(status)) }
            .padding(vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selectedStatus == status,
            onClick = { onStatusSelected(memoryFilterValue(status)) }
        )
        Text(label)
    }
}

@Composable
private fun MemoryNodeRow(node: MemoryNodeEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(node.title, fontWeight = FontWeight.SemiBold)
        Text(node.content)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(node.kind, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("可信度 ${(node.confidence * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Divider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
