package com.companion.cc.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.domain.model.AIProvider
import com.companion.cc.ui.theme.FunctionalColors

/**
 * Settings 界面 - 完全按照设计文档重构
 * 参考: Now in Android, Material 3, VS Code
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    // 状态收集
    val apiKey by viewModel.apiKey.collectAsState()
    val baseUrl by viewModel.baseUrl.collectAsState()
    val model by viewModel.model.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val currentProvider by viewModel.currentProvider.collectAsState()
    val isValidating by viewModel.isValidating.collectAsState()
    val validationMessage by viewModel.validationMessage.collectAsState()
    val visionApiKeySaved by viewModel.visionApiKeySaved.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val userAvatar by viewModel.userAvatar.collectAsState()
    val xiaoChanAvatar by viewModel.xiaoChanAvatar.collectAsState()
    val museAvatar by viewModel.museAvatar.collectAsState()

    var apiKeyInput by remember { mutableStateOf(apiKey ?: "") }
    var baseUrlInput by remember { mutableStateOf(baseUrl) }
    var visionApiKeyInput by remember { mutableStateOf("") }  // 视觉 API Key 输入
    var showUserAvatarDialog by remember { mutableStateOf(false) }
    var showXiaoChanAvatarDialog by remember { mutableStateOf(false) }
    var showMuseAvatarDialog by remember { mutableStateOf(false) }

    // 同步状态
    LaunchedEffect(apiKey) {
        apiKeyInput = apiKey ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "设置",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. 状态卡片
            item {
                ProviderStatusCard(
                    provider = currentProvider,
                    modelCount = availableModels.size,
                    selectedModel = model,
                    isConnected = currentProvider != null,
                    onTestConnection = { viewModel.testConnection() }
                )
            }

            // 2. API 配置区
            item {
                ApiConfigSection(
                    apiKey = apiKeyInput,
                    onApiKeyChange = { apiKeyInput = it },
                    isValidating = isValidating,
                    validationMessage = validationMessage,
                    currentProvider = currentProvider,
                    modelCount = availableModels.size,
                    onValidate = {
                        viewModel.validateAndConfigureApiKey(apiKeyInput, null)
                    }
                )
            }

            // 2.5. 视觉 API 配置区（图片理解专用）
            item {
                VisionApiConfigSection(
                    visionApiKey = visionApiKeyInput,
                    onVisionApiKeyChange = { visionApiKeyInput = it },
                    onSave = {
                        viewModel.saveVisionApiKey(visionApiKeyInput)
                    },
                    isSaved = visionApiKeySaved
                )
            }

            // 3. 模型选择区
            if (availableModels.isNotEmpty()) {
                item {
                    ModelSelectionSection(
                        selectedModel = model ?: availableModels.firstOrNull() ?: "",
                        availableModels = availableModels,
                        providerName = currentProvider?.displayName,
                        onModelChange = { viewModel.switchModel(it) }
                    )
                }
            }

            // 3.5. 主题设置
            item {
                ThemeSettingsSection(
                    currentTheme = themeMode,
                    onThemeChange = { viewModel.saveThemeMode(it) }
                )
            }

            // 3.6. 字体大小设置
            item {
                FontSizeSettingsSection(
                    currentSize = fontSize,
                    onSizeChange = { viewModel.saveFontSize(it) }
                )
            }

            // 3.7. 头像设置
            item {
                Text(
                    text = "头像设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                AvatarSettingItem(
                    title = "我的头像",
                    avatarUrl = userAvatar,
                    onClick = { showUserAvatarDialog = true }
                )
            }

            item {
                AvatarSettingItem(
                    title = "小璨的头像",
                    avatarUrl = xiaoChanAvatar,
                    defaultEmoji = "💕",
                    onClick = { showXiaoChanAvatarDialog = true }
                )
            }

            item {
                AvatarSettingItem(
                    title = "缪斯的头像",
                    avatarUrl = museAvatar,
                    defaultEmoji = "🎭",
                    onClick = { showMuseAvatarDialog = true }
                )
            }

            // 4. 高级设置
            item {
                AdvancedSettingsSection(
                    baseUrl = baseUrlInput,
                    onBaseUrlChange = { baseUrlInput = it }
                )
            }

            // 5. 支持的供应商
            item {
                SupportedProvidersCard()
            }

            // 底部间距
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 头像设置对话框
        if (showUserAvatarDialog) {
            AvatarSettingsDialog(
                currentAvatarUrl = userAvatar,
                title = "设置我的头像",
                onDismiss = { showUserAvatarDialog = false },
                onAvatarSelected = { uri ->
                    viewModel.saveUserAvatar(uri.toString())
                    showUserAvatarDialog = false
                },
                onClearAvatar = {
                    viewModel.saveUserAvatar(null)
                }
            )
        }

        if (showXiaoChanAvatarDialog) {
            AvatarSettingsDialog(
                currentAvatarUrl = xiaoChanAvatar,
                title = "设置小璨的头像",
                onDismiss = { showXiaoChanAvatarDialog = false },
                onAvatarSelected = { uri ->
                    viewModel.saveCompanionAvatar("xiaocan", uri.toString())
                    showXiaoChanAvatarDialog = false
                },
                onClearAvatar = {
                    viewModel.saveCompanionAvatar("xiaocan", null)
                }
            )
        }

        if (showMuseAvatarDialog) {
            AvatarSettingsDialog(
                currentAvatarUrl = museAvatar,
                title = "设置缪斯的头像",
                onDismiss = { showMuseAvatarDialog = false },
                onAvatarSelected = { uri ->
                    viewModel.saveCompanionAvatar("muse", uri.toString())
                    showMuseAvatarDialog = false
                },
                onClearAvatar = {
                    viewModel.saveCompanionAvatar("muse", null)
                }
            )
        }
    }
}

// ==================== 组件实现 ====================

@Composable
private fun ProviderStatusCard(
    provider: AIProvider?,
    modelCount: Int,
    selectedModel: String?,
    isConnected: Boolean,
    onTestConnection: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 状态指示
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isConnected)
                        Icons.Default.CheckCircle
                    else
                        Icons.Default.Info,
                    contentDescription = null,
                    tint = if (isConnected)
                        FunctionalColors.success
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = if (isConnected) "已连接" else "未配置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isConnected)
                        FunctionalColors.success
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (provider != null) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // 供应商信息
                InfoRow("供应商", provider.displayName)
                if (selectedModel != null) {
                    InfoRow("当前模型", selectedModel)
                }
                InfoRow("可用模型", "$modelCount 个")

                // 测试按钮
                OutlinedButton(
                    onClick = onTestConnection,
                    modifier = Modifier
                        .align(Alignment.End)
                        .height(36.dp)
                ) {
                    Text("测试连接", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 主题设置区
 */
