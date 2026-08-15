package com.companion.cc.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.domain.manager.OnlineStatusManager
import com.companion.cc.domain.repository.MessageRepository
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.*

/**
 * 聊天列表风格首页
 *
 * 设计理念：
 * - 完全模仿真实通讯软件（微信/Telegram/LINE）
 * - 无任何"AI"字眼
 * - 名字下方显示最新消息预览（像微信）
 * - 时间显示为"最后活跃时间"
 * - 按最后消息时间排序
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmersiveHomeScreen(
    onCompanionSelected: (String) -> Unit,
    onCustomCharacterSelected: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit,
    onNavigateToCharacterList: () -> Unit = {},
    messageRepository: MessageRepository = hiltViewModel<HomeViewModel>().messageRepository,
    viewModel: HomeViewModel = hiltViewModel(),
    onlineStatusManager: OnlineStatusManager = hiltViewModel<HomeViewModel>().onlineStatusManager
) {
    // 获取最后消息时间来排序
    var companions by remember { mutableStateOf<List<CompanionInfo>>(emptyList()) }

    // 监听在线状态（使用全局单例）
    val onlineCompanions by onlineStatusManager.onlineCompanions.collectAsState()

    // 监听自定义角色
    val customCharacters by viewModel.customCharacters.collectAsState()

    LaunchedEffect(Unit) {
        // 获取每个角色的最后消息时间和内容（userId 从 SettingsManager 获取）
        val userId = viewModel.getUserId()

        val museMessages = messageRepository.getMessages(userId, "muse").firstOrNull()
        val museLastMessage = museMessages?.lastOrNull()
        val museLastTime = museLastMessage?.timestamp ?: 0L
        val museLastContent = museLastMessage?.content ?: "还没有消息"

        val xiaocanMessages = messageRepository.getMessages(userId, "xiaocan").firstOrNull()
        val xiaocanLastMessage = xiaocanMessages?.lastOrNull()
        val xiaocanLastTime = xiaocanLastMessage?.timestamp ?: 0L
        val xiaocanLastContent = xiaocanLastMessage?.content ?: "还没有消息"

        val builtInCompanions = listOf(
            CompanionInfo(
                id = "muse",
                name = "缪斯",
                avatar = "🎭",
                status = museLastContent,
                lastMessageTime = museLastTime,
                isOnline = false,
                isCustomCharacter = false
            ),
            CompanionInfo(
                id = "xiaocan",
                name = "小璨",
                avatar = "🌸",
                status = xiaocanLastContent,
                lastMessageTime = xiaocanLastTime,
                isOnline = false,
                isCustomCharacter = false
            )
        )

        companions = builtInCompanions.sortedByDescending { it.lastMessageTime }
    }

    // 动态合并自定义角色
    LaunchedEffect(customCharacters, companions) {
        val userId = viewModel.getUserId()

        val customCompanionInfos = customCharacters.map { character ->
            val messages = messageRepository.getMessages(userId, character.id).firstOrNull()
            val lastMessage = messages?.lastOrNull()

            CompanionInfo(
                id = character.id,
                name = character.name,
                avatar = character.name.take(1),
                status = lastMessage?.content ?: "还没有消息",
                lastMessageTime = lastMessage?.timestamp ?: 0L,
                isOnline = false,
                isCustomCharacter = true
            )
        }

        // 合并并重新排序
        val builtIn = companions.filter { !it.isCustomCharacter }
        companions = (builtIn + customCompanionInfos).sortedByDescending { it.lastMessageTime }
    }

    // 动态更新在线状态
    LaunchedEffect(onlineCompanions) {
        if (companions.isNotEmpty()) {
            companions = companions.map { companion ->
                companion.copy(isOnline = onlineCompanions.contains(companion.id))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "消息",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 动态排序的联系人列表
            companions.forEachIndexed { index, companion ->
                ChatListItem(
                    companionInfo = companion,
                    onClick = {
                        if (companion.isCustomCharacter) {
                            onCustomCharacterSelected(companion.id)
                        } else {
                            onCompanionSelected(companion.id)
                        }
                    }
                )

                if (index < companions.size - 1) {
                    Divider(
                        modifier = Modifier.padding(start = 88.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }

            // 自定义角色入口
            Divider(
                modifier = Modifier.padding(start = 88.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            CustomCharacterEntry(
                onClick = onNavigateToCharacterList
            )
        }
    }
}

/**
 * 联系人信息数据类
 */
data class CompanionInfo(
    val id: String,
    val name: String,
    val avatar: String,
    val status: String,
    val lastMessageTime: Long,
    val isOnline: Boolean,
    val isCustomCharacter: Boolean = false
)

/**
 * 聊天列表项
 *
 * 完全模仿真实通讯软件的联系人条目：
 * 头像 + 名字 + 最新消息预览 + 最后活跃时间
 */
@Composable
private fun ChatListItem(
    companionInfo: CompanionInfo,
    onClick: () -> Unit
) {
    // 格式化时间显示：统一用"最后活跃时间"，避免在线状态不一致的困惑
    val timeDisplay = remember(companionInfo.lastMessageTime, companionInfo.isOnline) {
        if (companionInfo.lastMessageTime == 0L) {
            ""  // 没有聊过天
        } else {
            val now = System.currentTimeMillis()
            val diff = now - companionInfo.lastMessageTime
            when {
                diff < 60_000 -> "刚刚活跃"
                diff < 3_600_000 -> "${diff / 60_000}分钟前活跃"
                diff < 86_400_000 -> "${diff / 3_600_000}小时前活跃"
                diff < 172_800_000 -> "昨天活跃"
                diff < 604_800_000 -> "${diff / 86_400_000}天前活跃"
                else -> {
                    val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
                    sdf.format(Date(companionInfo.lastMessageTime))
                }
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像 + 在线状态
            Box {
                // 头像背景
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            when (companionInfo.id) {
                                "muse" -> Color(0xFFE8DEF8)
                                else -> Color(0xFFFFD8E4)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = companionInfo.avatar,
                        fontSize = 28.sp
                    )
                }

                // 在线状态指示器（正在回复时显示绿点）
                if (companionInfo.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 信息栏
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 名字和时间
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = companionInfo.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (timeDisplay.isNotEmpty()) {
                        Text(
                            text = timeDisplay,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (companionInfo.isOnline)
                                Color(0xFF4CAF50)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 最新消息预览（像微信一样）
                Text(
                    text = companionInfo.status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 自定义角色入口（聊天列表风格）
 */
@Composable
private fun CustomCharacterEntry(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 信息
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "自定义角色",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "创建属于你的独特 AI 伴侣",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
