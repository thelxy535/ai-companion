package com.companion.cc.ui.memory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.data.local.entity.MemoryNodeEntity
import com.companion.cc.ui.theme.GlassSurface

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
    Scaffold(topBar = {
        GlassSurface(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp), useStrongFill = true) {
            TopAppBar(
                title = { Text("记忆库", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "返回") } }
            )
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                placeholder = { Text("搜索正式记忆") },
                leadingIcon = { Icon(Icons.Default.Search, "搜索") },
                singleLine = true
            )
            LazyColumn(contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(nodes, key = { it.id }) { node -> MemoryNodeRow(node, onClick = { onOpenDetail(node.id) }) }
            }
        }
    }
}

@Composable
private fun MemoryNodeRow(node: MemoryNodeEntity, onClick: () -> Unit) {
    GlassSurface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), contentPadding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(node.title, fontWeight = FontWeight.SemiBold)
            Text(node.content)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(node.kind)
                Text("可信度 ${(node.confidence * 100).toInt()}%")
            }
        }
    }
}
