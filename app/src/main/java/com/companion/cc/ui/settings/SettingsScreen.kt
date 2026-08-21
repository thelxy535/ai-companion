package com.companion.cc.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.domain.model.BackdropTarget
import com.companion.cc.ui.components.UtilityDivider
import com.companion.cc.ui.components.UtilitySection
import com.companion.cc.ui.theme.GlassSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val apiKey by viewModel.apiKey.collectAsState()
    val baseUrl by viewModel.baseUrl.collectAsState()
    val model by viewModel.model.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val currentProvider by viewModel.currentProvider.collectAsState()
    val isValidating by viewModel.isValidating.collectAsState()
    val validationMessage by viewModel.validationMessage.collectAsState()
    val visionApiKeySaved by viewModel.visionApiKeySaved.collectAsState()
    val visionServiceMode by viewModel.visionServiceMode.collectAsState()
    val isSelfHostedVisionPaired by viewModel.isSelfHostedVisionPaired.collectAsState()
    val isPairingSelfHostedVision by viewModel.isPairingSelfHostedVision.collectAsState()
    val selfHostedVisionMessage by viewModel.selfHostedVisionMessage.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val visualCustomization by viewModel.visualCustomization.collectAsState()
    val visualCustomizationMessage by viewModel.visualCustomizationMessage.collectAsState()
    val userAvatar by viewModel.userAvatar.collectAsState()
    val xiaoChanAvatar by viewModel.xiaoChanAvatar.collectAsState()
    val museAvatar by viewModel.museAvatar.collectAsState()

    var apiKeyInput by remember { mutableStateOf(apiKey.orEmpty()) }
    var baseUrlInput by remember { mutableStateOf(baseUrl) }
    var visionApiKeyInput by remember { mutableStateOf("") }
    var selfHostedPairingCode by remember { mutableStateOf("") }
    var backdropPickerTarget by remember { mutableStateOf<BackdropTarget?>(null) }
    var avatarDialog by remember { mutableStateOf<AvatarTarget?>(null) }

    LaunchedEffect(apiKey) { apiKeyInput = apiKey.orEmpty() }
    LaunchedEffect(baseUrl) { baseUrlInput = baseUrl }

    val backdropPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val target = backdropPickerTarget
        if (uri != null && target != null) viewModel.selectVisualBackdrop(target, uri)
        backdropPickerTarget = null
    }

    Scaffold(
        topBar = { SettingsTopBar(onNavigateBack) },
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ProviderStatusSection(
                    provider = currentProvider,
                    modelCount = availableModels.size,
                    selectedModel = model,
                    onTestConnection = viewModel::testConnection
                )
            }
            item {
                ApiConfigSection(
                    apiKey = apiKeyInput,
                    onApiKeyChange = { apiKeyInput = it },
                    isValidating = isValidating,
                    validationMessage = validationMessage,
                    currentProvider = currentProvider,
                    onValidate = { viewModel.validateAndConfigureApiKey(apiKeyInput, null) }
                )
            }
            if (availableModels.isNotEmpty()) {
                item {
                    ModelSelectionSection(
                        selectedModel = model,
                        availableModels = availableModels,
                        providerName = currentProvider?.displayName,
                        onModelChange = viewModel::switchModel
                    )
                }
            }
            item { SupportedProvidersSection() }
            item {
                VisionApiConfigSection(
                    mode = visionServiceMode,
                    isSelfHostedPaired = isSelfHostedVisionPaired,
                    pairingCode = selfHostedPairingCode,
                    onPairingCodeChange = { selfHostedPairingCode = it },
                    isPairing = isPairingSelfHostedVision,
                    message = selfHostedVisionMessage,
                    onModeChange = viewModel::selectVisionService,
                    onPair = { viewModel.pairSelfHostedVision(selfHostedPairingCode) },
                    onUnpair = viewModel::unpairSelfHostedVision,
                    visionApiKey = visionApiKeyInput,
                    onVisionApiKeyChange = { visionApiKeyInput = it },
                    onSaveGemini = { viewModel.saveVisionApiKey(visionApiKeyInput) },
                    isGeminiKeySaved = visionApiKeySaved
                )
            }
            item { ThemeSettingsSection(themeMode, viewModel::saveThemeMode) }
            item {
                VisualCustomizationEditor(
                    customization = visualCustomization,
                    message = visualCustomizationMessage,
                    onPickBackdrop = { target ->
                        backdropPickerTarget = target
                        backdropPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onClearBackdrop = viewModel::clearVisualBackdrop,
                    onSaveAccent = viewModel::saveVisualAccent,
                    onSaveEffectsPreference = viewModel::saveVisualEffectsPreference,
                    onSaveGlassOpacity = viewModel::saveGlassOpacity,
                    onReset = viewModel::resetVisualCustomization,
                    onSaveBackdropAppearance = viewModel::saveBackdropAppearance
                )
            }
            item { FontSizeSettingsSection(fontSize, viewModel::saveFontSize) }
            item {
                UtilitySection(title = "身份与头像", icon = Icons.Default.AccountCircle) {
                    AvatarSettingItem("我的头像", userAvatar, onClick = { avatarDialog = AvatarTarget.USER })
                    UtilityDivider()
                    AvatarSettingItem("小璨的头像", xiaoChanAvatar, "💕") { avatarDialog = AvatarTarget.XIAO_CHAN }
                    UtilityDivider()
                    AvatarSettingItem("缪斯的头像", museAvatar, "🎭") { avatarDialog = AvatarTarget.MUSE }
                }
            }
            item { AdvancedSettingsSection(baseUrlInput) { baseUrlInput = it } }
            item { Spacer(Modifier.height(16.dp)) }
        }

        avatarDialog?.let { target ->
            AvatarSettingsDialog(
                currentAvatarUrl = when (target) {
                    AvatarTarget.USER -> userAvatar
                    AvatarTarget.XIAO_CHAN -> xiaoChanAvatar
                    AvatarTarget.MUSE -> museAvatar
                },
                title = when (target) {
                    AvatarTarget.USER -> "设置我的头像"
                    AvatarTarget.XIAO_CHAN -> "设置小璨的头像"
                    AvatarTarget.MUSE -> "设置缪斯的头像"
                },
                onDismiss = { avatarDialog = null },
                onAvatarSelected = { uri ->
                    when (target) {
                        AvatarTarget.USER -> viewModel.saveUserAvatar(uri?.toString())
                        AvatarTarget.XIAO_CHAN -> viewModel.saveCompanionAvatar("xiaocan", uri?.toString())
                        AvatarTarget.MUSE -> viewModel.saveCompanionAvatar("muse", uri?.toString())
                    }
                    avatarDialog = null
                },
                onClearAvatar = {
                    when (target) {
                        AvatarTarget.USER -> viewModel.saveUserAvatar(null)
                        AvatarTarget.XIAO_CHAN -> viewModel.saveCompanionAvatar("xiaocan", null)
                        AvatarTarget.MUSE -> viewModel.saveCompanionAvatar("muse", null)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(onNavigateBack: () -> Unit) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        contentPadding = PaddingValues(0.dp),
        useStrongFill = true
    ) {
        TopAppBar(
            title = { Text("设置", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
    }
}

private enum class AvatarTarget { USER, XIAO_CHAN, MUSE }
