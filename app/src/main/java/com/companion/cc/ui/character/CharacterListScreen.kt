package com.companion.cc.ui.character

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.domain.model.ChatCharacter
import com.companion.cc.data.character.CharacterExportFormat
import com.companion.cc.domain.model.CharacterAvatarResolver
import com.companion.cc.ui.designsystem.staggerRise
import com.companion.cc.ui.designsystem.smoothCorner
import com.companion.cc.ui.designsystem.rememberStaggerFirstPlay
import com.companion.cc.ui.designsystem.auroraScreenBackground
import com.companion.cc.ui.components.V9PMActionButton
import com.companion.cc.ui.components.V9PMDialogSurface
import com.companion.cc.ui.components.V9PMIconButton
import com.companion.cc.ui.theme.LocalVisualTheme
import com.companion.cc.ui.components.Avatar
import com.companion.cc.ui.theme.TactileGesture
import com.companion.cc.ui.theme.rememberTactileAction
import com.companion.cc.ui.theme.tactileLongClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * 角色列表界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    onNavigateBack: () -> Unit,
    onCreateCharacter: () -> Unit,
    onEditCharacter: (String) -> Unit,
    onDeleteCharacter: (String) -> Unit,
    onStartChat: (String) -> Unit = {},
    viewModel: CharacterCustomizationViewModel = hiltViewModel()
) {
    val characters by viewModel.characters.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // V9PM 头像一致性：设置页/聊天页自定义的伴侣头像覆盖，列表页同样应用
    val avatarOverrides by viewModel.avatarOverrides.collectAsStateWithLifecycle()
    val pendingImported by viewModel.pendingImportedCharacter.collectAsStateWithLifecycle()
    val importError by viewModel.importError.collectAsStateWithLifecycle()
    val pendingCardExport by viewModel.pendingCardExport.collectAsStateWithLifecycle()
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val export = pendingCardExport ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(export.second.toByteArray(Charsets.UTF_8))
                } ?: error("无法写入角色卡文件")
                viewModel.clearPendingCardExport()
            }.onFailure { error -> viewModel.setImportError(error.message ?: "角色卡导出失败") }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bytes = input.readBytes()
                require(bytes.size <= 2 * 1024 * 1024) { "角色卡文件过大" }
                viewModel.importCharacterCard(bytes.toString(Charsets.UTF_8))
            } ?: error("无法读取角色卡文件")
        }.onFailure { error ->
            viewModel.setImportError(error.message ?: "无法读取角色卡文件")
        }
    }
    // 冷加载标记：首次收到非空数据后置 true，此后为空才是真的没角色
    var hasLoadedOnce by remember { mutableStateOf(false) }
    if (characters.isNotEmpty()) hasLoadedOnce = true
    val createCharacter = rememberTactileAction {
        viewModel.startNewCharacter()
        onCreateCharacter()
    }

    val staggerPlay = rememberStaggerFirstPlay("character")
    LaunchedEffect(pendingCardExport) {
        pendingCardExport?.let { exportLauncher.launch(it.first) }
    }
    if (pendingImported != null) {
        val imported = pendingImported!!
        V9PMDialogSurface(onDismissRequest = viewModel::clearPendingImport) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("导入角色卡", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(imported.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text(imported.description.ifBlank { "未提供角色描述" }, maxLines = 4, overflow = TextOverflow.Ellipsis)
                Text("备用问候 ${imported.alternateGreetings.size} 条 · 标签 ${imported.tags.size} 个", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    V9PMActionButton(label = "取消", onClick = viewModel::clearPendingImport, modifier = Modifier.weight(1f), height = 40.dp)
                    V9PMActionButton(label = "导入并新建", onClick = viewModel::confirmImportedCharacter, modifier = Modifier.weight(1f), height = 40.dp)
                }
            }
        }
    } else if (importError != null) {
        V9PMDialogSurface(onDismissRequest = viewModel::clearPendingImport) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("角色卡导入失败", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(importError!!, color = MaterialTheme.colorScheme.error)
                V9PMActionButton(label = "关闭", onClick = viewModel::clearPendingImport, modifier = Modifier.fillMaxWidth(), height = 40.dp)
            }
        }
    }
    Scaffold(
        modifier = Modifier.auroraScreenBackground(LocalVisualTheme.current.tokens.backdrop.isDark),
        topBar = {
            // V7 view-head：大标题 + 副标题
            Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 8.dp)) {
                Text(
                    "角色",
                    modifier = Modifier.staggerRise(staggerPlay, 0),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "长按角色可编辑 · 点击开始聊天",
                    modifier = Modifier.staggerRise(staggerPlay, 1, intervalMs = 60),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                V9PMActionButton(
                    label = "导入角色卡",
                    icon = Icons.Default.FileOpen,
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/markdown", "text/plain")) },
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .widthIn(min = 156.dp),
                    height = 40.dp
                )
            }
        },
        containerColor = Color.Transparent,
        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
        floatingActionButton = {
            V9PMIconButton(
                icon = Icons.Default.Add,
                contentDescription = "创建角色",
                onClick = createCharacter,
                size = 56.dp,
                iconSize = 24.dp,
                shape = smoothCorner(24.dp),
                tint = Color.White
            )
        }
    ) { padding ->
        if (characters.isEmpty() && !hasLoadedOnce) {
            // V7 冷加载期：极光背景静默（不闪 EmptyState 引导页）
            Box(modifier = Modifier.fillMaxSize())
        } else if (characters.isEmpty()) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onCreateCharacter = createCharacter
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(
                    items = characters,
                    key = { _, c -> c.id }
                ) { index, character ->
                    Box(modifier = Modifier.staggerRise(staggerPlay, index)) {
                        CharacterRow(
                            character = character,
                            avatarOverride = avatarOverrides[character.id],
                            onStartChat = { onStartChat(character.id) },
                            onEdit = { onEditCharacter(character.id) },
                            onDelete = { onDeleteCharacter(character.id) },
                            onExport = if (character.isCustom()) { format -> viewModel.exportCharacterCard(character.id, format) } else null
                        )
                    }
                }
            }
        }
    }
}

