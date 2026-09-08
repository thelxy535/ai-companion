package com.companion.cc.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.companion.cc.ui.designsystem.AuroraDuration
import com.companion.cc.ui.designsystem.AuroraCurves
import com.companion.cc.ui.designsystem.smoothCorner
import com.companion.cc.ui.chat.components.EmotionalStatusBar
import com.companion.cc.ui.chat.components.EmoMiniBar
import com.companion.cc.ui.designsystem.AuroraDay
import com.companion.cc.ui.designsystem.AuroraNight
import com.companion.cc.ui.designsystem.AuroraChatTokens
import com.companion.cc.ui.designsystem.GlassTierV3
import com.companion.cc.ui.designsystem.auroraGlassV3
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.companion.cc.domain.model.Message
import com.companion.cc.domain.model.MessageRole
import com.companion.cc.domain.model.companions
import com.companion.cc.domain.message.MessageContentParser
import com.companion.cc.ui.chat.components.EmotionalStatsDialog
import com.companion.cc.ui.chat.components.EmotionalTimelineDialog
import com.companion.cc.ui.chat.components.TTSSpeakingIndicator
import com.companion.cc.ui.chat.components.VoiceListeningIndicator
import com.companion.cc.ui.components.TagSelectionDialog
import com.companion.cc.ui.components.TagManagementDialog
import com.companion.cc.ui.components.V9PMDialogSurface
import com.companion.cc.ui.components.V9PMIconButton
import com.companion.cc.ui.components.CompanionAvatar
import com.companion.cc.ui.components.UserAvatar
import com.companion.cc.ui.settings.AvatarSettingsDialog
import com.companion.cc.ui.theme.CompactGlassSurface
import com.companion.cc.ui.theme.GlassBottomDock
import com.companion.cc.ui.theme.GlassSurface
import com.companion.cc.ui.theme.LocalVisualTheme
import com.companion.cc.util.NetworkState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.material3.MaterialTheme

