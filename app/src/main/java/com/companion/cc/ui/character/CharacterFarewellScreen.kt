package com.companion.cc.ui.character

import com.companion.cc.ui.designsystem.auroraScreenBackground
import com.companion.cc.ui.components.V9PMActionButton
import com.companion.cc.ui.components.V9PMDialogSurface
import com.companion.cc.ui.components.V9PMActionButton
import com.companion.cc.ui.components.V9PMTopBar
import com.companion.cc.ui.theme.LocalVisualTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.domain.character.CharacterDeletionResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterFarewellScreen(
    characterId: String,
    onNavigateBack: () -> Unit,
    onOpenCapsuleVault: () -> Unit,
    viewModel: CharacterFarewellViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(characterId) {
        viewModel.load(characterId)
    }

    Scaffold(
        modifier = Modifier.auroraScreenBackground(LocalVisualTheme.current.tokens.backdrop.isDark),
        topBar = {
            V9PMTopBar(
                title = { Text("告别角色") },
                onNavigateBack = onNavigateBack,
                modifier = Modifier.padding(top = 44.dp)
            )
        },
        containerColor = Color.Transparent,
        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
    ) { padding ->
        when (val current = state) {
            CharacterFarewellState.Idle,
            CharacterFarewellState.Loading -> LoadingContent(padding)
            is CharacterFarewellState.Ready -> ReadyContent(
                padding = padding,
                characterName = current.character.name,
                onConfirm = { showConfirmDialog = true },
                onCancel = onNavigateBack
            )
            CharacterFarewellState.Deleting -> LoadingContent(padding, "正在保存记忆胶囊并删除角色…")
            is CharacterFarewellState.Deleted -> DeletedContent(
                padding = padding,
                result = current.result,
                onDone = onNavigateBack,
                onOpenCapsuleVault = onOpenCapsuleVault
            )
            is CharacterFarewellState.Failure -> FailureContent(
                padding = padding,
                message = current.message,
                onNavigateBack = onNavigateBack
            )
        }
    }

    if (showConfirmDialog) {
        V9PMDialogSurface(onDismissRequest = { showConfirmDialog = false }) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("确认告别", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("将先保存角色记忆胶囊，再删除角色及其运行数据。请确认你已准备好保管恢复令牌。")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    V9PMActionButton(label = "取消", onClick = { showConfirmDialog = false }, modifier = Modifier.weight(1f), height = 40.dp)
                    V9PMActionButton(label = "确认删除", onClick = {
                        showConfirmDialog = false
                        viewModel.confirmDeletion()
                    }, modifier = Modifier.weight(1f), height = 40.dp, destructive = true)
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(padding: PaddingValues, message: String = "正在加载…") {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.padding(8.dp))
        Text(message)
    }
}

@Composable
private fun ReadyContent(
    padding: PaddingValues,
    characterName: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("准备告别 $characterName？", style = MaterialTheme.typography.headlineSmall)
        Text(
            "删除前会生成加密记忆胶囊，并清理该角色的消息、记忆和运行时数据。删除完成后只能通过恢复令牌尝试恢复。",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        V9PMActionButton(
            label = "保存胶囊并删除",
            icon = Icons.Default.DeleteForever,
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            destructive = true
        )
        V9PMActionButton(label = "暂不删除", onClick = onCancel, modifier = Modifier.fillMaxWidth(), height = 40.dp)
    }
}

@Composable
private fun DeletedContent(
    padding: PaddingValues,
    result: CharacterDeletionResult,
    onDone: () -> Unit,
    onOpenCapsuleVault: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("角色已删除", style = MaterialTheme.typography.headlineSmall)
        Text("请立即保存以下恢复令牌。当前版本不会再次显示它。")
        Text(
            result.revivalToken,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text("胶囊 ID：${result.capsuleId}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        V9PMActionButton(label = "打开记忆胶囊库", onClick = onOpenCapsuleVault, modifier = Modifier.fillMaxWidth(), height = 44.dp)
        V9PMActionButton(label = "返回角色列表", onClick = onDone, modifier = Modifier.fillMaxWidth(), height = 44.dp)
    }
}

@Composable
private fun FailureContent(
    padding: PaddingValues,
    message: String,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("操作失败", style = MaterialTheme.typography.headlineSmall)
        Text(message, color = MaterialTheme.colorScheme.error)
        V9PMActionButton(label = "返回", onClick = onNavigateBack, modifier = Modifier.fillMaxWidth(), height = 44.dp)
    }
}