@Composable
private fun ThemeSettingsSection(
    currentTheme: String,
    onThemeChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "外观设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 主题选项
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeOption(
                    icon = Icons.Default.Brightness4,
                    label = "跟随系统",
                    description = "根据系统设置自动切换",
                    isSelected = currentTheme == "system",
                    onClick = { onThemeChange("system") }
                )
                ThemeOption(
                    icon = Icons.Default.LightMode,
                    label = "明亮模式",
                    description = "始终使用明亮主题",
                    isSelected = currentTheme == "light",
                    onClick = { onThemeChange("light") }
                )
                ThemeOption(
                    icon = Icons.Default.DarkMode,
                    label = "暗黑模式",
                    description = "始终使用暗黑主题",
                    isSelected = currentTheme == "dark",
                    onClick = { onThemeChange("dark") }
                )
            }
        }
    }
}

@Composable
private fun ThemeOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surface,
        border = if (isSelected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "已选择",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ApiConfigSection(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    isValidating: Boolean,
    validationMessage: String?,
    currentProvider: AIProvider?,
    modelCount: Int,
    onValidate: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题
            Text(
                text = "API 配置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // API Key 输入
            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API Key") },
                placeholder = { Text("sk-xxxxxxxxxxxxxx") },
                leadingIcon = {
                    Icon(Icons.Default.Key, contentDescription = null)
                },
                singleLine = true,
                shape = MaterialTheme.shapes.small
            )

            // 帮助文字
            Text(
                text = "粘贴 API 密钥后会自动识别供应商",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 验证按钮
            Button(
                onClick = onValidate,
                modifier = Modifier.fillMaxWidth(),
                enabled = apiKey.isNotBlank() && !isValidating
            ) {
                if (isValidating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("验证中...")
                } else {
                    Text("验证并保存")
                }
            }

            // 验证结果
            if (validationMessage != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (currentProvider != null)
                            Icons.Default.CheckCircle
                        else
                            Icons.Default.Error,
                        contentDescription = null,
                        tint = if (currentProvider != null)
                            FunctionalColors.success
                        else
                            MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = validationMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (currentProvider != null)
                            FunctionalColors.success
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelSelectionSection(
    selectedModel: String,
    availableModels: List<String>,
    providerName: String?,
    onModelChange: (String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "模型选择",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedModel,
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    label = { Text("当前模型") },
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    shape = MaterialTheme.shapes.small
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableModels.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model) },
                            onClick = {
                                onModelChange(model)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Text(
                text = "共 ${availableModels.size} 个 ${providerName ?: ""} 模型可用",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedSettingsSection(
    baseUrl: String,
    onBaseUrlChange: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "高级设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (isExpanded)
                        Icons.Default.ExpandLess
                    else
                        Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "收起" else "展开"
                )
            }

            // 可折叠内容
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Divider()

                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = onBaseUrlChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Base URL") },
                        placeholder = { Text("https://api.example.com/v1") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small
                    )

                    Text(
                        text = "仅在使用自定义 API 端点时修改",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportedProvidersCard() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "支持的 AI 供应商",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "SiliconFlow",
                    "OpenAI",
                    "智谱 AI (Zhipu)",
                    "DeepSeek",
                    "月之暗面 (Kimi)",
                    "阿里云百炼",
                    "自定义"
                ).forEach { provider ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = provider,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "系统会自动识别您的 API 密钥类型",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 视觉 API 配置区（图片理解专用）
 */
@Composable
private fun VisionApiConfigSection(
    visionApiKey: String,
    onVisionApiKeyChange: (String) -> Unit,
    onSave: () -> Unit,
    isSaved: Boolean = false
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "图片理解配置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // 说明文字
            Text(
                text = "配置 Gemini API Key 以启用图片理解功能（免费额度 1500次/天）",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Gemini API Key 输入
            OutlinedTextField(
                value = visionApiKey,
                onValueChange = onVisionApiKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Gemini API Key") },
                placeholder = { Text("粘贴您的 Gemini API Key") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null
                    )
                },
                singleLine = true,
                shape = MaterialTheme.shapes.small
            )

            // 获取 API Key 引导
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "访问 aistudio.google.com 免费获取 API Key",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 保存成功提示
            AnimatedVisibility(
                visible = isSaved,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = FunctionalColors.success.copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = FunctionalColors.success,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "视觉 API Key 保存成功",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FunctionalColors.success
                    )
                }
            }

            // 保存按钮
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = visionApiKey.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("保存视觉 API Key")
            }
        }
    }
}

/**
 * 字体大小设置区
 */
@Composable
private fun FontSizeSettingsSection(
    currentSize: String,
    onSizeChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TextFields,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "字体大小",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 字体大小选项
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FontSizeOption(
                    label = "小",
                    description = "紧凑显示，适合小屏幕",
                    isSelected = currentSize == "small",
                    onClick = { onSizeChange("small") }
                )
                FontSizeOption(
                    label = "中（推荐）",
                    description = "默认大小，平衡舒适",
                    isSelected = currentSize == "medium",
                    onClick = { onSizeChange("medium") }
                )
                FontSizeOption(
                    label = "大",
                    description = "易于阅读",
                    isSelected = currentSize == "large",
                    onClick = { onSizeChange("large") }
                )
                FontSizeOption(
                    label = "超大",
                    description = "最大字体，视力辅助",
                    isSelected = currentSize == "xlarge",
                    onClick = { onSizeChange("xlarge") }
                )
            }
        }
    }
}

@Composable
private fun FontSizeOption(
    label: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surface,
        border = if (isSelected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "已选择",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