/**
 * 空状态
 */
@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    onCreateCharacter: () -> Unit
) {
    Column(
        modifier = modifier
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "还没有自定义角色",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "创建属于你的独特AI伴侣",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        V9PMActionButton(
            label = "创建角色",
            icon = Icons.Default.Add,
            onClick = onCreateCharacter,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            height = 48.dp
        )
    }
}

/**
 * 角色行
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterRow(
    character: ChatCharacter,
    avatarOverride: String? = null,
    onStartChat: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExport: ((CharacterExportFormat) -> Unit)? = null
) {
    var showActionMenu by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    val startChat = rememberTactileAction(action = onStartChat)
    val editCharacter = rememberTactileAction(action = onEdit)

    // V9PM：长按行弹出操作菜单（编辑/删除/导出），不占行内空间
    val rowInteraction = if (character.isCustom()) {
        Modifier.tactileLongClickable(
            role = androidx.compose.ui.semantics.Role.Button,
            onClick = startChat,
            onLongClick = { showActionMenu = true }
        )
    } else {
        Modifier.clickable(onClick = startChat)
    }

    // V7 会话行玻璃卡片参数：18 圆角 + hairline 描边
    val rowShape = com.companion.cc.ui.designsystem.smoothCorner(24.dp) // V9PM 角色卡连续大圆角
    val rowNight = com.companion.cc.ui.theme.LocalVisualTheme.current.tokens.backdrop.isDark
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .background(
                if (rowNight) androidx.compose.ui.graphics.Color(0x59142028) else androidx.compose.ui.graphics.Color(0x66FFFFFF),
                rowShape
            )
            .border(
                1.dp,
                if (rowNight) androidx.compose.ui.graphics.Color(0x1AFFFFFF) else androidx.compose.ui.graphics.Color(0xCCFFFFFF),
                rowShape
            )
            .then(rowInteraction)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像（V9PM：应用 DataStore 覆盖，与聊天页/设置页一致）
            val avatar = CharacterAvatarResolver.resolve(character, overrideAvatar = avatarOverride)
            Avatar(
                backgroundColor = com.companion.cc.ui.components.auraColorFor(character.name),
                avatarUrl = avatar.avatarUrl,
                emoji = avatar.emoji,
                size = 56.dp,
                showRing = true,
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = character.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 特质标签（仅自定义角色显示）
                if (character is ChatCharacter.Custom) {
                    Text(
                        text = "自定义角色",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 操作按钮：内置角色只提供聊天；自定义角色长按弹出操作菜单（编辑/删除/导出）
            // V9PM：操作区紧凑布局——聊天按钮 + 导出图标（自定义角色长按行调出删除等更多操作）
            Column(horizontalAlignment = Alignment.End) {
                V9PMActionButton(
                    label = "开始聊天",
                    onClick = startChat,
                    modifier = Modifier.widthIn(min = 96.dp).height(40.dp),
                    height = 40.dp
                )
                onExport?.let { export ->
                    V9PMIconButton(
                        icon = Icons.Default.FileDownload,
                        contentDescription = "导出角色卡",
                        onClick = { showExportMenu = true },
                        size = 40.dp,
                        iconSize = 18.dp
                    )
                }
            }
        }
    }

    // V9PM：长按行弹出操作菜单（编辑/删除/导出）
    if (showActionMenu) {
        V9PMDialogSurface(onDismissRequest = { showActionMenu = false }) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(character.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                V9PMActionButton(
                    label = "编辑角色",
                    icon = Icons.Default.Edit,
                    onClick = {
                        showActionMenu = false
                        onEdit()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    height = 44.dp
                )
                onExport?.let { export ->
                    V9PMActionButton(
                        label = "导出角色卡",
                        icon = Icons.Default.FileDownload,
                        onClick = {
                            showActionMenu = false
                            showExportMenu = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        height = 44.dp
                    )
                }
                V9PMActionButton(
                    label = "删除角色",
                    icon = Icons.Default.Delete,
                    onClick = {
                        showActionMenu = false
                        onDelete()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    height = 44.dp,
                    destructive = true
                )
                V9PMActionButton(
                    label = "取消",
                    onClick = { showActionMenu = false },
                    modifier = Modifier.fillMaxWidth(),
                    height = 40.dp
                )
            }
        }
    }

    if (showExportMenu && onExport != null) {
        V9PMDialogSurface(onDismissRequest = { showExportMenu = false }) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("导出 ${character.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("选择文件格式", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                CharacterExportFormat.values().forEach { format ->
                    V9PMActionButton(
                        label = format.label,
                        icon = Icons.Default.FileDownload,
                        onClick = {
                            showExportMenu = false
                            onExport(format)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        height = 44.dp
                    )
                }
            }
        }
    }
}

/**
 * 特质标签
 */
@Composable
fun TraitChip(text: String) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
