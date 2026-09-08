package com.companion.cc.ui.character

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryCapsuleVaultScreen(
    onNavigateBack: () -> Unit,
    onOpenRevival: () -> Unit,
    viewModel: MemoryCapsuleVaultViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Scaffold(
        modifier = Modifier.auroraScreenBackground(LocalVisualTheme.current.tokens.backdrop.isDark),
        topBar = {
            V9PMTopBar(
                title = { Text("记忆胶囊库") },
                onNavigateBack = onNavigateBack,
                modifier = Modifier.padding(top = 40.dp)
            )
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { padding ->
        when (val current = state) {
            MemoryCapsuleVaultState.Idle,
            MemoryCapsuleVaultState.Loading ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text("正在加载胶囊…")
                }
            is MemoryCapsuleVaultState.Ready -> {
                if (current.capsules.isEmpty()) {
                    EmptyVault(padding)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(current.capsules, key = { it.id }) { capsule ->
                            CapsuleRow(capsule, onOpenRevival)
                        }
                    }
                }
            }
            is MemoryCapsuleVaultState.Failure ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("胶囊库加载失败", style = MaterialTheme.typography.titleLarge)
                    Text(current.message, color = MaterialTheme.colorScheme.error)
                    V9PMActionButton(label = "重试", onClick = viewModel::load, modifier = Modifier.fillMaxWidth())
                }
        }
    }
}

@Composable
private fun EmptyVault(padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.Inventory2, contentDescription = null)
        Text("还没有可用的记忆胶囊", style = MaterialTheme.typography.titleLarge)
        Text(
            "删除角色并保存胶囊后，胶囊会出现在这里。",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CapsuleRow(capsule: CapsuleSummary, onOpenRevival: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "胶囊 ${capsule.id.take(8)}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = capsule.status,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Text(
                    text = "原角色：${capsule.sourceCharacterId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            V9PMActionButton(label = "恢复", onClick = onOpenRevival, modifier = Modifier.widthIn(min = 96.dp), height = 40.dp)
        }
        Divider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
