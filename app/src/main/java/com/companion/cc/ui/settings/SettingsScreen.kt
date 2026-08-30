package com.companion.cc.ui.settings

import com.companion.cc.ui.designsystem.auroraScreenBackground
import com.companion.cc.ui.theme.LocalVisualTheme
import com.companion.cc.ui.theme.LocalFontScale
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
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
import com.companion.cc.util.NotificationHelper
import com.companion.cc.util.NotificationPermissionPolicy
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.ChevronRight
import com.companion.cc.ui.designsystem.pressableV5
import com.companion.cc.ui.designsystem.smoothCorner
import com.companion.cc.ui.designsystem.materialSurface
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val apiKey by viewModel.apiKey.collectAsState()
    val notificationsOn by viewModel.notificationsEnabled.collectAsState(initial = true)
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
    val materialStyle by viewModel.materialStyle.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val tactileIntensity by viewModel.tactileIntensity.collectAsState()
    val visualCustomization by viewModel.visualCustomization.collectAsState()
    val visualCustomizationMessage by viewModel.visualCustomizationMessage.collectAsState()
    val userAvatar by viewModel.userAvatar.collectAsState()
    val xiaoChanAvatar by viewModel.xiaoChanAvatar.collectAsState()
    val museAvatar by viewModel.museAvatar.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    // V9PM：触感通路（色点反馈/Switch 拨动共用）
    val tactilePref = com.companion.cc.ui.theme.LocalTactileIntensityPreference.current
    val effectTier = LocalVisualTheme.current.effectTier
    val tactile = remember(context) { com.companion.cc.ui.theme.TactileFeedbackController(context) }
    var notificationPermissionGranted by remember {
        mutableStateOf(NotificationHelper.canPostNotifications(context))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionGranted = granted
    }

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
        modifier = Modifier.auroraScreenBackground(LocalVisualTheme.current.tokens.backdrop.isDark),
        containerColor = Color.Transparent
    ) { padding ->
        // 单开手风琴：点行展开对应功能区
        var expandedSection by remember { mutableStateOf<String?>(null) }
        val listState = rememberLazyListState()
        // V9PM 修复3：展开分组后自动滚动定位
        LaunchedEffect(expandedSection) {
            when (expandedSection) {
                "api", "model", "vision" -> listState.animateScrollToItem(2)
                "material", "accent", "font", "tactile" -> listState.animateScrollToItem(4)
                "avatar" -> listState.animateScrollToItem(6)
                else -> {}
            }
        }
        val night = LocalVisualTheme.current.tokens.backdrop.isDark
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 50.dp, bottom = 16.dp)
        ) {
            // V7 view-head（根 Tab 无返回键）
            item {
                Text("设置", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text("全局配置 · 所有自定义都会自动过可读性门禁", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { Spacer(Modifier.height(12.dp)) }

            // ── 服务 ──（展开内容嵌在对应行正下方）
            item { V7GroupLabel("服务") }
            item {
                V7SGroupCard(night, materialStyle) {
                    V7SRow(
                        title = "API 与连接",
                        subtitle = currentProvider?.displayName?.let { "已配置 · $it" } ?: "未配置 · 粘贴 Key 即可",
                        expanded = expandedSection == "api",
                        night = night,
                        onClick = { expandedSection = if (expandedSection == "api") null else "api" }
                    )
                    if (expandedSection == "api") {
                        ProviderStatusSection(
                            provider = currentProvider,
                            modelCount = availableModels.size,
                            selectedModel = model,
                            onTestConnection = viewModel::testConnection
                        )
                        ApiConfigSection(
                            apiKey = apiKeyInput,
                            onApiKeyChange = { apiKeyInput = it },
                            isValidating = isValidating,
                            validationMessage = validationMessage,
                            currentProvider = currentProvider,
                            onValidate = { viewModel.validateAndConfigureApiKey(apiKeyInput, null) }
                        )
                        AdvancedSettingsSection(baseUrlInput) { baseUrlInput = it }
                    }
                    V7SRow(
                        title = "模型",
                        subtitle = model?.takeIf { it.isNotBlank() } ?: "验证 Key 后自动获取",
                        expanded = expandedSection == "model",
                        night = night,
                        onClick = { expandedSection = if (expandedSection == "model") null else "model" }
                    )
                    if (expandedSection == "model" && availableModels.isNotEmpty()) {
                        ModelSelectionSection(
                            selectedModel = model,
                            availableModels = availableModels,
                            providerName = currentProvider?.displayName,
                            onModelChange = viewModel::switchModel
                        )
                    }
                    V7SRow(
                        title = "图片理解",
                        subtitle = if (isSelfHostedVisionPaired) "自托管 · 已配对" else if (visionApiKeySaved) "云端 · 已配置" else "未配置",
                        expanded = expandedSection == "vision",
                        night = night,
                        onClick = { expandedSection = if (expandedSection == "vision") null else "vision" }
                    )
                    if (expandedSection == "vision") {
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
                }
            }

            // ── 外观 ──
            item { V7GroupLabel("外观") }
            item {
                V7SGroupCard(night, materialStyle) {
                    V7SRow(
                        title = "材质风格",
                        subtitle = when (materialStyle) {
                            "MATTE" -> "雾面 · 无模糊"
                            "LIQUID" -> "液态 · 流动感"
                            "FABRIC" -> "织物 · 纹理"
                            "SANDBLASTED" -> "磨砂 · 颗粒"
                            else -> "玻璃 · 实时模糊"
                        },
                        expanded = expandedSection == "material",
                        night = night,
                        onClick = { expandedSection = if (expandedSection == "material") null else "material" }
                    )
                    if (expandedSection == "material") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            listOf("GLASS", "MATTE", "LIQUID", "FABRIC", "SANDBLASTED").forEach { style ->
                                val label = when (style) {
                                    "GLASS" -> "玻璃"
                                    "MATTE" -> "雾面"
                                    "LIQUID" -> "液态"
                                    "FABRIC" -> "织物"
                                    else -> "磨砂"
                                }
                                val selected = materialStyle == style
                                OutlinedButton(
                                    onClick = { viewModel.saveMaterialStyle(style) },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent
                                    ),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                        Text(
                            "切换后全局生效：Dock、记忆磁贴、玻璃卡片都会跟随变换",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                        )
                    }
                    V7SRow(
                        title = "强调色",
                        subtitle = "主题强调色（按钮/选中态/链接）",
                        expanded = expandedSection == "accent",
                        night = night,
                        onClick = { expandedSection = if (expandedSection == "accent") null else "accent" }
                    )
                    if (expandedSection == "accent") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            val accentChoices = listOf(
                                "默认蓝" to null,
                                "绿" to "#2EA985",
                                "粉" to "#C97BA8",
                                "琥珀" to "#D69A4F",
                                "紫" to "#8B7FD6"
                            )
                            accentChoices.forEach { (label, hex) ->
                                val c = when (hex) {
                                    "#2EA985" -> Color(0xFF2EA985)
                                    "#C97BA8" -> Color(0xFFC97BA8)
                                    "#D69A4F" -> Color(0xFFD69A4F)
                                    "#8B7FD6" -> Color(0xFF8B7FD6)
                                    else -> Color(0xFF4A7CE8)
                                }
                                val selected = visualCustomization.accentHex == hex
                                // V9PM 修复4：色点按压反馈（scale 0.88）+ 触感
                                val dotInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                val dotPressed by dotInteraction.collectIsPressedAsState()
                                val dotScale by animateFloatAsState(if (dotPressed) 0.88f else 1f, spring(dampingRatio = 0.6f), label = "accentDot")
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .scale(dotScale)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(c)
                                        .border(
                                            2.dp,
                                            if (selected) MaterialTheme.colorScheme.onSurface else androidx.compose.ui.graphics.Color.Transparent,
                                            androidx.compose.foundation.shape.CircleShape
                                        )
                                        .clickable(
                                            interactionSource = dotInteraction,
                                            indication = null
                                        ) {
                                            tactile.perform(com.companion.cc.ui.theme.TactileGesture.CLICK, tactilePref, effectTier)
                                            viewModel.saveVisualAccent(hex)
                                        }
                                )
                            }
                        }
                    }
                    V7SRow(
                        title = "字体大小",
                        subtitle = when (fontSize) {
                            "small" -> "小 · 紧凑显示"
                            "large" -> "大 · 更易阅读"
                            "xlarge" -> "超大 · 视力辅助"
                            else -> "中（推荐）"
                        },
                        expanded = expandedSection == "font",
                        night = night,
                        onClick = { expandedSection = if (expandedSection == "font") null else "font" }
                    )
                    if (expandedSection == "font") {
                        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            listOf(
                                "小" to "small",
                                "中（推荐）" to "medium",
                                "大" to "large",
                                "超大" to "xlarge"
                            ).forEach { (label, value) ->
                                val selected = fontSize == value
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { viewModel.saveFontSize(value) }
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(label, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f))
                                    if (selected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                    V7SRow(
                        title = "触感强度",
                        subtitle = when (tactileIntensity) {
                            com.companion.cc.ui.theme.TactileIntensityPreference.LIGHT -> "轻 · 若隐若现"
                            com.companion.cc.ui.theme.TactileIntensityPreference.MEDIUM -> "中 · 平衡"
                            com.companion.cc.ui.theme.TactileIntensityPreference.STRONG -> "强 · 明确反馈"
                            com.companion.cc.ui.theme.TactileIntensityPreference.OFF -> "已关闭"
                            else -> "跟随系统"
                        },
                        expanded = expandedSection == "tactile",
                        night = night,
                        onClick = { expandedSection = if (expandedSection == "tactile") null else "tactile" }
                    )
                    if (expandedSection == "tactile") {
                        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            listOf(
                                "跟随系统" to com.companion.cc.ui.theme.TactileIntensityPreference.SYSTEM,
                                "轻" to com.companion.cc.ui.theme.TactileIntensityPreference.LIGHT,
                                "中" to com.companion.cc.ui.theme.TactileIntensityPreference.MEDIUM,
                                "强" to com.companion.cc.ui.theme.TactileIntensityPreference.STRONG,
                                "关闭" to com.companion.cc.ui.theme.TactileIntensityPreference.OFF
                            ).forEach { (label, value) ->
                                val selected = tactileIntensity == value
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { viewModel.saveTactileIntensity(value) }
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(label, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f))
                                    if (selected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // 材质选择行内嵌在"材质风格"行下方（放进同组卡片）

            // ── 通用 ──
            item { V7GroupLabel("通用") }
            item {
                V7SGroupCard(night, materialStyle) {
                    V7SRow(
                        title = "头像",
                        subtitle = "我的头像 · 伴侣形象",
                        expanded = expandedSection == "avatar",
                        night = night,
                        onClick = { expandedSection = if (expandedSection == "avatar") null else "avatar" }
                    )
                    if (expandedSection == "avatar") {
                        AvatarSettingItem("我的头像", userAvatar, onClick = { avatarDialog = AvatarTarget.USER })
                        UtilityDivider()
                        AvatarSettingItem("小璨的头像", xiaoChanAvatar, "💗") { avatarDialog = AvatarTarget.XIAO_CHAN }
                        UtilityDivider()
                        AvatarSettingItem("缪斯的头像", museAvatar, "🦌") { avatarDialog = AvatarTarget.MUSE }
                    }
                    // 通知行（设计稿带 Switch；授权失败/未授权时点 Switch 触发系统授权）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressableV5({
                                if (!notificationPermissionGranted) {
                                    // 未授权：行点击触发系统授权
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    // V9PM 修复6：已授权——行点击打开系统通知设置
                                    context.startActivity(
                                        android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                            .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    )
                                }
                            }, isNight = night)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("通知", fontSize = (14.5f * com.companion.cc.ui.theme.LocalFontScale.current).sp, fontWeight = FontWeight.Medium, lineHeight = (19f * com.companion.cc.ui.theme.LocalFontScale.current).sp, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                if (notificationPermissionGranted) "已开启 · 系统通知正常" else "需系统授权",
                                fontSize = (11.5f * com.companion.cc.ui.theme.LocalFontScale.current).sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = notificationsOn,
                            onCheckedChange = { want ->
                                // V9PM 修复5：拨动触感
                                tactile.perform(com.companion.cc.ui.theme.TactileGesture.CLICK, tactilePref, effectTier)
                                viewModel.saveNotificationsEnabled(want)
                                if (want && !notificationPermissionGranted) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        )
                    }
                }
            }
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

@Composable
private fun NotificationPermissionSection(
    onRequestPermission: () -> Unit
) {
    UtilitySection(
        title = "通知权限",
        icon = Icons.Default.Notifications
    ) {
        Text(
            text = "开启通知后，应用才能发送聊天提醒和记忆回顾。"
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "你可以稍后在系统设置中修改此权限。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRequestPermission) {
            Text("允许通知")
        }
    }
}

private enum class AvatarTarget { USER, XIAO_CHAN, MUSE }


/**
 * V7 设置页：分组标签（10sp/600/字距加宽）
 */
@Composable
private fun V7GroupLabel(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

/**
 * V7 sgroup 卡片：20dp 圆角玻璃容器，行间 6dp
 */
@Composable
private fun V7SGroupCard(
    night: Boolean,
    materialStyle: String = "GLASS",
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val style = try {
        com.companion.cc.ui.designsystem.MaterialStyle.valueOf(materialStyle)
    } catch (e: IllegalArgumentException) {
        com.companion.cc.ui.designsystem.MaterialStyle.GLASS
    }
    val shape = smoothCorner(28.dp)   // V9PM 连续大圆角（原 20dp 圆角玻璃卡）
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .materialSurface(style, night, shape)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        content = content
    )
}

/**
 * V7 srow 行：16dp 圆角 + glass-t 底（无模糊）+ hairline 描边 + 右侧 chevron
 * 规格：标题 14.5sp/500/行高 19sp，副标题 11.5sp --ink-faint，内边距 13x14
 */
@Composable
private fun V7SRow(
    title: String,
    subtitle: String,
    expanded: Boolean,
    night: Boolean,
    showChevron: Boolean = true,
    showDivider: Boolean = true,
    onClick: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val inkFaint = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // V7 设计稿：srow 无独立圆角底，扁平嵌在 sgroup 卡内
                .pressableV5(onClick, isNight = night)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                // V9PM 字体覆盖：硬编码 sp × LocalFontScale
                val fontScale = com.companion.cc.ui.theme.LocalFontScale.current
                Text(title, fontSize = (14.5f * fontScale).sp, fontWeight = FontWeight.Medium, lineHeight = (19f * fontScale).sp, color = ink)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = (11.5f * fontScale).sp, color = inkFaint)
            }
            if (showChevron) {
                Spacer(Modifier.width(12.dp))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(if (expanded) 90f else 0f),
                    tint = inkFaint
                )
            }
        }
        // V7 hairline：行间分隔线（组内最后一行 showDivider=false）
        if (showDivider && !expanded) {
            Box(
                modifier = Modifier
                    .padding(start = 14.dp, end = 14.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(if (night) Color(0x14FFFFFF) else Color(0x0F1A2030))
            )
        }
    }
}
