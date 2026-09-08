package com.companion.cc.ui.home

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import com.companion.cc.ui.designsystem.staggerRise
import com.companion.cc.ui.designsystem.rememberStaggerFirstPlay
import com.companion.cc.ui.designsystem.pressableV5
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.domain.model.ChatCharacter
import com.companion.cc.domain.model.CharacterAvatarResolver
import com.companion.cc.ui.components.Avatar
import com.companion.cc.ui.theme.LocalVisualTheme
import com.companion.cc.ui.designsystem.auroraScreenBackground
import com.companion.cc.ui.theme.LocalFontScale
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.MaterialTheme

/**
 * 沉浸式主页
 *
 * 响应式目录投影：
 * - 显示所有角色（内置+自定义）
 * - 按最后消息时间排序
 * - 实时更新在线状态
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmersiveHomeScreen(
    onCompanionSelected: (String) -> Unit,
    onCustomCharacterSelected: (String) -> Unit = {},
    onNavigateToCharacterList: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val avatarOverrides by viewModel.avatarOverrides.collectAsStateWithLifecycle()

    // V9PM：背景统一由根容器权威极光承担（本页不再自绘）
    val night = LocalVisualTheme.current.tokens.backdrop.isDark

    // V7 首帧淡入：极光+内容 300ms 渐现（消除冷启动"空白→突现"跳变）
    val staggerPlay = rememberStaggerFirstPlay("home")
    // V8.1 主题切换 .8s ease：主页极光三色+底色对齐全局节奏（与其他页一致）
    val auroraSpec = tween<Color>(800, easing = LinearEasing)
    val cBase by androidx.compose.animation.animateColorAsState(if (night) Color(0xFF10121B) else Color(0xFFF9F8FF), auroraSpec, label = "hBase")
    val cBlobA by androidx.compose.animation.animateColorAsState(if (night) Color(0xFF5B74C4) else Color(0xFF93BCEE), auroraSpec, label = "hBlobA")
    val cBlobB by androidx.compose.animation.animateColorAsState(if (night) Color(0xFF645BB8) else Color(0xFFE2C2EC), auroraSpec, label = "hBlobB")
    val cBlobC by androidx.compose.animation.animateColorAsState(if (night) Color(0xFF20364C) else Color(0xFFD5E6E2), auroraSpec, label = "hBlobC")
    Scaffold(
        modifier = Modifier
            .auroraScreenBackground(night),
        topBar = {
            // V7 view-head：大标题（避开顶部状态胶囊区域：胶囊高约 44dp + 边距）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 6.dp)
            ) {
                Text(
                    modifier = Modifier.staggerRise(staggerPlay, 0, durationMillis = 220),
                    text = "消息",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // V7 统计副标题：X 位角色 · Y 位正在回复
                val onlineCount = uiState.let { state ->
                    when (state) {
                        is HomeUiState.Content -> state.items.count { it.isOnline }
                        else -> 0
                    }
                }
                val totalCount = uiState.let { state ->
                    when (state) {
                        is HomeUiState.Content -> state.items.size
                        else -> 0
                    }
                }
                Text(
                    modifier = Modifier.staggerRise(staggerPlay, 1, intervalMs = 40, durationMillis = 220),
                    text = "$totalCount 位角色 · $onlineCount 位在线",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { padding ->
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is HomeUiState.Failure -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            is HomeUiState.Content -> {
            // V9PM：主页就绪后后台预热消息缓存——任何角色首次进聊天都零空窗
            LaunchedEffect(state.items.map { it.character.id }.toSet()) {
                kotlinx.coroutines.delay(800)
                viewModel.prewarmMessageCaches(state.items)
            }
                if (state.items.isEmpty()) {
                    // V9PM 消息页空状态：emoji aura 圆底 96dp → 标题 → 副标题 → CTA 药丸（m-rise 进场）
                    val emptyStagger = com.companion.cc.ui.designsystem.rememberStaggerFirstPlay("home-empty")
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                            )
                                        )
                                    )
                                    .staggerRise(emptyStagger, 0),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("💬", fontSize = 64.sp)
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                "和 TA 开始对话吧",
                                fontSize = (17f * LocalFontScale.current).sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.staggerRise(emptyStagger, 1, intervalMs = 60)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "一切从这里开始",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.staggerRise(emptyStagger, 2, intervalMs = 60)
                            )
                            Spacer(modifier = Modifier.height(22.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .pressableV5(
                                        onClick = onNavigateToCharacterList,
                                        isNight = night,
                                        outlineShape = RoundedCornerShape(999.dp)
                                    )
                                    .padding(horizontal = 28.dp, vertical = 12.dp)
                                    .staggerRise(emptyStagger, 3, intervalMs = 60)
                            ) {
                                Text(
                                    "开始聊天",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .background(Color.Transparent),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        itemsIndexed(
                            items = state.items,
                            key = { _, entry -> entry.character.id }
                        ) { index, item ->
                            Box(modifier = Modifier.staggerRise(staggerPlay, index, intervalMs = 32, durationMillis = 220)) {
                                ChatListItem(
                avatarOverride = avatarOverrides[item.character.id],
                                    item = item,
                                    onClick = {
                                        viewModel.markConversationRead(item.character.id)
                                        if (item.character is ChatCharacter.Custom) {
                                            onCustomCharacterSelected(item.character.id)
                                        } else {
                                            onCompanionSelected(item.character.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 聊天列表项
 */