/**
 * 自然聊天界面
 *
 * - 消息气泡（对白 + 动作分离显示）
 * - 流式逐字动画（仅对正在流式传输的消息播放）
 * - 顶部动态状态（正在思考 / 正在回复 / 离线 / 最后活跃时间）
 * - 语音输入、错误重试、离线横幅
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun NaturalChatScreen(
    companionId: String,
    onNavigateBack: () -> Unit,
    onNavigateToMemory: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToData: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToCompanionDetail: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    // 初始化时传递角色信息到 ViewModel（单参数调用）
    LaunchedEffect(companionId) {
        viewModel.setCharacter(companionId)
    }

    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val networkState by viewModel.networkState.collectAsState()
    val usesLocalTestEndpoint by viewModel.usesLocalTestEndpoint.collectAsState()
    val latestStreamingMessageId by viewModel.latestStreamingMessageId.collectAsState()
    val activeStream by viewModel.activeStream.collectAsState()
    val sendState by viewModel.sendState.collectAsState()
    val customCharacterName by viewModel.customCharacterName.collectAsState()

    // 头像状态
    val userAvatar by viewModel.userAvatar.collectAsState()
    val companionAvatar by viewModel.companionAvatar.collectAsState()

    // 头像更换对话框状态
    var showUserAvatarDialog by remember { mutableStateOf(false) }
    var showCompanionAvatarDialog by remember { mutableStateOf(false) }

    // 是否离线
    val isOffline = networkState is NetworkState.Offline && !usesLocalTestEndpoint

    // 图片选择状态
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // 图片选择器（相册）
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    // 发送/回复状态是唯一的 typing 来源，避免依赖不会更新的时间戳变量
    val shouldShowTyping = sendState is ChatSendState.Sending || sendState is ChatSendState.Streaming
    val listState = rememberLazyListState()
    var lastMessageCount by remember(companionId) { mutableIntStateOf(0) }
    val latestMessageId = messages.lastOrNull()?.id
    LaunchedEffect(latestMessageId, activeStream?.sequence, shouldShowTyping) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(0)
            lastMessageCount = messages.size
        }
    }


    // 新功能状态
    // V9PM 状态本地化：emotionalState 由 EmoMiniBar 内部收集（重组局部化）
    // V9PM 状态本地化：conversationStats 由 EmotionalStatsDialog 内部收集
    val isListening by viewModel.isListening.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var inputText by remember { mutableStateOf("") }

    // 加载草稿
    LaunchedEffect(companionId) {
        inputText = viewModel.getDraft(companionId)
    }

    // 保存草稿（输入变化时自动保存）
    LaunchedEffect(inputText) {
        viewModel.saveDraft(companionId, inputText)
    }

    var selectedMessage by remember { mutableStateOf<Message?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showStatsPanel by remember { mutableStateOf(false) }
    var showTimelineDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showTagManagementDialog by remember { mutableStateOf(false) }
    var timelineData by remember { mutableStateOf<List<com.companion.cc.ui.chat.components.EmotionalTimelinePoint>>(emptyList()) }
    val clipboardManager = LocalClipboardManager.current

    val companion = remember(companionId) {
        companions.find { it.id == companionId } ?: companions[0]
    }

    // 消息初始化由 ChatViewModel.setCharacter 统一负责；Room Flow 只做后续校正
    // 自动滚动到底部
    // V7 回底：新消息/首进都无动画直跳（animate 在长列表会“从上面滚下来”）
    // V7 键盘呼出/收起回底：WindowInsets 响应式读取
    // V9PM：退出前先收键盘（键盘开着直接 pop 会让 Dock 在转场期间跳动闪现）
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val exitScope = rememberCoroutineScope()
    val exitDensity = androidx.compose.ui.platform.LocalDensity.current
    val exitImeInsets = androidx.compose.foundation.layout.WindowInsets.ime
    var isExiting by remember { mutableStateOf(false) }
    var navBarFrozen by remember { mutableStateOf(0.dp) }
    val navBarPadNow = androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    androidx.compose.runtime.SideEffect { if (!isExiting) navBarFrozen = navBarPadNow }
    val exitFocus = androidx.compose.ui.platform.LocalFocusManager.current
    val exitChat = {
        if (!isExiting) {
            isExiting = true
            exitFocus.clearFocus() // 清残留焦点：输入框销毁时焦点释放会触发键盘短暂重启闪现
            if (exitImeInsets.getBottom(exitDensity) > 0) {
                keyboard?.hide()
                exitScope.launch {
                    kotlinx.coroutines.delay(160)
                    onNavigateBack()
                }
            } else {
                onNavigateBack()
            }
        }
    }
    // 系统返回手势/返回键与顶栏返回钮统一走同一退出保护路径
    androidx.activity.compose.BackHandler(enabled = !isExiting) { exitChat() }


    // 错误提示（可重试的错误带重试按钮）
    LaunchedEffect(error) {
        error?.let { chatError ->
            val result = snackbarHostState.showSnackbar(
                message = chatError.userMessage,
                actionLabel = if (chatError.canRetry) "重试" else "关闭",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed && chatError.canRetry) {
                viewModel.retryLastMessage()
            } else {
                viewModel.clearError()
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                ChatTopBar(
                    companion = companion,
                    customCharacterName = customCharacterName,
                    onNavigateBack = { exitChat() },
                    onMenuClick = { showMenu = true },
                    onAvatarLongPress = onNavigateToCompanionDetail,  // 长按名字打开角色详情页
                    isLoading = isLoading,
                    shouldShowTyping = shouldShowTyping,
                    isOffline = isOffline,
                    messages = messages
                )
                // V9PM emo-mini：六段彩条（点击展开情绪面板）
                EmoMiniBar(
                    stateFlow = viewModel.emotionalState,
                    onClick = {
                        scope.launch {
                            timelineData = viewModel.getEmotionalTimeline(companionId)
                            showTimelineDialog = true
                        }
                    }
                )
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .padding(bottom = navBarFrozen + if (isExiting) 0.dp else exitImeInsets.asPaddingValues().calculateBottomPadding())
            ) {
                // 语音输入指示器
                if (isListening) {
                    VoiceListeningIndicator()
                }

                // TTS 播放指示器
                if (isSpeaking) {
                    TTSSpeakingIndicator(
                        onStop = { viewModel.stopSpeaking() }
                    )
                }

                ChatInputBar(
                    message = inputText,
                    onMessageChange = { inputText = it },
                    onSend = {
                        // 根据是否有图片选择不同的发送方法
                        val imageUri = selectedImageUri
                        if (imageUri != null) {
                            // 发送带图片的消息
                            val accepted = viewModel.sendMessageWithImage(companionId, inputText, imageUri)
                            if (accepted) {
                                selectedImageUri = null
                                inputText = ""
                                viewModel.clearDraft(companionId)
                            }
                        } else if (inputText.isNotBlank()) {
                            // 发送纯文字消息
                            val accepted = viewModel.sendMessage(companionId, inputText)
                            if (accepted) {
                                inputText = ""
                                viewModel.clearDraft(companionId)
                            }
                        }
                    },
                    onImageClick = {
                        // 打开图片选择器
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onVoiceClick = {
                        if (isListening) {
                            viewModel.stopVoiceInput()
                        } else {
                            viewModel.startVoiceInput()
                        }
                    },
                    selectedImageUri = selectedImageUri,
                    onClearImage = { selectedImageUri = null },
                    isSending = isLoading,
                    showVoiceButton = true
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 离线提示横幅
                AnimatedVisibility(
                    visible = isOffline,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    CompactGlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudOff,
                                contentDescription = "离线",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "当前离线，仅可查看历史消息",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // 主聊天区域
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(padding)
                        .weight(1f)
                        // V9PM：点空白收键盘（点气泡/按钮由子元素消费，不冲突）
                        .pointerInput(Unit) {
                            detectTapGestures {
                                keyboard?.hide()
                                exitFocus.clearFocus()
                            }
                        },
                    contentPadding = PaddingValues(vertical = 8.dp),
                    reverseLayout = true, // V9PM：反转列表——首帧即底部，天然钉底零跳变（市面聊天标准架构）
                ) {
                    // V8 ② 连发气泡 60ms 递进（同回合同角色）；入场仅播新到消息
                    // 打字指示器（reverseLayout：首个 item=视口底部=最新消息下方）
                    if (shouldShowTyping) {
                        item {
                            TypingIndicator(companionEmoji = companion.emoji, companionAvatar = companionAvatar)
                        }
                    }
                        itemsIndexed(
                        // Room/缓存到达后直接显示历史快照，不再等待两帧
                        items = messages.asReversed(),
                        key = { _, m -> m.id }
                    ) { index, message ->
                        // 流式期间的占位气泡内容为空：跳过渲染，打字指示器已代表"正在回复"，
                        // 避免出现一闪而过的小气泡
                        val isStreamingPlaceholder = message.role == MessageRole.ASSISTANT &&
                            message.id == latestStreamingMessageId &&
                            message.content.isBlank()
                        if (isStreamingPlaceholder) {
                            return@itemsIndexed
                        }
                        // 历史快照不播放气泡入场；新消息由发送状态独立驱动
                        // 不在 Lazy item 内计算 burstIndex，避免反转列表与原序索引错位
                        // V9PM：相邻消息间隔 >2h 插入居中时间胶囊
                        val showTimeDivider =
                            index == messages.size - 1 ||
                                (message.timestamp - messages[messages.size - index - 2].timestamp) > 2L * 3_600_000L
                        Column {
                            if (showTimeDivider) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = formatTime(message.timestamp),
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(
                                                if (com.companion.cc.ui.theme.LocalVisualTheme.current.tokens.backdrop.isDark) Color.White.copy(alpha = 0.08f)
                                                else Color.White.copy(alpha = 0.5f)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            MessageBubble(
                                message = message,
                                companionEmoji = companion.emoji,
                                companionAvatar = companionAvatar,
                                userAvatar = userAvatar,
                                onLongPress = { selectedMessage = message },
                                shouldStream = message.id == latestStreamingMessageId,
                                streamText = activeStream?.takeIf { it.assistantMessageId == message.id }?.text,
                                // 历史消息不播入场；新消息由流式状态和列表结构事件驱动
                                entranceDelayMs = -1
                            )
                        }
                    }

                }
            }
// V9PM 加载模式重构：进页不再有任何阻塞式加载层——页面框架（顶栏/输入框/极光）常驻，
// 消息数据后台加载、到达即渐进填充。短暂空窗是暂态，不显示转圈（此前全屏白底 Loading 是"加载中闪一下"的元凶）。
// isInitialLoad 保留供埋点/调试。
        }
    }

    // 消息操作菜单
    selectedMessage?.let { message ->
        MessageActionsMenu(
            message = message,
            onDismiss = { selectedMessage = null },
            onCopy = {
                clipboardManager.setText(AnnotatedString(message.content))
                selectedMessage = null
                scope.launch {
                    snackbarHostState.showSnackbar("已复制到剪贴板")
                }
            },
            onDelete = {
                viewModel.deleteMessage(message.id)
                selectedMessage = null
                scope.launch {
                    snackbarHostState.showSnackbar("消息已删除")
                }
            },
            onMarkImportant = {
                viewModel.updateMessageImportance(message.id, 90)
                selectedMessage = null
                scope.launch {
                    snackbarHostState.showSnackbar("已标记为重要")
                }
            },
            onToggleFavorite = {
                viewModel.toggleMessageFavorite(message.id)
                selectedMessage = null
                val text = if (!message.isFavorited) "已添加到收藏" else "已取消收藏"
                scope.launch {
                    snackbarHostState.showSnackbar(text)
                }
            },
            onAddTag = {
                // 打开标签选择对话框
                showTagDialog = true
            },
            onShare = {
                // 分享消息功能
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, message.content)
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "来自 CC-Switch 的消息")
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "分享消息"))
                selectedMessage = null
            }
        )
    }

    // 功能菜单（V8 ⑧：scale .92→1 + fade 260ms bezier(.34,1.3,.5,1)，消失 160ms fade）
    if (showMenu) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopEnd
        ) {
            AnimatedVisibility(
                visible = showMenu,
                enter = scaleIn(initialScale = 0.92f, animationSpec = tween(AuroraDuration.BubbleIn, easing = AuroraCurves.BubbleEmphasized)) +
                    fadeIn(tween(AuroraDuration.BubbleIn, easing = AuroraCurves.BubbleEmphasized)),
                exit = fadeOut(tween(160)),
            ) {
                FunctionMenu(
                    onDismiss = { showMenu = false },
                    onMemoryClick = {
                        showMenu = false
                        onNavigateToMemory()
                    },
                    onStatsClick = {
                        showMenu = false
                        onNavigateToStats()
                    },
                    onDataClick = {
                        showMenu = false
                        onNavigateToData()
                    },
                    onFavoritesClick = {
                        showMenu = false
                        onNavigateToFavorites()
                    }
                )
            }
        }
    }

    // 情感状态面板（弹出式）
    if (showStatsPanel) {
        EmotionalStatsDialog(
            emotionalStateFlow = viewModel.emotionalState,
            conversationStatsFlow = viewModel.conversationStats,
            onDismiss = { showStatsPanel = false }
        )
    }

    // 情感历史时间线对话框
    if (showTimelineDialog) {
        EmotionalTimelineDialog(
            timelineData = timelineData,
            onDismiss = { showTimelineDialog = false }
        )
    }

    // 标签选择对话框
    if (showTagDialog && selectedMessage != null) {
        val availableTags by viewModel.getUserTags().collectAsState(initial = emptyList())
        val messageTags by viewModel.getMessageTags(selectedMessage!!.id).collectAsState(initial = emptyList())

        TagSelectionDialog(
            messageId = selectedMessage!!.id,
            availableTags = availableTags,
            selectedTags = messageTags,
            onDismiss = {
                showTagDialog = false
                selectedMessage = null
            },
            onTagToggle = { tag ->
                val isSelected = messageTags.any { it.id == tag.id }
                if (isSelected) {
                    viewModel.removeTagFromMessage(selectedMessage!!.id, tag.id)
                } else {
                    viewModel.addTagToMessage(selectedMessage!!.id, tag.id)
                }
            },
            onCreateTag = { name, color ->
                viewModel.createTag(name, color)
            },
            onManageTags = {
                showTagDialog = false
                showTagManagementDialog = true
            }
        )
    }

    // 标签管理对话框
    if (showTagManagementDialog) {
        val allTags by viewModel.getUserTags().collectAsState(initial = emptyList())

        TagManagementDialog(
            tags = allTags,
            onDismiss = {
                showTagManagementDialog = false
                selectedMessage = null
            },
            onDeleteTag = { tag ->
                viewModel.deleteTag(tag)
            },
            onCreateTag = { name, color ->
                viewModel.createTag(name, color)
            }
        )
    }

    // 用户头像更换对话框
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

    // AI 伴侣头像更换对话框
    if (showCompanionAvatarDialog) {
        AvatarSettingsDialog(
            currentAvatarUrl = companionAvatar,
            title = "设置${companion.name}的头像",
            onDismiss = { showCompanionAvatarDialog = false },
            onAvatarSelected = { uri ->
                viewModel.saveCompanionAvatar(companionId, uri.toString())
                showCompanionAvatarDialog = false
            },
            onClearAvatar = {
                viewModel.saveCompanionAvatar(companionId, null)
            }
        )
    }
}

/**
 * 聊天顶部栏：头像 + 名字 + 动态状态
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ChatTopBar(
    companion: com.companion.cc.domain.model.Companion,
    customCharacterName: String? = null,
    onNavigateBack: () -> Unit,
    onMenuClick: () -> Unit,
    onAvatarLongPress: () -> Unit = {},
    isLoading: Boolean = false,
    shouldShowTyping: Boolean = false,
    isOffline: Boolean = false,
    messages: List<Message> = emptyList()
) {
    // V9PM 原型：透明顶栏，62dp 起浮于极光上；ghost 玻璃圆钮；presence 居中；无时间线按钮
    val visualTheme = com.companion.cc.ui.theme.LocalVisualTheme.current
    val night = visualTheme.tokens.backdrop.isDark
    val colors = if (night) com.companion.cc.ui.designsystem.AuroraNight else com.companion.cc.ui.designsystem.AuroraDay
    val lastMessage = messages.lastOrNull()
    val statusText = if (lastMessage != null) {
        val diff = System.currentTimeMillis() - lastMessage.timestamp
        when {
            diff < 60_000 -> "刚刚活跃"
            diff < 3_600_000 -> "${diff / 60_000}分钟前活跃"
            diff < 86_400_000 -> "${diff / 3_600_000}小时前活跃"
            else -> "空闲中"
        }
    } else {
        "等待开始对话"
    }
    val statusColor = when {
        isLoading -> visualTheme.tokens.status.warning
        shouldShowTyping -> visualTheme.tokens.status.success
        isOffline -> visualTheme.tokens.contentMuted
        else -> visualTheme.tokens.status.info
    }
    val statusLabel = when {
        isLoading -> "正在思考..."
        shouldShowTyping -> "正在回复..."
        isOffline -> "离线模式"
        else -> statusText
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 62.dp, start = 12.dp, end = 12.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        V9PMIconButton(
            icon = Icons.Default.ArrowBack,
            contentDescription = "返回",
            onClick = onNavigateBack,
            size = 42.dp,
            iconSize = 20.dp,
            shape = CircleShape,
            tint = colors.ink
        )
        Spacer(Modifier.width(6.dp))
        // presence：名字/状态 居中（长按 = 角色详情，保住原长按头像入口）
        Column(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(onClick = {}, onLongClick = onAvatarLongPress),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = customCharacterName ?: companion.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 21.sp,
                color = colors.ink
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatusDot(statusColor)
                StatusText(statusLabel)
            }
        }
        Spacer(Modifier.width(6.dp))
        V9PMIconButton(
            icon = Icons.Default.MoreVert,
            contentDescription = "菜单",
            onClick = onMenuClick,
            size = 42.dp,
            iconSize = 20.dp,
            shape = CircleShape,
            tint = colors.ink
        )
    }
}

@Composable
private fun StatusDot(color: Color) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color, CircleShape)
    )
}

@Composable
private fun StatusText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * 消息气泡：对白为主体，动作以小字浅色显示在对白下方
 */
