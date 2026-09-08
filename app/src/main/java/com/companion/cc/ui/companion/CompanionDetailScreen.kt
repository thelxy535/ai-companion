package com.companion.cc.ui.companion

import com.companion.cc.ui.designsystem.auroraScreenBackground
import com.companion.cc.ui.components.V9PMIconButton
import com.companion.cc.ui.components.V9PMTopBar
import com.companion.cc.ui.theme.LocalVisualTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.domain.model.CharacterAvatarResolver
import com.companion.cc.ui.components.CompanionAvatar
import com.companion.cc.ui.components.UtilitySection
import com.companion.cc.ui.settings.AvatarSettingsDialog
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionDetailScreen(
    companionId: String,
    onNavigateBack: () -> Unit,
    viewModel: CompanionDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(companionId) { viewModel.setCompanion(companionId) }
    val state by viewModel.uiState.collectAsState()
    when (val currentState = state) {
        CompanionDetailUiState.Loading -> CompanionDetailUnavailableScreen(
            title = "加载角色",
            message = null,
            onNavigateBack = onNavigateBack
        )
        CompanionDetailUiState.Missing -> CompanionDetailUnavailableScreen(
            title = "角色不可用",
            message = "无法加载该角色，可能已被删除或无权访问",
            onNavigateBack = onNavigateBack
        )
        is CompanionDetailUiState.Failure -> CompanionDetailUnavailableScreen(
            title = "角色不可用",
            message = currentState.message,
            onNavigateBack = onNavigateBack
        )
        is CompanionDetailUiState.Content -> CompanionDetailContent(
            state = currentState,
            onNavigateBack = onNavigateBack,
            onSaveAvatar = viewModel::saveAvatar
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompanionDetailContent(
    state: CompanionDetailUiState.Content,
    onNavigateBack: () -> Unit,
    onSaveAvatar: (String?) -> Unit
) {
    val character = state.character
    val resolvedAvatar = remember(state.avatar, character.avatar, character.name) {
        CharacterAvatarResolver.resolve(character, state.avatar)
    }
    var showAvatarDialog by remember(character.id) { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.auroraScreenBackground(LocalVisualTheme.current.tokens.backdrop.isDark),
        topBar = {
            V9PMTopBar(
                title = { Text("关于她") },
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                CompanionAvatar(
                    avatarUrl = resolvedAvatar.avatarUrl,
                    emoji = resolvedAvatar.emoji,
                    size = 120.dp
                )
                V9PMIconButton(
                    icon = Icons.Default.Edit,
                    contentDescription = "更换头像",
                    onClick = { showAvatarDialog = true },
                    size = 48.dp,
                    iconSize = 20.dp,
                    shape = com.companion.cc.ui.designsystem.smoothCorner(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = character.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = character.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            UtilitySection(title = "关系概览") {
                InfoRow(label = "相识", value = formatTimeSince(state.firstMetTimestamp ?: System.currentTimeMillis()))
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                InfoRow(
                    label = "聊天",
                    value = when {
                        state.messageCount < 50 -> "刚开始认识"
                        state.messageCount < 200 -> "聊了不少"
                        state.messageCount < 500 -> "聊了好久了"
                        else -> "老朋友了"
                    }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            UtilitySection(title = "关于 ${character.name}") {
                Text(
                    text = character.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 24.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showAvatarDialog) {
        AvatarSettingsDialog(
            currentAvatarUrl = state.avatar,
            title = "设置${character.name}的头像",
            onDismiss = { showAvatarDialog = false },
            onAvatarSelected = { uri ->
                onSaveAvatar(uri?.toString())
                showAvatarDialog = false
            },
            onClearAvatar = {
                onSaveAvatar(null)
                showAvatarDialog = false
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompanionDetailUnavailableScreen(
    title: String,
    message: String?,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            V9PMTopBar(
                title = { Text(title) },
                onNavigateBack = onNavigateBack,
                modifier = Modifier.padding(top = 44.dp)
            )
        },
        containerColor = Color.Transparent,
        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (message == null) CircularProgressIndicator() else Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTimeSince(timestamp: Long): String {
    val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - timestamp)
    val months = days / 30
    val years = days / 365
    return when {
        days < 7 -> "刚认识"
        days < 30 -> "认识${days}天了"
        months < 12 -> "认识${months}个月了"
        else -> "认识${years}年了"
    }
}