@Composable
fun ChatListItem(
    item: HomeCharacterItem,
    onClick: () -> Unit,
    avatarOverride: String? = null
) {
    val night = LocalVisualTheme.current.tokens.backdrop.isDark
    // V7 会话行：L1 G-Thin 玻璃卡片（18 圆角 + hairline 描边）
    val rowShape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .background(
                if (night) Color(0x59142028) else Color(0x66FFFFFF),
                rowShape
            )
            .border(1.dp, if (night) Color(0x1AFFFFFF) else Color(0xCCFFFFFF), rowShape)
            .pressableV5(onClick, isNight = night)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像（V7 aura 渐变 + 光环；在线状态以整圈绿色环表示）
            val avatar = CharacterAvatarResolver.resolve(item.character, avatarOverride)
            Avatar(
                backgroundColor = com.companion.cc.ui.components.auraColorFor(item.character.name),
                avatarUrl = avatar.avatarUrl,
                emoji = avatar.emoji,
                size = 56.dp,
                showRing = true,
                showOnlineRing = item.isOnline,
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 信息列（名字 + 最后消息）
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.character.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = (17f * com.companion.cc.ui.theme.LocalFontScale.current).sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        item.hasUnreadMessage -> "有新消息"
                        item.lastMessage != null -> item.lastMessage.content
                        else -> "还没有消息"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.hasUnreadMessage) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (item.hasUnreadMessage) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // V7 row-side：竖排时间列 + 在线徽标
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (item.hasUnreadMessage) {
                    Text(
                        text = "新消息",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                item.lastMessage?.let { message ->
                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.isOnline) {
                    Text(
                        text = "在线",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2EA985),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 格式化时间戳
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = now }
    val todayDay = cal.get(java.util.Calendar.DAY_OF_YEAR)
    cal.timeInMillis = timestamp
    val msgDay = cal.get(java.util.Calendar.DAY_OF_YEAR)
    val isYesterday = (todayDay - msgDay == 1) || (todayDay == 1 && msgDay >= 364) // 跨年昨天

    return when {
        diff < 60_000 -> "刚刚"
        diff < 3600_000 -> "${diff / 60_000}分钟前"
        isYesterday -> "昨天 " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        diff < 86400_000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        diff < 604800_000 -> SimpleDateFormat("E HH:mm", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(timestamp))
    }
}