@Composable
private fun MessageBubble(
    message: Message,
    companionEmoji: String,
    companionAvatar: String?,
    userAvatar: String?,
    onLongPress: () -> Unit,
    shouldStream: Boolean = false,
    streamText: String? = null,
    entranceDelayMs: Int = -1
) {
    val isUser = message.role == MessageRole.USER
    val visualTheme = LocalVisualTheme.current
    // V8 ② 气泡入场：m-bubble-in 260ms（scale .92 + 16dp 上移 + 淡入）；entranceDelayMs<0 时不播
    val entrance = remember { Animatable(if (entranceDelayMs >= 0) 0f else 1f) }
    LaunchedEffect(entranceDelayMs) {
        if (entranceDelayMs >= 0) {
            kotlinx.coroutines.delay(entranceDelayMs.toLong())
            entrance.animateTo(1f, tween(AuroraDuration.BubbleIn, easing = AuroraCurves.BubbleEmphasized))
        }
    }
    val bubbleRisePx = with(LocalDensity.current) { 16.dp.toPx() }

    // 流式输出效果：仅正在流式传输的 AI 消息播放逐字动画，历史消息直接显示
    // 流式期间原始文本可能含 [动作: xxx]，先剥离动作只显示对白，避免动作混入内容
    val parsedStoredContent = remember(message.id, message.content) {
        if (!isUser && message.action.isNullOrBlank()) {
            MessageContentParser.parse(message.content)
        } else {
            null
        }
    }
    val parsedStreamContent = remember(message.id, streamText) {
        if (!isUser && !streamText.isNullOrBlank()) {
            MessageContentParser.parse(streamText)
        } else {
            null
        }
    }
    val actionText = message.action?.trim().orEmpty().ifBlank {
        parsedStreamContent?.action.orEmpty().ifBlank { parsedStoredContent?.action.orEmpty() }
    }
    val displayText = if (!isUser) {
        val raw = streamText?.let { parsedStreamContent?.dialogue ?: it }
            ?: parsedStoredContent?.dialogue
            ?: message.content
        rememberStreamingText(
            fullText = raw,
            isStreaming = shouldStream,
            streamingSpeed = 30L,
            streamKey = message.id
        ).trim()
    } else {
        message.content.trim()
    }
    val visibleDialogue = when {
        displayText.isNotBlank() -> displayText
        !isUser && actionText.isNotBlank() -> "……"
        else -> ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = entrance.value
                val s = 0.92f + 0.08f * entrance.value
                scaleX = s; scaleY = s
                translationY = bubbleRisePx * (1f - entrance.value)
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        // AI 消息显示头像
        if (!isUser) {
            CompanionAvatar(
                avatarUrl = companionAvatar,
                emoji = companionEmoji,
                size = 30.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // 消息内容
        Column(
            modifier = Modifier.widthIn(
                max = LocalConfiguration.current.screenWidthDp.dp * 0.75f
            ),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // V9PM 气泡新装：AI=固定玻璃 / 用户=135° 渐变实心；非对称圆角 22/22/10/22；用户 shadow-low
            val night = com.companion.cc.ui.theme.LocalVisualTheme.current.tokens.backdrop.isDark
            val auroraColors = if (night) com.companion.cc.ui.designsystem.AuroraNight else com.companion.cc.ui.designsystem.AuroraDay
            val bubbleShape = if (isUser)
                RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomEnd = 10.dp, bottomStart = 22.dp)
            else
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 6.dp)
            Column(
                modifier = Modifier
                    .shadow(
                        if (isUser) 4.dp else 0.dp,
                        bubbleShape,
                        ambientColor = if (night) Color(0x59000000) else Color(0x121C2230),
                        spotColor = if (night) Color(0x59000000) else Color(0x121C2230)
                    )
                    .clip(bubbleShape)
                    .then(
                        if (isUser) Modifier.drawBehind {
                            // V7 --bubble-me：linear-gradient(135deg) 左上→右下
                            drawRect(
                                Brush.linearGradient(
                                    colors = listOf(auroraColors.bubbleMeStart, auroraColors.bubbleMeEnd),
                                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                    end = androidx.compose.ui.geometry.Offset(size.width, size.height)
                                )
                            )
                        } else Modifier.auroraGlassV3(
                            GlassTierV3.Regular,
                            night,
                            AuroraChatTokens.MessageRadius.value.toInt(),
                            scrolling = shouldStream,
                            allowRenderEffect = false
                        )
                    )
                    .then(
                        if (!isUser) Modifier.drawWithContent {
                            // 非 Generic border 位图路径在 0 尺寸首帧会崩——hairline 用轮廓描边
                            drawContent()
                            drawOutline(
                                bubbleShape.createOutline(size, layoutDirection, this),
                                auroraColors.hairline.copy(alpha = 0.55f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx())
                            )
                        } else Modifier
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = { onLongPress() })
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 图片附件（用户消息且有图片时显示）
                    if (isUser && !message.imageUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 图片缩略图
                                AsyncImage(
                                    model = androidx.compose.ui.platform.LocalContext.current.let { context ->
                                        coil.request.ImageRequest.Builder(context)
                                            .data(message.imageUrl)
                                            .crossfade(true)
                                            .build()
                                    },
                                    contentDescription = "用户发送的图片",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 200.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )

                                // 视觉理解结果（如果有）
                                if (!message.imageAnalysis.isNullOrBlank()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = "图片理解",
                                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                        )
                                        Text(
                                            text = message.imageAnalysis,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 对白（Markdown 渲染）
                    if (visibleDialogue.isNotBlank()) {
                        MarkdownText(
                            text = visibleDialogue,
                            color = if (isUser)
                                Color.White
                            else
                                auroraColors.ink
                        )
                    }

                    // 动作描述（仅 AI 消息且有动作时显示，弱化视觉存在感）
                    if (!isUser && !shouldStream && actionText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = actionText,
                            style = MaterialTheme.typography.bodySmall,
                            color = auroraColors.inkMuted.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }

                    // 重要标记
                    if (message.importance > 80) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "重要",
                                tint = visualTheme.tokens.status.importance,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "重要记忆",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isUser)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 时间戳
            Text(
                text = formatTime(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp)
            )
        }

        // 用户消息显示头像
        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            UserAvatar(
                avatarUrl = userAvatar,
                size = 40.dp
            )
        }
    }
}

/**
 * 打字指示器（三个跳动的小点）
 */
@Composable
private fun TypingIndicator(companionEmoji: String, companionAvatar: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        CompactGlassSurface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                com.companion.cc.ui.components.CompanionAvatar(avatarUrl = companionAvatar, emoji = companionEmoji, size = 40.dp)
            }
        }

        CompactGlassSurface(
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TypingDot(delay = 0)
                TypingDot(delay = 150)
                TypingDot(delay = 300)
            }
        }
    }
}

