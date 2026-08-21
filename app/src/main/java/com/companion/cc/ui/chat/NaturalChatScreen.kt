package com.companion.cc.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.companion.cc.domain.model.Message
import com.companion.cc.domain.model.MessageRole
import com.companion.cc.domain.model.companions
import com.companion.cc.ui.chat.components.EmotionalStatsDialog
import com.companion.cc.ui.chat.components.EmotionalTimelineDialog
import com.companion.cc.ui.chat.components.TTSSpeakingIndicator
import com.companion.cc.ui.chat.components.VoiceListeningIndicator
import com.companion.cc.ui.components.TagSelectionDialog
import com.companion.cc.ui.components.TagManagementDialog
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

/**
 * 自然聊天界面
 *
 * - 消息气泡（对白 + 动作分离显示）
 * - 流式逐字动画（仅对正在流式传输的消息播放）
 * - 顶部动态状态（正在思考 / 正在回复 / 离线 / 最后活跃时间）
 * - 语音输入、错误重试、离线横幅
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NaturalChatScreen(
    companionId: String,
    onNavigateBack: () -> Unit,
    onNavigateToMemory: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToData: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
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
    val latestStreamingMessageId by viewModel.latestStreamingMessageId.collectAsState()
    val customCharacterName by viewModel.customCharacterName.collectAsState()

    // 头像状态
    val userAvatar by viewModel.userAvatar.collectAsState()
    val companionAvatar by viewModel.companionAvatar.collectAsState()

    // 头像更换对话框状态
    var showUserAvatarDialog by remember { mutableStateOf(false) }
    var showCompanionAvatarDialog by remember { mutableStateOf(false) }

    // 是否离线
    val isOffline = networkState is NetworkState.Offline

    // 图片选择状态
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // 图片选择器（相册）
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    // 记录上一次的消息数量，用于检测新的用户消息
    var previousMessageCount by remember { mutableStateOf(messages.size) }
    var lastUserMessageTime by remember { mutableStateOf(0L) }

    // 检测最后一条消息是否是用户消息
    val isLastMessageFromUser = messages.lastOrNull()?.role == MessageRole.USER

    // 如果最后是用户消息，记录时间
    LaunchedEffect(messages.size, isLastMessageFromUser) {
        if (isLastMessageFromUser && messages.size != previousMessageCount) {
            lastUserMessageTime = System.currentTimeMillis()
        }
        previousMessageCount = messages.size
    }

    // 最后一条是用户消息且发送不超过30秒时，显示打字指示器
    val shouldShowTyping = isLastMessageFromUser &&
        (System.currentTimeMillis() - lastUserMessageTime) < 30000

    // 新功能状态
    val emotionalState by viewModel.emotionalState.collectAsState()
    val conversationStats by viewModel.conversationStats.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()

    val listState = rememberLazyListState()
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

    // 加载消息（只加载一次，Room Flow 会自动推送后续变化）
    var isInitialLoad by remember { mutableStateOf(true) }
    LaunchedEffect(companionId) {
        isInitialLoad = true
        viewModel.loadMessages(companionId)
        isInitialLoad = false
    }

    // 自动滚动到底部
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

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
            ChatTopBar(
                companion = companion,
                companionAvatar = companionAvatar,
                customCharacterName = customCharacterName,
                onNavigateBack = onNavigateBack,
                onMenuClick = { showMenu = true },
                onTimelineClick = {
                    scope.launch {
                        timelineData = viewModel.getEmotionalTimeline(companionId)
                        showTimelineDialog = true
                    }
                },
                onAvatarLongPress = onNavigateToCompanionDetail,  // 点击头像打开角色详情页
                isLoading = isLoading,
                shouldShowTyping = shouldShowTyping,
                isOffline = isOffline,
                messages = messages
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
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
                            viewModel.sendMessageWithImage(companionId, inputText, imageUri)
                            selectedImageUri = null  // 清空选中的图片
                        } else if (inputText.isNotBlank()) {
                            // 发送纯文字消息
                            viewModel.sendMessage(companionId, inputText)
                        }
                        inputText = ""
                        viewModel.clearDraft(companionId)  // 清除草稿
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
        containerColor = Color.Transparent
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
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { message ->
                        MessageBubble(
                            message = message,
                            companionEmoji = companion.emoji,
                            companionAvatar = companionAvatar,
                            userAvatar = userAvatar,
                            onLongPress = { selectedMessage = message },
                            shouldStream = message.id == latestStreamingMessageId
                        )
                    }

                    // 打字指示器
                    if (shouldShowTyping) {
                        item {
                            TypingIndicator(companionEmoji = companion.emoji)
                        }
                    }
                }
            }

            // 初始加载指示器
            if (isInitialLoad && messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "加载对话中...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
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

    // 功能菜单
    if (showMenu) {
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
            onSettingsClick = {
                showMenu = false
                onNavigateToSettings()
            },
            onFavoritesClick = {
                showMenu = false
                onNavigateToFavorites()
            }
        )
    }

    // 情感状态面板（弹出式）
    if (showStatsPanel) {
        EmotionalStatsDialog(
            emotionalState = emotionalState,
            conversationStats = conversationStats,
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    companion: com.companion.cc.domain.model.Companion,
    companionAvatar: String?,
    customCharacterName: String? = null,
    onNavigateBack: () -> Unit,
    onMenuClick: () -> Unit,
    onTimelineClick: () -> Unit = {},
    onAvatarLongPress: () -> Unit = {},
    isLoading: Boolean = false,
    shouldShowTyping: Boolean = false,
    isOffline: Boolean = false,
    messages: List<Message> = emptyList()
) {
    val visualTheme = LocalVisualTheme.current
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
        contentPadding = PaddingValues(0.dp),
        useStrongFill = true
    ) {
        TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 伴侣头像（支持自定义，长按更换）
                CompanionAvatar(
                    avatarUrl = companionAvatar,
                    emoji = companion.emoji,
                    size = 40.dp,
                    onClick = onAvatarLongPress
                )

                // 伴侣信息 + 动态状态
                Column {
                    Text(
                        text = customCharacterName ?: companion.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        when {
                            // 1. AI 正在思考（正在生成回复）
                            isLoading -> {
                                StatusDot(visualTheme.tokens.status.warning)
                                StatusText("正在思考...")
                            }
                            // 2. 有未回复的用户消息
                            shouldShowTyping -> {
                                StatusDot(visualTheme.tokens.status.success)
                                StatusText("正在回复...")
                            }
                            // 3. 网络离线
                            isOffline -> {
                                StatusDot(visualTheme.tokens.contentMuted)
                                StatusText("离线模式")
                            }
                            // 4. 空闲：显示最后活跃时间
                            else -> {
                                StatusDot(visualTheme.tokens.status.info)
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
                                StatusText(statusText)
                            }
                        }
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回"
                )
            }
        },
        actions = {
            // 情感历史时间线按钮
            IconButton(onClick = onTimelineClick) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = "情感历史"
                )
            }
            // 菜单按钮
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "菜单"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
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
    shouldStream: Boolean = false
) {
    val isUser = message.role == MessageRole.USER
    val visualTheme = LocalVisualTheme.current

    // 流式输出效果：仅正在流式传输的 AI 消息播放逐字动画，历史消息直接显示
    val displayText = if (!isUser) {
        rememberStreamingText(message.content, isStreaming = shouldStream, streamingSpeed = 30L)
    } else {
        message.content
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),  // 增加垂直间距从4dp到8dp
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        // AI 消息显示头像
        if (!isUser) {
            CompanionAvatar(
                avatarUrl = companionAvatar,
                emoji = companionEmoji,
                size = 40.dp
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
            // 气泡
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.94f)
                } else MaterialTheme.colorScheme.surface,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onLongPress = { onLongPress() })
                }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
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
                    if (displayText.isNotBlank()) {
                        MarkdownText(
                            text = displayText,
                            color = if (isUser)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 动作描述（仅 AI 消息且有动作时显示，弱化视觉存在感）
                    if (!isUser && !message.action.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = message.action,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
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
private fun TypingIndicator(companionEmoji: String) {
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
                Text(text = companionEmoji, fontSize = 20.sp)
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
                IconButton(
                    onClick = onImageClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Photo,
                        contentDescription = "选择图片",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // 语音输入按钮
                if (showVoiceButton) {
                    IconButton(
                        onClick = onVoiceClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "语音输入",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                OutlinedTextField(
                    value = message,
                    onValueChange = onMessageChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            if (selectedImageUri != null) "描述一下这张图片..."
                            else "输入消息..."
                        )
                    },
                    maxLines = 5,
                    shape = RoundedCornerShape(24.dp)
                )

                FloatingActionButton(
                    onClick = onSend,
                    modifier = Modifier.size(48.dp),
                    containerColor = if ((message.isNotBlank() || selectedImageUri != null) && !isSending)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "发送"
                        )
                    }
                }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("消息操作") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // 复制
                TextButton(
                    onClick = onCopy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("复制消息")
                    Spacer(modifier = Modifier.weight(1f))
                }

                // 收藏/取消收藏
                TextButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (message.isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (message.isFavorited) MaterialTheme.colorScheme.error else Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (message.isFavorited) "取消收藏" else "收藏消息")
                    Spacer(modifier = Modifier.weight(1f))
                }

                // 标记为重要
                if (message.importance < 90) {
                    TextButton(
                        onClick = onMarkImportant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = LocalVisualTheme.current.tokens.status.importance,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("标记为重要")
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                // 添加标签
                TextButton(
                    onClick = onAddTag,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Label,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("添加标签")
                    Spacer(modifier = Modifier.weight(1f))
                }

                // 分享
                TextButton(
                    onClick = onShare,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("分享消息")
                    Spacer(modifier = Modifier.weight(1f))
                }

                // 删除
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("删除消息", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 功能菜单：记忆树 / 数据统计 / 数据管理 / 设置
 */
@Composable
private fun FunctionMenu(
    onDismiss: () -> Unit,
    onMemoryClick: () -> Unit,
    onStatsClick: () -> Unit,
    onDataClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onFavoritesClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.TopEnd)
            .padding(top = 56.dp, end = 8.dp)
    ) {
        DropdownMenu(
            expanded = true,
            onDismissRequest = onDismiss
        ) {
            FunctionMenuItem(Icons.Default.AccountTree, "记忆树", onMemoryClick)
            FunctionMenuItem(Icons.Default.Favorite, "收藏夹", onFavoritesClick)
            FunctionMenuItem(Icons.Default.BarChart, "数据统计", onStatsClick)
            FunctionMenuItem(Icons.Default.Storage, "数据管理", onDataClick)
            FunctionMenuItem(Icons.Default.Settings, "设置", onSettingsClick)
        }
    }
}

@Composable
private fun FunctionMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
        Spacer(modifier = Modifier.weight(1f))
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

            IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "取消选择",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
