package com.companion.cc.ui.data

import com.companion.cc.ui.designsystem.auroraScreenBackground
import com.companion.cc.ui.theme.LocalVisualTheme
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.ui.components.AdaptiveMetricGrid
import com.companion.cc.ui.components.MetricGridItem
import com.companion.cc.ui.components.SceneAction
import com.companion.cc.ui.components.SceneActionTone
import com.companion.cc.ui.components.SceneSection
import com.companion.cc.ui.components.SceneSectionTone
import com.companion.cc.ui.components.SceneTopBar
import com.companion.cc.ui.theme.LocalVisualTheme
import com.companion.cc.ui.theme.TactileGesture
import com.companion.cc.ui.theme.rememberTactileAction
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmersiveDataManagementScreen(
    companionId: String,
    onNavigateBack: () -> Unit,
    viewModel: DataManagementViewModel = hiltViewModel()
) {
    val isProcessing by viewModel.isProcessing.collectAsState()
    val resultMessage by viewModel.resultMessage.collectAsState()
    val storageInfo by viewModel.storageInfo.collectAsState()
    val pendingCapsuleExport by viewModel.pendingCapsuleExport.collectAsState()
    val pendingDataExport by viewModel.pendingDataExport.collectAsState()
    val capsuleImportReport by viewModel.lastCapsuleImportReport.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }
    var showCapsulePrivacyDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val pendingExport = pendingCapsuleExport
        if (uri != null && pendingExport != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.writer(Charsets.UTF_8).use { writer -> writer.write(pendingExport.json) }
                } ?: error("无法打开目标文件")
            }.onSuccess {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("记忆胶囊已保存")
                }
            }.onFailure { error ->
                android.util.Log.e("DataManagementScreen", "写入文件失败", error)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("保存失败：${error.message}")
                }
            }
        }
        viewModel.consumePendingCapsuleExport()
    }

    LaunchedEffect(pendingCapsuleExport) {
        pendingCapsuleExport?.let { createDocumentLauncher.launch(it.fileName) }
    }

    val createDataDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val pendingExport = pendingDataExport
        if (uri != null && pendingExport != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.writer(Charsets.UTF_8).use { writer -> writer.write(pendingExport.json) }
                } ?: error("无法打开目标文件")
            }.onSuccess {
                coroutineScope.launch { snackbarHostState.showSnackbar("数据备份已保存") }
            }.onFailure { error ->
                android.util.Log.e("DataManagementScreen", "写入数据备份失败", error)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("保存失败：${error.message ?: "无法写入文件"}")
                }
            }
        }
        viewModel.consumePendingDataExport()
    }

    LaunchedEffect(pendingDataExport) {
        pendingDataExport?.let { createDataDocumentLauncher.launch(it.fileName) }
    }

    val dataFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            runCatching {
                val size = context.contentResolver.openAssetFileDescriptor(it, "r")?.use { descriptor ->
                    descriptor.length
                } ?: -1L
                require(size < 10L * 1024L * 1024L) { "文件过大，最大支持 10 MB" }
                context.contentResolver.openInputStream(it)?.use { input ->
                    input.reader(Charsets.UTF_8).use { reader -> reader.readText() }
                } ?: error("无法打开源文件")
            }.onSuccess(viewModel::importData)
                .onFailure { error ->
                    android.util.Log.e("DataManagementScreen", "读取数据备份失败", error)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("读取失败：${error.message ?: "文件无效"}")
                    }
                }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            runCatching {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                val size = context.contentResolver.openAssetFileDescriptor(it, "r")?.use { descriptor ->
                    descriptor.length
                } ?: -1L
                require(size < 10L * 1024L * 1024L) { "文件过大，最大支持 10 MB" }
                context.contentResolver.openInputStream(it)?.use { input ->
                    input.reader(Charsets.UTF_8).use { reader -> reader.readText() }
                } ?: error("无法打开源文件")
            }.onSuccess { jsonData ->
                viewModel.importMemoryCapsule(jsonData, companionId)
            }.onFailure { error ->
                android.util.Log.e("DataManagementScreen", "读取文件失败", error)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("读取失败：${error.message ?: "文件无效"}")
                }
            }
        }
    }

    LaunchedEffect(resultMessage) {
        resultMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearResultMessage()
        }
    }

    val visualTheme = LocalVisualTheme.current
    val accentColor = visualTheme.tokens.accent
    val requestDataExport = rememberTactileAction(enabled = !isProcessing) {
        viewModel.exportData()
    }
    val importData = rememberTactileAction(enabled = !isProcessing) {
        dataFilePickerLauncher.launch(arrayOf("application/json"))
    }
    val requestCapsuleExport = rememberTactileAction(enabled = !isProcessing) {
        showCapsulePrivacyDialog = true
    }
    val importCapsule = rememberTactileAction(enabled = !isProcessing) {
        filePickerLauncher.launch(arrayOf("application/json"))
    }
    val requestClearMessages = rememberTactileAction(enabled = !isProcessing) {
        showClearDialog = true
    }
    val resetSettings = rememberTactileAction(enabled = !isProcessing) {
        viewModel.resetSettings()
    }
    val confirmCapsuleExport = rememberTactileAction {
        showCapsulePrivacyDialog = false
        viewModel.exportMemoryCapsule(companionId)
    }
    val confirmClearMessages = rememberTactileAction(gesture = TactileGesture.DESTRUCTIVE_CONFIRM) {
        viewModel.clearAllMessages()
        showClearDialog = false
    }

    Scaffold(
        modifier = Modifier.auroraScreenBackground(LocalVisualTheme.current.tokens.backdrop.isDark),
        topBar = {
            SceneTopBar(
                title = { Text("数据管理", fontWeight = FontWeight.Bold) },
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SceneSection(title = "存储概览", icon = Icons.Default.Storage) {
                AdaptiveMetricGrid(
                    items = listOf(
                        MetricGridItem("数据库", storageInfo.databaseSize, Icons.Default.Storage),
                        MetricGridItem("消息", storageInfo.messageCount, Icons.Default.Message),
                        MetricGridItem("记忆", storageInfo.memoryCount, Icons.Default.Psychology),
                        MetricGridItem("缓存", storageInfo.cacheSize, Icons.Default.FolderOpen)
                    )
                )
            }

            SceneSection(title = "数据传输", icon = Icons.Default.SwapHoriz) {
                Text(
                    text = "数据备份包含当前账户的对话与连接设置；记忆胶囊仅处理当前角色的结构化记忆。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                SceneAction(
                    title = "导出数据备份",
                    description = "将当前账户的对话与连接设置保存为 JSON",
                    icon = Icons.Default.FileDownload,
                    onClick = requestDataExport,
                    enabled = !isProcessing,
                    tone = SceneActionTone.Primary
                )
                Divider()
                SceneAction(
                    title = "导入数据备份",
                    description = "从 JSON 恢复当前账户的数据",
                    icon = Icons.Default.FileUpload,
                    onClick = importData,
                    enabled = !isProcessing
                )
                Divider()
                SceneAction(
                    title = "导出记忆胶囊",
                    description = "导出当前角色的结构化记忆",
                    icon = Icons.Default.FileDownload,
                    onClick = requestCapsuleExport,
                    enabled = !isProcessing
                )
                Divider()
                SceneAction(
                    title = "导入记忆胶囊",
                    description = "导入当前角色的结构化记忆",
                    icon = Icons.Default.FileUpload,
                    onClick = importCapsule,
                    enabled = !isProcessing
                )
            }

            capsuleImportReport?.let { report ->
                SceneSection(title = "最近一次导入报告", icon = Icons.Default.Assignment) {
                    ImportReportItem("新增来源", report.insertedSources)
                    ImportReportItem("新增节点", report.insertedNodes)
                    ImportReportItem("新增证据", report.insertedEvidence)
                    ImportReportItem("新增审核", report.insertedReviews)
                    ImportReportItem("新增版本", report.insertedVersions)
                    ImportReportItem("新增关系", report.insertedRelations)
                    ImportReportItem("新增检索记录", report.insertedTraces)
                    ImportReportItem("新增反馈", report.insertedFeedback)
                    ImportReportItem("跳过重复记录", report.skippedRecords)
                }
            }

            SceneSection(
                title = "危险操作",
                icon = Icons.Default.Warning,
                tone = SceneSectionTone.Destructive
            ) {
                Text("清除全部对话记录，此操作不可撤销。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                SceneAction(
                    title = "清除全部对话",
                    description = "永久删除当前账户的所有对话记录",
                    icon = Icons.Default.DeleteForever,
                    onClick = requestClearMessages,
                    enabled = !isProcessing,
                    tone = SceneActionTone.Destructive
                )
            }

            SceneSection(title = "偏好设置", icon = Icons.Default.Settings) {
                SceneAction(
                    title = "重置设置",
                    description = "恢复应用的默认连接与显示设置",
                    icon = Icons.Default.RestartAlt,
                    onClick = resetSettings,
                    enabled = !isProcessing
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showCapsulePrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showCapsulePrivacyDialog = false },
                icon = { Icon(Icons.Default.Security, contentDescription = null) },
                title = { Text("导出记忆胶囊") },
                text = {
                    Text("记忆胶囊包含当前角色作用域的结构化记忆、证据和检索记录。请只保存到你信任的位置，不要分享给他人。")
                },
                confirmButton = {
                    TextButton(onClick = confirmCapsuleExport) { Text("继续导出") }
                },
                dismissButton = {
                    TextButton(onClick = { showCapsulePrivacyDialog = false }) { Text("取消") }
                }
            )
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("确认清除") },
                text = { Text("确定要清除所有对话记录吗？此操作不可撤销！") },
                confirmButton = {
                    TextButton(
                        onClick = confirmClearMessages,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("清除")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }

        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(visualTheme.tokens.glass.scrim),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = accentColor)
                        Text("处理中...")
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportReportItem(label: String, value: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.toString(), fontWeight = FontWeight.Bold)
    }
}
