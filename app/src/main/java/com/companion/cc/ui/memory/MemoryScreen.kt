package com.companion.cc.ui.memory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.domain.model.MemoryLayered
import com.companion.cc.domain.model.PersonalityCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    companionId: String,
    onNavigateBack: () -> Unit,
    viewModel: MemoryViewModel = hiltViewModel()
) {
    val memories by viewModel.memories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(companionId) {
        viewModel.loadMemories(companionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("记忆库") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshMemories(companionId) }) {
                        Icon(Icons.Default.Refresh, "刷新")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 标签栏
            ScrollableTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("永久记忆 (${memories.permanent.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("长期记忆 (${memories.longTerm.size})") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("中期记忆 (${memories.midTerm.size})") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("短期记忆 (${memories.shortTerm.size})") }
                )
            }

            // 内容区域
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> PermanentMemoryList(memories.permanent)
                    1 -> LongTermMemoryList(memories.longTerm)
                    2 -> MidTermMemoryList(memories.midTerm)
                    3 -> ShortTermMemoryList(memories.shortTerm)
                }
            }
        }
    }
}

@Composable
private fun PermanentMemoryList(memories: List<MemoryLayered.Permanent>) {
    if (memories.isEmpty()) {
        EmptyMemoryView("暂无永久记忆")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 按类别分组
            memories.groupBy { it.category }.forEach { (category, categoryMemories) ->
                item {
                    Text(
                        text = category.displayName(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(categoryMemories) { memory ->
                    PermanentMemoryCard(memory)
                }
            }
        }
    }
}

@Composable
private fun PermanentMemoryCard(memory: MemoryLayered.Permanent) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = memory.trait,
                    style = MaterialTheme.typography.bodyLarge
                )
                Icon(
                    imageVector = getCategoryIcon(memory.category),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (memory.examples.isNotEmpty()) {
                Text(
                    text = "示例: ${memory.examples.first()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LongTermMemoryList(memories: List<MemoryLayered.LongTerm>) {
    if (memories.isEmpty()) {
        EmptyMemoryView("暂无长期记忆")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(memories) { memory ->
                LongTermMemoryCard(memory)
            }
        }
    }
}

@Composable
private fun LongTermMemoryCard(memory: MemoryLayered.LongTerm) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = memory.content,
                style = MaterialTheme.typography.bodyLarge
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = formatTimestamp(memory.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "重要性: ${(memory.importance * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun MidTermMemoryList(memories: List<MemoryLayered.MidTerm>) {
    if (memories.isEmpty()) {
        EmptyMemoryView("暂无中期记忆")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(memories) { memory ->
                MidTermMemoryCard(memory)
            }
        }
    }
}

@Composable
private fun MidTermMemoryCard(memory: MemoryLayered.MidTerm) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = memory.summary,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = formatTimestamp(memory.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShortTermMemoryList(memories: List<MemoryLayered.ShortTerm>) {
    if (memories.isEmpty()) {
        EmptyMemoryView("暂无短期记忆")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(memories) { memory ->
                ShortTermMemoryCard(memory)
            }
        }
    }
}

@Composable
private fun ShortTermMemoryCard(memory: MemoryLayered.ShortTerm) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (memory.message.role == com.companion.cc.domain.model.MessageRole.USER) "我" else "AI",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatTimestamp(memory.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = memory.message.content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun EmptyMemoryView(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getCategoryIcon(category: PersonalityCategory): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category) {
        PersonalityCategory.CORE_VALUES -> Icons.Default.Star
        PersonalityCategory.INTERESTS -> Icons.Default.Favorite
        PersonalityCategory.SPEAKING_STYLE -> Icons.Default.ChatBubble
        PersonalityCategory.BEHAVIOR_PATTERN -> Icons.Default.Repeat
        PersonalityCategory.RELATIONSHIP -> Icons.Default.People
        PersonalityCategory.BACKGROUND -> Icons.Default.Book
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

private fun PersonalityCategory.displayName(): String {
    return when (this) {
        PersonalityCategory.CORE_VALUES -> "核心价值观"
        PersonalityCategory.INTERESTS -> "兴趣爱好"
        PersonalityCategory.SPEAKING_STYLE -> "说话风格"
        PersonalityCategory.BEHAVIOR_PATTERN -> "行为模式"
        PersonalityCategory.RELATIONSHIP -> "关系定位"
        PersonalityCategory.BACKGROUND -> "背景"
    }
}