@Composable
private fun TypingDot(delay: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = delay),
            repeatMode = RepeatMode.Reverse
        ),
        label = "typing"
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .offset(y = offsetY.dp)
            .background(
                MaterialTheme.colorScheme.onSurfaceVariant,
                CircleShape
            )
    )
}

/**
 * 底部输入栏：图片按钮 + 语音按钮 + 输入框 + 发送按钮
 */
@Composable
private fun ChatInputBar(
    message: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onImageClick: () -> Unit = {},
    onVoiceClick: () -> Unit = {},
    isSending: Boolean,
    showVoiceButton: Boolean = false,
    selectedImageUri: Uri? = null,
    onClearImage: () -> Unit = {}
) {
    GlassBottomDock {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 图片预览（选中图片后显示）
            if (selectedImageUri != null) {
                ImagePreview(
                    imageUri = selectedImageUri,
                    onClear = onClearImage
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 图片按钮
                V9PMIconButton(
                    icon = Icons.Default.Photo,
                    contentDescription = "选择图片",
                    onClick = onImageClick,
                    size = 48.dp,
                    iconSize = 20.dp,
                    shape = smoothCorner(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                // 语音输入按钮
                if (showVoiceButton) {
                    V9PMIconButton(
                        icon = Icons.Default.Mic,
                        contentDescription = "语音输入",
                        onClick = onVoiceClick,
                        size = 48.dp,
                        iconSize = 20.dp,
                        shape = smoothCorner(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                val inputShape = smoothCorner(18.dp)
                val inputBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(inputShape)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
                            inputShape
                        )
                        .drawBehind {
                            // 活聊天输入框：由 V9PM 连续大 R 外壳绘制，避免 M3 默认边框盖住形状
                            drawOutline(
                                inputShape.createOutline(size, layoutDirection, this),
                                inputBorderColor,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx())
                            )
                        }
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = message,
                        onValueChange = onMessageChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp, max = 124.dp)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 5,
                        decorationBox = { innerTextField ->
                            Box {
                                if (message.isEmpty()) {
                                    Text(
                                        if (selectedImageUri != null) "描述一下这张图片..." else "输入消息...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                val canSend = (message.isNotBlank() || selectedImageUri != null) && !isSending
                V9PMIconButton(
                    icon = if (isSending) Icons.Default.HourglassTop else Icons.Default.Send,
                    contentDescription = if (isSending) "正在发送" else "发送",
                    onClick = onSend,
                    enabled = canSend,
                    size = 48.dp,
                    iconSize = 20.dp,
                    shape = smoothCorner(18.dp),
                    tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 消息长按操作菜单：复制 / 删除
 */
@Composable
private fun MessageActionsMenu(
    message: Message,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onMarkImportant: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddTag: () -> Unit,
    onShare: () -> Unit
) {
    val night = LocalVisualTheme.current.tokens.backdrop.isDark
    val colors = if (night) AuroraNight else AuroraDay

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = true,
                enter = scaleIn(initialScale = 0.92f, animationSpec = tween(260, easing = AuroraCurves.BubbleEmphasized)) +
                    fadeIn(tween(260, easing = AuroraCurves.BubbleEmphasized)),
                exit = fadeOut(tween(160)),
            ) {
                V9PMDialogSurface(
                    onDismissRequest = onDismiss,
                    modifier = Modifier.widthIn(max = 360.dp)
                ) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 12.dp)) {
                        Text(
                            "消息操作",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            color = colors.ink,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        PopupActionRow(Icons.Default.ContentCopy, "复制消息", onCopy, colors.ink)
                        PopupActionRow(
                            if (message.isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            if (message.isFavorited) "取消收藏" else "收藏消息",
                            onToggleFavorite,
                            if (message.isFavorited) MaterialTheme.colorScheme.error else colors.ink,
                        )
                        if (message.importance < 90) {
                            PopupActionRow(Icons.Default.Star, "标记为重要", onMarkImportant, LocalVisualTheme.current.tokens.status.importance)
                        }
                        PopupActionRow(Icons.Default.Label, "添加标签", onAddTag, colors.ink)
                        PopupActionRow(Icons.Default.Share, "分享消息", onShare, colors.ink)
                        PopupActionRow(Icons.Default.Delete, "删除消息", onDelete, MaterialTheme.colorScheme.error)
                        Text(
                            "取消",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onDismiss)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            color = colors.inkMuted,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PopupActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = tint, fontSize = 15.sp)
    }
}

/**
 * 功能菜单：记忆树 / 数据统计 / 数据管理 / 收藏夹
 */
@Composable
private fun FunctionMenu(
    onDismiss: () -> Unit,
    onMemoryClick: () -> Unit,
    onStatsClick: () -> Unit,
    onDataClick: () -> Unit,
    onFavoritesClick: () -> Unit
) {
    val night = LocalVisualTheme.current.tokens.backdrop.isDark
    val colors = if (night) AuroraNight else AuroraDay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.TopEnd
    ) {
        GlassSurface(
            modifier = Modifier
                .padding(top = 126.dp, end = 12.dp)
                .widthIn(min = 210.dp, max = 280.dp),
            shape = smoothCorner(28.dp),
            contentPadding = PaddingValues(0.dp),
            useStrongFill = true,
            enableTouchFeedback = false
        ) {
            Column(Modifier.padding(vertical = 10.dp)) {
                Text(
                    "去看看",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = colors.inkMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                PopupActionRow(Icons.Default.AccountTree, "记忆树", onMemoryClick, colors.ink)
                PopupActionRow(Icons.Default.Favorite, "收藏夹", onFavoritesClick, colors.ink)
                PopupActionRow(Icons.Default.BarChart, "数据统计", onStatsClick, colors.ink)
                PopupActionRow(Icons.Default.Storage, "数据管理", onDataClick, colors.ink)
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    val calendar = Calendar.getInstance()
    val messageCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }

    val today = calendar.get(Calendar.DAY_OF_YEAR)
    val messageDay = messageCalendar.get(Calendar.DAY_OF_YEAR)
    val sameYear = calendar.get(Calendar.YEAR) == messageCalendar.get(Calendar.YEAR)

    return when {
        // 1分钟内：刚才
        diff < 60 * 1000 -> "刚才"

        // 1小时内：X分钟前
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"

        // 今天：显示时间
        today == messageDay && sameYear -> {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        }

        // 昨天
        today - messageDay == 1 && sameYear -> {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            "昨天 ${sdf.format(Date(timestamp))}"
        }

        // 本周内：显示星期
        diff < 7 * 24 * 60 * 60 * 1000 && sameYear -> {
            val weekdays = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
            val dayOfWeek = messageCalendar.get(Calendar.DAY_OF_WEEK) - 1
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            "${weekdays[dayOfWeek]} ${sdf.format(Date(timestamp))}"
        }

        // 今年内：月-日 时间
        sameYear -> {
            val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        }

        // 更早：年-月-日
        else -> {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

/**
 * 图片预览组件（输入栏上方显示选中的图片）
 */
@Composable
private fun ImagePreview(
    imageUri: Uri,
    onClear: () -> Unit
) {
    CompactGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier.size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "图片预览",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Column {
                    Text(
                        text = "已选择图片",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "将通过视觉模型理解图片内容",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            V9PMIconButton(
                icon = Icons.Default.Close,
                contentDescription = "取消选择",
                onClick = onClear,
                size = 42.dp,
                iconSize = 18.dp,
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
