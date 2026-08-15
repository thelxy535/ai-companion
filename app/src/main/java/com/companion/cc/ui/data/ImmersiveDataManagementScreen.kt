package com.companion.cc.ui.data

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 沉浸式数据管理页面 - 保留所有功能，Material 3 Expressive 美化
 */
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
    val snackbarHostState = remember { SnackbarHostState() }
    var showClearDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // 文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val jsonData = reader.readText()
                reader.close()
                viewModel.importData(jsonData)
            } catch (e: Exception) {
                android.util.Log.e("DataManagementScreen", "读取文件失败", e)
            }
        }
    }

    // 显示结果消息
    LaunchedEffect(resultMessage) {
        resultMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearResultMessage()
        }
    }

    val backgroundColor = getBackgroundForCompanion(companionId)
    val accentColor = getAccentColor(companionId)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = backgroundColor)
                .padding(padding)
        ) {
            // 顶部栏
            TopAppBar(
                title = {
                    Text(
                        text = "数据管理",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 存储信息卡片
                StorageInfoCard(storageInfo, accentColor)

                // 清除数据卡片
                ClearDataCard(
                    isProcessing = isProcessing,
                    accentColor = accentColor,
                    onClearClick = { showClearDialog = true }
                )

                // 导出数据卡片
                ExportDataCard(
                    isProcessing = isProcessing,
                    accentColor = accentColor,
                    onExportClick = { viewModel.exportData("default", companionId) }
                )

                // 导入数据卡片
                ImportDataCard(
                    isProcessing = isProcessing,
                    accentColor = accentColor,
                    onImportClick = {
                        filePickerLauncher.launch(arrayOf("application/json"))
                    }
                )

                // 重置设置卡片
                ResetSettingsCard(
                    isProcessing = isProcessing,
                    accentColor = accentColor,
                    onResetClick = { viewModel.resetSettings() }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 清除确认对话框
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFF44336)
                    )
                },
                title = { Text("确认清除") },
                text = { Text("确定要清除所有对话记录吗？此操作不可撤销！") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearAllMessages("default")
                            showClearDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFFF44336)
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

        // 加载指示器
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp)
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

/**
 * 存储信息卡片
 */
@Composable
private fun StorageInfoCard(storageInfo: StorageInfo, accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "存储信息",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            StorageInfoItem("数据库大小", storageInfo.databaseSize, Icons.Default.Storage, accentColor)
            StorageInfoItem("消息数量", storageInfo.messageCount, Icons.Default.Message, accentColor)
            StorageInfoItem("记忆数量", storageInfo.memoryCount, Icons.Default.Psychology, accentColor)
            StorageInfoItem("缓存大小", storageInfo.cacheSize, Icons.Default.FolderOpen, accentColor)
        }
    }
}

@Composable
private fun StorageInfoItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 清除数据卡片
 */
@Composable
private fun ClearDataCard(
    isProcessing: Boolean,
    accentColor: Color,
    onClearClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFF44336)
                )
                Text(
                    text = "清除数据",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "清除所有对话记录。此操作不可撤销，请谨慎操作。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onClearClick,
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("清除所有对话")
            }
        }
    }
}

/**
 * 导出数据卡片
 */
@Composable
private fun ExportDataCard(
    isProcessing: Boolean,
    accentColor: Color,
    onExportClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Upload,
                    contentDescription = null,
                    tint = accentColor
                )
                Text(
                    text = "导出数据",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "将所有对话记录导出为 JSON 文件，方便备份和迁移。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onExportClick,
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("导出数据")
            }
        }
    }
}

/**
 * 导入数据卡片
 */
@Composable
private fun ImportDataCard(
    isProcessing: Boolean,
    accentColor: Color,
    onImportClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = accentColor
                )
                Text(
                    text = "导入数据",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "从 JSON 文件导入之前备份的数据。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onImportClick,
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = accentColor
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.linearGradient(listOf(accentColor, accentColor))
                )
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("选择文件导入")
            }
        }
    }
}

/**
 * 重置设置卡片
 */
@Composable
private fun ResetSettingsCard(
    isProcessing: Boolean,
    accentColor: Color,
    onResetClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    tint = Color(0xFFFF9800)
                )
                Text(
                    text = "重置设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "将所有设置恢复为默认值。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onResetClick,
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFFF9800)
                )
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("重置设置")
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
