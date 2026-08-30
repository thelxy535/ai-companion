package com.companion.cc.ui.memory

import com.companion.cc.ui.designsystem.auroraScreenBackground
import com.companion.cc.ui.theme.LocalVisualTheme
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.companion.cc.domain.model.Message
import com.companion.cc.domain.model.MessageRole
import com.companion.cc.domain.model.MessagesByDate
import com.companion.cc.ui.theme.LocalVisualTheme
import com.companion.cc.ui.theme.GlassDialogSurface
import com.companion.cc.ui.designsystem.pressableV5
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.layout.padding

/**
 * Memory Tree 界面
 * 参考: Notion, Obsidian, VS Code
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryTreeScreen(
    companionId: String,
    onNavigateBack: () -> Unit,
    onNavigateToReview: () -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    viewModel: MemoryTreeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val messagesByDate by viewModel.messagesByDate.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val totalMessages by viewModel.totalMessages.collectAsState()
    val totalDays by viewModel.totalDays.collectAsState()

    var showSearchDialog by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("all") }

    val selectedFilterLabel = when (selectedFilter) {
        "important" -> "重要"
        "user" -> "用户"
        "assistant" -> "AI 回复"
        else -> "全部"
    }

    // 监听生命周期，每次页面恢复（RESUMED）时重新加载数据
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            android.util.Log.d("MemoryTreeScreen", "生命周期事件: $event")
            if (event == Lifecycle.Event.ON_RESUME) {
                android.util.Log.d("MemoryTreeScreen", "页面恢复，重新加载消息")
                viewModel.loadMessages(companionId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = Modifier.auroraScreenBackground(LocalVisualTheme.current.tokens.backdrop.isDark),
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 44.dp),
                title = {
                    Column {
                        Text(
                            "记忆树",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$totalMessages 条消息 · $totalDays 天",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showSearchDialog = true }) {
                        Icon(Icons.Default.Search, "搜索")
                    }
                    IconButton(
                        onClick = {
                            showFilterMenu = false
                            showMoreMenu = true
                        }
                    ) {
                        Icon(Icons.Default.MoreVert, "更多")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
        ) {
            MemoryTreeShortcuts(
                selectedFilterLabel = selectedFilterLabel,
                onOpenLibrary = onNavigateToLibrary,
                onOpenReview = onNavigateToReview,
                onOpenFilter = {
                    showMoreMenu = false
                    showFilterMenu = true
                }
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            when {
                isLoading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                messagesByDate.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "当前角色还没有对话历史",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> TreeVisualizedMemoryList(
                    messagesByDate = messagesByDate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }

        // 统计对话框
        if (showStatsDialog) {
            MemoryStatsDialog(
                totalMessages = totalMessages,
                totalDays = totalDays,
                messagesByDate = messagesByDate,
                onDismiss = { showStatsDialog = false }
            )
        }

        // 删除确认对话框
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                icon = {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("清理记忆") },
                text = { Text("确定要清理当前角色的所有记忆吗？此操作不可撤销。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearAllMemories()
                            showDeleteDialog = false
                            android.widget.Toast.makeText(
                                context,
                                "已清理所有记忆",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("确定清理")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }

    // 搜索对话框
    if (showSearchDialog) {
        SearchDialog(
            onSearch = { query ->
                viewModel.searchMessages(query)
                showSearchDialog = false
            },
            onDismiss = { showSearchDialog = false }
        )
    }

    // 导入对话框
    if (showImportDialog) {
        ImportMemoryDialog(
            onImport = { jsonContent ->
                scope.launch {
                    val result = viewModel.importMemories(jsonContent)
                    result.onSuccess { count ->
                        android.widget.Toast.makeText(
                            context,
                            "成功导入 $count 条记忆",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }.onFailure { e ->
                        android.widget.Toast.makeText(
                            context,
                            "导入失败: ${e.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                showImportDialog = false
            },
            onDismiss = { showImportDialog = false }
        )
    }

    if (showFilterMenu) {
        MemoryTreeActionPopup(
            title = "筛选记忆",
            onDismiss = { showFilterMenu = false }
        ) {
            MemoryTreeActionItem(
                icon = Icons.Default.AllInclusive,
                label = "全部消息",
                selected = selectedFilter == "all",
                onClick = {
                    selectedFilter = "all"
                    viewModel.filterMessages("all")
                    showFilterMenu = false
                }
            )
            MemoryTreeActionItem(
                icon = Icons.Default.Star,
                label = "重要记忆",
                selected = selectedFilter == "important",
                onClick = {
                    selectedFilter = "important"
                    viewModel.filterMessages("important")
                    showFilterMenu = false
                }
            )
            MemoryTreeActionItem(
                icon = Icons.Default.Person,
                label = "用户消息",
                selected = selectedFilter == "user",
                onClick = {
                    selectedFilter = "user"
                    viewModel.filterMessages("user")
                    showFilterMenu = false
                }
            )
            MemoryTreeActionItem(
                icon = Icons.Default.SmartToy,
                label = "AI 回复",
                selected = selectedFilter == "assistant",
                onClick = {
                    selectedFilter = "assistant"
                    viewModel.filterMessages("assistant")
                    showFilterMenu = false
                }
            )
        }
    }

    if (showMoreMenu) {
        MemoryTreeActionPopup(
            title = "记忆操作",
            onDismiss = { showMoreMenu = false }
        ) {
            MemoryTreeActionItem(
                icon = Icons.Default.BarChart,
                label = "记忆统计",
                onClick = {
                    showMoreMenu = false
                    showStatsDialog = true
                }
            )
            MemoryTreeActionItem(
                icon = Icons.Default.Download,
                label = "导出记忆",
                onClick = {
                    showMoreMenu = false
                    try {
                        val json = viewModel.exportMemories()
                        val filename = "memories_${System.currentTimeMillis()}.json"
                        val file = java.io.File(context.getExternalFilesDir(null), filename)
                        file.writeText(json)
                        android.widget.Toast.makeText(
                            context,
                            "已导出到: ${file.absolutePath}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(
                            context,
                            "导出失败: ${e.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
            MemoryTreeActionItem(
                icon = Icons.Default.Upload,
                label = "导入记忆",
                onClick = {
                    showMoreMenu = false
                    showImportDialog = true
                }
            )
            Divider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            MemoryTreeActionItem(
                icon = Icons.Default.DeleteSweep,
                label = "清理记忆",
                destructive = true,
                onClick = {
                    showMoreMenu = false
                    showDeleteDialog = true
                }
            )
        }
    }
}

@Composable
private fun MemoryTreeShortcuts(
    selectedFilterLabel: String,
    onOpenLibrary: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenFilter: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        MemoryTreeShortcut(
            icon = Icons.Default.MenuBook,
            label = "记忆库",
            onClick = onOpenLibrary,
            modifier = Modifier.weight(1f)
        )
        MemoryTreeShortcut(
            icon = Icons.Default.Verified,
            label = "待审核",
            onClick = onOpenReview,
            modifier = Modifier.weight(1f)
        )
        MemoryTreeShortcut(
            icon = Icons.Default.FilterList,
            label = "筛选 · $selectedFilterLabel",
            onClick = onOpenFilter,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MemoryTreeShortcut(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visualTheme = LocalVisualTheme.current
    Column(
        modifier = modifier
            .pressableV5(
                onClick = onClick,
                isNight = visualTheme.tokens.backdrop.isDark
            )
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MemoryTreeActionPopup(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val density = LocalDensity.current
    val offset = with(density) {
        IntOffset(x = (-12).dp.roundToPx(), y = 68.dp.roundToPx())
    }

    Popup(
        alignment = Alignment.TopEnd,
        offset = offset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        GlassDialogSurface(
            modifier = Modifier.width(224.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = title,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                content()
            }
        }
    }
}

@Composable
private fun MemoryTreeActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean = false,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val visualTheme = LocalVisualTheme.current
    val contentColor = if (destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressableV5(
                onClick = onClick,
                isNight = visualTheme.tokens.backdrop.isDark
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = contentColor
        )
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "已选择",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoryStatsDialog(
    totalMessages: Int,
    totalDays: Int,
    messagesByDate: List<MessagesByDate>,
    onDismiss: () -> Unit
) {
    val userMessages = messagesByDate.flatMap { it.messages }.count { it.role == MessageRole.USER }
    val aiMessages = messagesByDate.flatMap { it.messages }.count { it.role == MessageRole.ASSISTANT }
    val importantMessages = messagesByDate.flatMap { it.messages }.count { it.importance > 70 }
    val avgMessagesPerDay = if (totalDays > 0) totalMessages.toFloat() / totalDays else 0f

    // 准备图表数据
    val dailyData = messagesByDate.take(7).reversed().map {
        com.companion.cc.ui.memory.components.DailyMessageData(
            date = it.date.substringAfter("-").replace("-", "/"),
            count = it.messages.size
        )
    }

    val emotionData = messagesByDate.flatMap { it.messages }
        .mapNotNull { it.emotion }
        .groupingBy { it }
        .eachCount()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "记忆统计",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            StatsRow(
                icon = Icons.Default.Chat,
                label = "总消息数",
                value = "$totalMessages 条"
            )
            StatsRow(
                icon = Icons.Default.CalendarMonth,
                label = "记录天数",
                value = "$totalDays 天"
            )
            StatsRow(
                icon = Icons.Default.TrendingUp,
                label = "日均消息",
                value = "%.1f 条".format(avgMessagesPerDay)
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            StatsRow(
                icon = Icons.Default.Person,
                label = "用户消息",
                value = "$userMessages 条"
            )
            StatsRow(
                icon = Icons.Default.SmartToy,
                label = "AI 回复",
                value = "$aiMessages 条"
            )
            StatsRow(
                icon = Icons.Default.Star,
                label = "重要记忆",
                value = "$importantMessages 条"
            )

            if (dailyData.isNotEmpty()) {
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                com.companion.cc.ui.memory.components.MessageTrendChart(
                    dailyData = dailyData,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (emotionData.isNotEmpty()) {
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                com.companion.cc.ui.memory.components.EmotionPieChart(
                    emotionData = emotionData,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("关闭")
            }
        }
    }
}

@Composable
private fun StatsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun MemoryDateGroup(
    date: String,
    messages: List<Message>
) {
    var isExpanded by remember { mutableStateOf(true) }

    Column {
        // 日期标题
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "📁", fontSize = 20.sp)
                    Text(
                        text = date,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "(${messages.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (isExpanded)
                        Icons.Default.ExpandMore
                    else
                        Icons.Default.ChevronRight,
                    contentDescription = null
                )
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        // 消息列表
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                messages.forEach { message ->
                    MemoryItem(message = message)
                }
            }
        }
    }
}

@Composable
private fun MemoryItem(message: Message) {
    val visualTheme = LocalVisualTheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* 打开详情 */ }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (message.importance > 80) "⭐" else "💬",
                fontSize = 18.sp
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(message.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (message.role == MessageRole.ASSISTANT) {
                        Text(
                            text = "AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (message.importance > 80) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .background(
                            visualTheme.tokens.status.importanceContainer,
                            MaterialTheme.shapes.extraSmall
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "重要",
                        style = MaterialTheme.typography.labelSmall,
                        color = visualTheme.tokens.status.importance
                    )
                }
            }
        }
        Divider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchDialog(
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedDateRange by remember { mutableStateOf("all") } // all, week, month, custom

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "搜索记忆",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("输入关键词") },
                placeholder = { Text("搜索消息内容…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "时间范围",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DateRangeOption(
                    label = "全部时间",
                    isSelected = selectedDateRange == "all",
                    onClick = { selectedDateRange = "all" }
                )
                DateRangeOption(
                    label = "最近 7 天",
                    isSelected = selectedDateRange == "week",
                    onClick = { selectedDateRange = "week" }
                )
                DateRangeOption(
                    label = "最近 30 天",
                    isSelected = selectedDateRange == "month",
                    onClick = { selectedDateRange = "month" }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("取消") }
                Button(
                    onClick = { onSearch(searchQuery) },
                    enabled = searchQuery.isNotBlank()
                ) {
                    Text("搜索")
                }
            }
        }
    }
}

@Composable
private fun DateRangeOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportMemoryDialog(
    onImport: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var jsonInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Upload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "导入记忆",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "粘贴从“导出记忆”功能生成的 JSON 内容。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = jsonInput,
                onValueChange = {
                    jsonInput = it
                    errorMessage = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                placeholder = { Text("粘贴 JSON 内容…") },
                isError = errorMessage != null,
                supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
            )

            Text(
                text = "导入只会添加新记忆，不会删除现有记忆。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("取消") }
                Button(
                    onClick = {
                        if (jsonInput.isBlank()) {
                            errorMessage = "请输入 JSON 内容"
                        } else {
                            try {
                                kotlinx.serialization.json.Json.parseToJsonElement(jsonInput)
                                onImport(jsonInput)
                            } catch (e: Exception) {
                                errorMessage = "JSON 格式错误"
                            }
                        }
                    },
                    enabled = jsonInput.isNotBlank()
                ) {
                    Text("导入")
                }
            }
        }
    }
}
