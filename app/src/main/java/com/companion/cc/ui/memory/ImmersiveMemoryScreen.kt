package com.companion.cc.ui.memory

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.domain.model.MemoryLayered

/**
 * 沉浸式记忆页面 - Material 3 Expressive
 */
@Composable
fun ImmersiveMemoryScreen(
    companionId: String,
    onNavigateBack: () -> Unit,
    viewModel: MemoryViewModel = hiltViewModel()
) {
    val memories by viewModel.memories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(companionId) {
        viewModel.loadMemories(companionId)
    }

    val backgroundColor = getBackgroundForCompanion(companionId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundColor)
    ) {
        // 顶部栏
        ImmersiveMemoryTopBar(
            companionId = companionId,
            onNavigateBack = onNavigateBack,
            onRefresh = { viewModel.refreshMemories(companionId) }
        )

        // 标签栏
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = getAccentColor(companionId),
            edgePadding = 16.dp
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("永久记忆") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("长期记忆") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("中期记忆") }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("短期记忆") }
            )
        }

        // 内容区域
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = getAccentColor(companionId)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    0 -> items(memories.permanent) { memory ->
                        PermanentMemoryCard(memory, companionId)
                    }
                    1 -> items(memories.longTerm) { memory ->
                        LongTermMemoryCard(memory, companionId)
                    }
                    2 -> items(memories.midTerm) { memory ->
                        MidTermMemoryCard(memory, companionId)
                    }
                    3 -> items(memories.shortTerm) { memory ->
                        ShortTermMemoryCard(memory, companionId)
                    }
                }
            }
        }
    }
}

/**
 * 顶部栏
 */
@Composable
private fun ImmersiveMemoryTopBar(
    companionId: String,
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "${getCompanionName(companionId)}的记忆",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "刷新",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * 永久记忆卡片
 */
@Composable
private fun PermanentMemoryCard(
    memory: MemoryLayered.Permanent,
    companionId: String
) {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true },  // 点击查看完整内容
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = getAccentColor(companionId).copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(getAccentColor(companionId).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(memory.category),
                    contentDescription = null,
                    tint = getAccentColor(companionId),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = memory.category.displayName(),
                    style = MaterialTheme.typography.labelMedium,
                    color = getAccentColor(companionId),
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = memory.trait,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,  // 最多显示3行
                    overflow = TextOverflow.Ellipsis
                )

                // 如果文字较长，显示提示
                if (memory.trait.length > 50) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "点击查看完整内容",
                        style = MaterialTheme.typography.labelSmall,
                        color = getAccentColor(companionId),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // 详情对话框
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = memory.category.displayName(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                // 使用 Column 包装，确保内容可以滚动
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = memory.trait,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }
}

/**
 * 长期记忆卡片
 */
@Composable
private fun LongTermMemoryCard(
    memory: MemoryLayered.LongTerm,
    companionId: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "重要度: ${memory.importance}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = memory.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = Int.MAX_VALUE  // 允许多行显示
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatTimestamp(memory.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 中期记忆卡片
 */
@Composable
private fun MidTermMemoryCard(
    memory: MemoryLayered.MidTerm,
    companionId: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = memory.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = Int.MAX_VALUE  // 允许多行显示
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatTimestamp(memory.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 短期记忆卡片
 */
@Composable
private fun ShortTermMemoryCard(
    memory: MemoryLayered.ShortTerm,
    companionId: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (memory.message.role == com.companion.cc.domain.model.MessageRole.USER)
                    Icons.Default.Person else Icons.Default.SmartToy,
                contentDescription = null,
                tint = getAccentColor(companionId),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = memory.message.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = Int.MAX_VALUE  // 允许多行显示
                )
            }
        }
    }
}

// 辅助函数
@Composable
private fun getBackgroundForCompanion(companionId: String): Brush {
    return when (companionId) {
        "muse" -> Brush.verticalGradient(
            listOf(Color(0xFFFAFAFA), Color(0xFFF3E5F5))
        )
        "xiaocan" -> Brush.verticalGradient(
            listOf(Color(0xFFFFFAFA), Color(0xFFFFF0F5))
        )
        else -> Brush.verticalGradient(
            listOf(Color(0xFFFFFFFF), Color(0xFFF5F5F5))
        )
    }
}

@Composable
private fun getAccentColor(companionId: String): Color {
    return when (companionId) {
        "muse" -> Color(0xFF9C88FF)
        "xiaocan" -> Color(0xFFFFB3D9)
        else -> MaterialTheme.colorScheme.primary
    }
}

private fun getCompanionName(companionId: String): String {
    return when (companionId) {
        "muse" -> "缪斯"
        "xiaocan" -> "小璨"
        else -> "伴侣"
    }
}

private fun getCategoryIcon(category: com.companion.cc.domain.model.PersonalityCategory): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category) {
        com.companion.cc.domain.model.PersonalityCategory.CORE_VALUES -> Icons.Default.Star
        com.companion.cc.domain.model.PersonalityCategory.INTERESTS -> Icons.Default.Favorite
        com.companion.cc.domain.model.PersonalityCategory.SPEAKING_STYLE -> Icons.Default.ChatBubble
        com.companion.cc.domain.model.PersonalityCategory.BEHAVIOR_PATTERN -> Icons.Default.Settings
        com.companion.cc.domain.model.PersonalityCategory.RELATIONSHIP -> Icons.Default.People
        com.companion.cc.domain.model.PersonalityCategory.BACKGROUND -> Icons.Default.Book
    }
}

private fun com.companion.cc.domain.model.PersonalityCategory.displayName(): String {
    return when (this) {
        com.companion.cc.domain.model.PersonalityCategory.CORE_VALUES -> "核心价值观"
        com.companion.cc.domain.model.PersonalityCategory.INTERESTS -> "兴趣爱好"
        com.companion.cc.domain.model.PersonalityCategory.SPEAKING_STYLE -> "说话风格"
        com.companion.cc.domain.model.PersonalityCategory.BEHAVIOR_PATTERN -> "行为模式"
        com.companion.cc.domain.model.PersonalityCategory.RELATIONSHIP -> "关系定位"
        com.companion.cc.domain.model.PersonalityCategory.BACKGROUND -> "背景"
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
