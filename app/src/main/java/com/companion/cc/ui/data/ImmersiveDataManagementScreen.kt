package com.companion.cc.ui.data

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.ui.components.UtilityDivider
import com.companion.cc.ui.components.UtilitySection
import com.companion.cc.ui.theme.GlassSurface
import com.companion.cc.ui.theme.GlassDialogSurface
import com.companion.cc.ui.theme.LocalVisualTheme
import java.io.BufferedReader
import java.io.InputStreamReader

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

    LaunchedEffect(resultMessage) {
        resultMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearResultMessage()
        }
    }

    val visualTheme = LocalVisualTheme.current
    val accentColor = visualTheme.tokens.accent

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .padding(padding)
        ) {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp),
                useStrongFill = true
            ) {
                TopAppBar(
                    title = { Text("数据管理", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, "返回")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                UtilitySection(title = "存储概览", icon = Icons.Default.Storage) {
                    StorageInfoItem("数据库", storageInfo.databaseSize, Icons.Default.Storage, accentColor)
                    UtilityDivider()
                    StorageInfoItem("消息", storageInfo.messageCount, Icons.Default.Message, accentColor)
                    UtilityDivider()
                    StorageInfoItem("记忆", storageInfo.memoryCount, Icons.Default.Psychology, accentColor)
                    UtilityDivider()
                    StorageInfoItem("缓存", storageInfo.cacheSize, Icons.Default.FolderOpen, accentColor)
                }

                UtilitySection(title = "数据传输", icon = Icons.Default.SwapHoriz) {
                    Button(
                        onClick = { viewModel.exportData(companionId) },
                        enabled = !isProcessing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("导出数据")
                    }
                    UtilityDivider()
                    OutlinedButton(
                        onClick = { filePickerLauncher.launch(arrayOf("application/json")) },
                        enabled = !isProcessing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("导入数据")
                    }
                }

                UtilitySection(title = "危险操作", icon = Icons.Default.Warning) {
                    Text(
                        "清除全部对话记录，此操作不可撤销。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { showClearDialog = true },
                        enabled = !isProcessing,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("清除全部对话")
                    }
                }

                UtilitySection(title = "偏好设置", icon = Icons.Default.Settings) {
                    OutlinedButton(
                        onClick = { viewModel.resetSettings() },
                        enabled = !isProcessing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("重置设置")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
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
                        onClick = {
                            viewModel.clearAllMessages()
                            showClearDialog = false
                        },
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
                GlassDialogSurface(shape = RoundedCornerShape(16.dp)) {
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
