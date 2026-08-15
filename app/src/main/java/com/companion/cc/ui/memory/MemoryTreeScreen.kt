package com.companion.cc.ui.memory

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.companion.cc.domain.model.Message
import com.companion.cc.domain.model.MessageRole
import com.companion.cc.domain.model.MessagesByDate
import com.companion.cc.ui.theme.FunctionalColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Memory Tree 界面
 * 参考: Notion, Obsidian, VS Code
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryTreeScreen(
    onNavigateBack: () -> Unit,
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

    // 监听生命周期，每次页面恢复（RESUMED）时重新加载数据
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            android.util.Log.d("MemoryTreeScreen", "生命周期事件: $event")
            if (event == Lifecycle.Event.ON_RESUME) {
                android.util.Log.d("MemoryTreeScreen", "页面恢复，重新加载消息")
                viewModel.loadMessages()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
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
                    // 搜索按钮
                    IconButton(onClick = { showSearchDialog = true }) {
                        Icon(Icons.Default.Search, "搜索")
                    }

                    // 过滤按钮
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, "过滤")
                    }

                    // 更多菜单（三个点）
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Default.MoreVert, "更多")
                    }

                    // 过滤菜单
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("全部消息") },
                            leadingIcon = { Icon(Icons.Default.AllInclusive, null) },
                            onClick = {
                                viewModel.filterMessages("all")
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("重要记忆") },
                            leadingIcon = { Icon(Icons.Default.Star, null) },
                            onClick = {
                                viewModel.filterMessages("important")
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("用户消息") },
                            leadingIcon = { Icon(Icons.Default.Person, null) },
                            onClick = {
                                viewModel.filterMessages("user")
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("AI回复") },
                            leadingIcon = { Icon(Icons.Default.SmartToy, null) },
                            onClick = {
                                viewModel.filterMessages("assistant")
                                showFilterMenu = false
                            }
                        )
                    }

                    // 更多菜单（参考实时记录的风格）
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("记忆统计") },
                            leadingIcon = { Icon(Icons.Default.BarChart, null) },
                            onClick = {
                                showStatsDialog = true
                                showMoreMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("导出记忆") },
                            leadingIcon = { Icon(Icons.Default.Download, null) },
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
                        DropdownMenuItem(
                            text = { Text("导入记忆") },
                            leadingIcon = { Icon(Icons.Default.Upload, null) },
                            onClick = {
                                showMoreMenu = false
                                showImportDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("清理记忆") },
                            leadingIcon = { Icon(Icons.Default.DeleteSweep, null) },
                            onClick = {
                                showDeleteDialog = true
                                showMoreMenu = false
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // 使用树状可视化组件
            TreeVisualizedMemoryList(
                messagesByDate = messagesByDate,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
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
                icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                title = { Text("清理记忆") },
                text = { Text("确定要清理所有记忆吗？此操作不可撤销。") },
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
                        }
                    ) {
                        Text("确定")
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
}

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

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
        title = { Text("记忆统计", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 总体统计
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

                Divider()

                // 分类统计
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

                Divider()

                // 消息趋势图
                if (dailyData.isNotEmpty()) {
                    com.companion.cc.ui.memory.components.MessageTrendChart(
                        dailyData = dailyData,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Divider()

                // 情感分布图
                if (emotionData.isNotEmpty()) {
                    com.companion.cc.ui.memory.components.EmotionPieChart(
                        emotionData = emotionData,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
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
        Surface(
            onClick = { isExpanded = !isExpanded },
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small
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
    Surface(
        onClick = { /* 打开详情 */ },
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Text(
                text = if (message.importance > 80) "⭐" else "💬",
                fontSize = 18.sp
            )

            // 内容
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
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

            // 重要性标记
            if (message.importance > 80) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = FunctionalColors.importantBg
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "重要",
                            style = MaterialTheme.typography.labelSmall,
                            color = FunctionalColors.importantText
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
private fun SearchDialog(
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedDateRange by remember { mutableStateOf("all") } // all, week, month, custom

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("搜索记忆") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 关键词输入
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("输入关键词") },
                    placeholder = { Text("搜索消息内容...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 日期范围选择
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
                        label = "最近7天",
                        isSelected = selectedDateRange == "week",
                        onClick = { selectedDateRange = "week" }
                    )
                    DateRangeOption(
                        label = "最近30天",
                        isSelected = selectedDateRange == "month",
                        onClick = { selectedDateRange = "month" }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (searchQuery.isNotBlank()) {
                        // 这里可以传递日期范围信息，暂时只用关键词
                        onSearch(searchQuery)
                    }
                },
                enabled = searchQuery.isNotBlank()
            ) {
                Text("搜索")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
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

@Composable
private fun ImportMemoryDialog(
    onImport: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var jsonInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Upload, contentDescription = null) },
        title = { Text("导入记忆", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "粘贴从\"导出记忆\"功能生成的 JSON 内容：",
                    style = MaterialTheme.typography.bodyMedium
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
                    placeholder = { Text("粘贴 JSON 内容...") },
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                )

                Text(
                    text = "⚠️ 导入会添加新记忆，不会删除现有记忆",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (jsonInput.isBlank()) {
                        errorMessage = "请输入 JSON 内容"
                    } else {
                        try {
                            // 简单验证 JSON 格式
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
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
