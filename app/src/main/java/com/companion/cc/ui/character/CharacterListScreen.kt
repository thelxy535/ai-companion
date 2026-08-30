package com.companion.cc.ui.character

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.domain.model.ChatCharacter
import com.companion.cc.domain.model.CharacterAvatarResolver
import com.companion.cc.ui.designsystem.staggerRise
import com.companion.cc.ui.designsystem.smoothCorner
import com.companion.cc.ui.designsystem.rememberStaggerFirstPlay
import com.companion.cc.ui.designsystem.auroraScreenBackground
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
    val characters by viewModel.characters.collectAsState()
    // 冷加载标记：首次收到非空数据后置 true，此后为空才是真的没角色
    var hasLoadedOnce by remember { mutableStateOf(false) }
    if (characters.isNotEmpty()) hasLoadedOnce = true
    val createCharacter = rememberTactileAction {
        viewModel.startNewCharacter()
        onCreateCharacter()
    }

    val staggerPlay = rememberStaggerFirstPlay("character")
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
            }
        },
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = createCharacter,
                modifier = Modifier.size(56.dp),
                // V7 设计稿：圆角方形
                shape = smoothCorner(24.dp),   // V9PM 连续大圆角
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "创建角色")
            }
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
                            onStartChat = { onStartChat(character.id) },
                            onEdit = { onEditCharacter(character.id) },
                            onDelete = { onDeleteCharacter(character.id) }
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
        Button(onClick = onCreateCharacter) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("创建角色")
        }
    }
}

/**
 * 角色行
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterRow(
    character: ChatCharacter,
    onStartChat: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val startChat = rememberTactileAction(action = onStartChat)
    val editCharacter = rememberTactileAction(action = onEdit)
    val openDeleteDialog = rememberTactileAction {
        showDeleteDialog = true
    }
    val confirmDelete = rememberTactileAction(gesture = TactileGesture.DESTRUCTIVE_CONFIRM) {
        onDelete()
        showDeleteDialog = false
    }

    // V7 会话行玻璃卡片参数：18 圆角 + hairline 描边
    val rowShape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
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
            .tactileLongClickable(
                role = androidx.compose.ui.semantics.Role.Button,
                onClick = startChat,
                onLongClick = editCharacter
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像
            val avatar = CharacterAvatarResolver.resolve(character.avatar, character.name)
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

            // 操作按钮（仅自定义角色显示编辑和删除）
            // V7 char-cta：开始聊天药丸（gk 玻璃底 + accent 字 + 999 圆角）
            TextButton(
                onClick = startChat,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                // V7 设计稿：描边胶囊
                modifier = Modifier
                    .height(30.dp)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                        RoundedCornerShape(999.dp)
                    )
            ) {
                Text("开始聊天", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            // V7 设计稿：行右侧仅 CTA，编辑入口走长按
        }
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("删除角色") },
            text = { Text("确定要删除「${character.name}」吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = confirmDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
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
