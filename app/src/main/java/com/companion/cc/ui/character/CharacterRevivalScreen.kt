package com.companion.cc.ui.character

import com.companion.cc.ui.designsystem.auroraScreenBackground
import com.companion.cc.ui.components.V9PMActionButton
import com.companion.cc.ui.components.V9PMChoiceRow
import com.companion.cc.ui.components.V9PMTextField
import com.companion.cc.ui.components.V9PMTopBar
import com.companion.cc.ui.theme.LocalVisualTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.domain.character.CharacterRevivalMode
import com.companion.cc.ui.theme.rememberTactileAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterRevivalScreen(
    onNavigateBack: () -> Unit,
    viewModel: CharacterRevivalViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var token by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(CharacterRevivalMode.REINTRODUCTION) }
    val isReviving = state is CharacterRevivalState.Reviving
    val startRevival = rememberTactileAction(
        enabled = token.isNotBlank() && !isReviving
    ) {
        viewModel.revive(token, mode)
    }

    Scaffold(
        modifier = Modifier.auroraScreenBackground(LocalVisualTheme.current.tokens.backdrop.isDark),
        topBar = {
            V9PMTopBar(
                title = { Text("恢复角色") },
                onNavigateBack = onNavigateBack,
                modifier = Modifier.padding(top = 44.dp)
            )
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("使用恢复令牌恢复角色", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "输入删除角色时生成的令牌，并选择恢复方式。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            V9PMTextField(
                value = token,
                onValueChange = { token = it },
                modifier = Modifier.fillMaxWidth(),
                label = "恢复令牌",
                supportingText = "令牌会在提交前自动去除首尾空格",
                enabled = !isReviving
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "恢复模式",
                    style = MaterialTheme.typography.titleMedium
                )
                RevivalModeRow(
                    selected = mode == CharacterRevivalMode.REINTRODUCTION,
                    title = "重新认识",
                    description = "恢复角色设定，但不带回历史记忆",
                    onClick = { mode = CharacterRevivalMode.REINTRODUCTION }
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                RevivalModeRow(
                    selected = mode == CharacterRevivalMode.WITH_MEMORIES,
                    title = "恢复记忆",
                    description = "尝试恢复胶囊中保存的角色记忆",
                    onClick = { mode = CharacterRevivalMode.WITH_MEMORIES }
                )
            }

            when (val current = state) {
                CharacterRevivalState.Reviving -> Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Text("正在恢复角色…")
                }
                is CharacterRevivalState.Success -> Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "恢复成功：${current.character.name}",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    V9PMActionButton(label = "返回角色列表", onClick = onNavigateBack, modifier = Modifier.fillMaxWidth(), height = 44.dp)
                }
                is CharacterRevivalState.Failure -> Text(
                    current.message,
                    color = MaterialTheme.colorScheme.error
                )
                CharacterRevivalState.Idle -> Unit
            }

            V9PMActionButton(
                label = "开始恢复",
                onClick = startRevival,
                enabled = token.isNotBlank() && !isReviving,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RevivalModeRow(
    selected: Boolean,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    V9PMChoiceRow(
        title = title,
        subtitle = description,
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(vertical = 2.dp),
        trailing = {
            RadioButton(selected = selected, onClick = null)
        }
    )
}
