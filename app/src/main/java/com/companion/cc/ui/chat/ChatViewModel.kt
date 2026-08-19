package com.companion.cc.ui.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.companion.cc.data.local.SettingsManager
import com.companion.cc.di.ApplicationScope
import com.companion.cc.domain.manager.*
import com.companion.cc.domain.engine.EmotionalEngine
import com.companion.cc.domain.model.EmotionalState
import com.companion.cc.domain.model.Message
import com.companion.cc.domain.model.MessageRole
import com.companion.cc.domain.model.ChatError
import com.companion.cc.domain.model.VisionError
import com.companion.cc.domain.model.toChatError
import com.companion.cc.domain.repository.MessageRepository
import com.companion.cc.domain.usecase.*
import com.companion.cc.domain.memory.MemoryRetrievalService
import com.companion.cc.util.Logger
import com.companion.cc.util.NetworkMonitor
import com.companion.cc.util.NetworkState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * 完整集成版 ChatViewModel
 *
 * 集成了所有新功能：
 * - 4层记忆系统
 * - 情感状态引擎
 * - 人格配置系统
 * - 语音交互系统
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    // 旧的依赖（保持兼容）
    private val getMessagesUseCase: GetMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val streamSendMessageUseCase: StreamSendMessageUseCase,
    private val steppedThinkingUseCase: SteppedThinkingUseCase,
    private val vectorMemoryManagementUseCase: VectorMemoryManagementUseCase,
    private val detectEmotionUseCase: DetectEmotionUseCase,
    private val messageRepository: MessageRepository,
    private val memoryManagementUseCase: MemoryManagementUseCase,
    private val companionStateManager: CompanionStateManager,
    // 移除旧的 systemPromptManager，只使用新的 personalityManager

    // 新的依赖（新功能）
    private val memoryLayerManager: MemoryLayerManager,
    private val emotionalEngine: EmotionalEngine,
    private val personalityManager: PersonalityManager,
    private val contextManager: ContextManager,
    private val voiceManager: VoiceManager,
    private val settingsManager: SettingsManager,
    private val networkMonitor: NetworkMonitor,
    private val onlineStatusManager: OnlineStatusManager,
    private val visionManager: VisionManager,  // 视觉感知层
    private val tagDao: com.companion.cc.data.local.dao.TagDao,  // 标签系统
    private val timeContextManager: TimeContextManager,  // 时间上下文管理器
    private val eventTracker: EventTracker,  // 事件追踪器
    private val customCharacterRepository: com.companion.cc.domain.repository.CustomCharacterRepository,  // 自定义角色仓库

    // 应用级别的 Scope（替代 GlobalScope）
    @ApplicationScope private val applicationScope: CoroutineScope
    private val memoryRetrievalService: MemoryRetrievalService,
) : ViewModel() {

    private val drafts = mutableMapOf<String, String>()
    private val stateCache = mutableMapOf<String, Any>()

    // 使用真实的用户ID，而不是硬编码的 "default"
    private val userIdFlow: StateFlow<String> = settingsManager.userIdFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = "default"
    )

    private fun getUserId(): String = userIdFlow.value

    private suspend fun appendMemory2Context(basePrompt: String, companionId: String, query: String): String {
        return runCatching {
            val result = memoryRetrievalService.retrieve(query, "companion:$companionId")
            if (result.memories.isEmpty()) basePrompt else basePrompt + "\n\n【已确认的长期记忆】\n" + result.memories.joinToString("\n") { "- ${it.node.content}" }
        }.getOrElse { error ->
            Logger.w("ChatViewModel", "Memory 2.0 retrieval fallback: ${error.message}")
            basePrompt
        }
    }

    // ==================== 基础状态 ====================

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<ChatError?>(null)
    val error: StateFlow<ChatError?> = _error.asStateFlow()

    // 最后一次失败的消息（用于重试）：(userId, companionId, content)
    private var lastFailedMessage: Triple<String, String, String>? = null

    // 网络状态
    val networkState: StateFlow<NetworkState> = networkMonitor.networkState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NetworkState.Online
    )

    // 搜索状态
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Message>>(emptyList())
    val searchResults: StateFlow<List<Message>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // ==================== 新功能状态 ====================

    // 情感状态
    val emotionalState: StateFlow<EmotionalState> = emotionalEngine.currentState

    // 对话统计
    private val _conversationStats = MutableStateFlow(ConversationStats())
    val conversationStats: StateFlow<ConversationStats> = _conversationStats.asStateFlow()

    // 正在回复的角色列表（持久化状态，退出页面不丢失）
    private val _replyingCompanions = MutableStateFlow<Set<String>>(emptySet())
    val replyingCompanions: StateFlow<Set<String>> = _replyingCompanions.asStateFlow()

    // 最新流式显示的消息 ID（只有这条消息需要播放逐字动画）
    private val _latestStreamingMessageId = MutableStateFlow<String?>(null)
    val latestStreamingMessageId: StateFlow<String?> = _latestStreamingMessageId.asStateFlow()

    // 头像状态
    val userAvatar: StateFlow<String?> = settingsManager.userAvatarFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // 当前伴侣头像（动态根据 companionId 获取）
    private val _currentCompanionId = MutableStateFlow<String?>(null)
    val companionAvatar: StateFlow<String?> = _currentCompanionId.flatMapLatest { companionId ->
        if (companionId != null) {
            settingsManager.getCompanionAvatarFlow(companionId)
        } else {
            flowOf(null)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // 语音状态
    val isListening: StateFlow<Boolean> = voiceManager.isListening
    val recognitionResult: StateFlow<String?> = voiceManager.recognitionResult
    val isSpeaking: StateFlow<Boolean> = voiceManager.isSpeaking

    // TTS 设置
    private val _autoPlayTTS = MutableStateFlow(false)
    val autoPlayTTS: StateFlow<Boolean> = _autoPlayTTS.asStateFlow()

    // ==================== 初始化 ====================

    // 当前角色信息
    private val _currentCharacterId = MutableStateFlow<String?>(null)
    private val _isCustomCharacter = MutableStateFlow(false)
    private val _customCharacterName = MutableStateFlow<String?>(null)
    val customCharacterName: StateFlow<String?> = _customCharacterName.asStateFlow()

    init {
        // 初始化语音管理器
        voiceManager.initialize()

        // 监听识别结果
        viewModelScope.launch {
            voiceManager.recognitionResult.collect { result ->
                result?.let {
                    // 自动填充识别的文字
                    Logger.d("ChatViewModel", "语音识别结果: $it")
                }
            }
        }
    }

    /**
     * 设置当前角色（支持自定义角色和默认角色）
     */
    fun setCharacter(characterId: String, isCustomCharacter: Boolean) {
        _currentCharacterId.value = characterId
        _isCustomCharacter.value = isCustomCharacter
        _currentCompanionId.value = characterId

        // 加载角色的聊天历史
        loadMessages(characterId)

        // 如果是自定义角色，加载角色的人格设定
        if (isCustomCharacter) {
            viewModelScope.launch {
                try {
                    val character = customCharacterRepository.getCharacterById(characterId)
                    if (character != null) {
                        _customCharacterName.value = character.name
                        Logger.d("ChatViewModel", "加载自定义角色成功: ${character.name}")
                        // TODO: 设置角色人格到 PersonalityManager
                    } else {
                        Logger.w("ChatViewModel", "未找到自定义角色: $characterId")
                    }
                } catch (e: Exception) {
                    Logger.e("ChatViewModel", "加载自定义角色失败", e)
                }
            }
        } else {
            _customCharacterName.value = null
        }
    }

    // ==================== 草稿功能 ====================

    fun saveDraft(companionId: String, text: String) {
        drafts[companionId] = text
    }

    fun getDraft(companionId: String): String {
        return drafts[companionId] ?: ""
    }

    fun clearDraft(companionId: String) {
        drafts.remove(companionId)
    }

    // ==================== 消息重试 ====================

    /**
     * 重试最后一次失败的消息
     */
    fun retryLastMessage() {
        lastFailedMessage?.let { (_, companionId, content) ->
            Logger.d("ChatViewModel", "【重试】重试发送消息")
            _error.value = null  // 清除错误
            sendMessage(companionId, content)
        } ?: run {
            Logger.w("ChatViewModel", "【重试】没有可重试的消息")
        }
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _error.value = null
    }

    // ==================== 消息搜索 ====================

    /**
     * 搜索消息
     *
     * @param query 搜索关键词
     * @param companionId 限定伴侣ID（可选，为空则搜索所有对话）
     */
    fun searchMessages(query: String, companionId: String? = null) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _searchQuery.value = ""
            _isSearching.value = false
            return
        }

        _searchQuery.value = query
        _isSearching.value = true

        viewModelScope.launch {
            try {
                val userId = getUserId()
                messageRepository.searchMessages(userId, companionId, query).collect { results ->
                    _searchResults.value = results
                }
            } catch (e: CancellationException) {
                // 协程取消是正常的
                throw e
            } catch (e: Exception) {
                Logger.e("ChatViewModel", "搜索消息失败", e)
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    /**
     * 清除搜索结果
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    // ==================== 加载消息 ====================

    fun loadMessages(companionId: String) {
        viewModelScope.launch {
            try {
                // 设置当前 companionId，用于加载对应的头像
                _currentCompanionId.value = companionId

                val userId = getUserId()
                // 设置当前会话
                contextManager.setCurrentSession(userId, companionId)
                emotionalEngine.setCurrentSession(userId, companionId)

                // Room Flow 自动监听数据库变化，无需轮询
                getMessagesUseCase(userId, companionId).collect { messageList ->
                    _messages.value = messageList

                    // 更新对话统计
                    updateConversationStats(companionId, messageList)
                }
            } catch (e: CancellationException) {
                // 协程取消是正常的（用户退出页面），不记录为错误
                throw e
            } catch (e: Exception) {
                Logger.e("ChatViewModel", "加载消息失败", e)
                _error.value = e.toChatError()
            }
        }
    }

    // ==================== 发送消息（完整集成版）====================

    /**
     * 发送带图片的消息（多模态）
     *
     * 流程：
     * 1. 调用 VisionManager 理解图片
     * 2. 将视觉理解结果注入到用户消息
     * 3. 调用主对话模型（结合人格 + 记忆 + 视觉信息）
     */
    fun sendMessageWithImage(companionId: String, text: String, imageUri: Uri) {
        Logger.d("ChatViewModel", "========== 开始发送带图片的消息 ==========")
        Logger.d("ChatViewModel", "companionId: $companionId, text: $text, imageUri: $imageUri")

        if (_isLoading.value) {
            Logger.w("ChatViewModel", "正在发送中，忽略本次请求")
            return
        }

        // 使用 applicationScope，确保即使退出页面也能完成
        applicationScope.launch(Dispatchers.Main + SupervisorJob()) {
            _isLoading.value = true
            _error.value = null

            // 标记该角色正在回复
            _replyingCompanions.value = _replyingCompanions.value + companionId
            onlineStatusManager.setOnline(companionId)

            try {
                val userId = getUserId()
                val timestamp = System.currentTimeMillis()
                val sessionId = "${userId}_${companionId}_${timestamp}"

                // === 1. 记录事件（图片消息）===
                eventTracker.trackEvent(
                    userId = userId,
                    companionId = companionId,
                    eventType = com.companion.cc.domain.model.EventType.MESSAGE_WITH_IMAGE,
                    metadata = mapOf(
                        "textLength" to text.length.toString(),
                        "hasText" to text.isNotBlank().toString()
                    )
                )

                // === 2. 视觉理解 ===
                Logger.d("ChatViewModel", "开始视觉理解...")
                Logger.d("ChatViewModel", "imageUri: $imageUri")
                val conversationContext = _messages.value.takeLast(3).map { it.content }
                Logger.d("ChatViewModel", "对话上下文: ${conversationContext.size} 条")

                val visionAnalysis = try {
                    visionManager.analyzeImage(
                        imageUri = imageUri,
                        userText = text,
                        conversationContext = conversationContext
                    )
                } catch (e: VisionError) {
                    // 视觉理解失败时，继续流程但不带分析结果
                    Logger.e("ChatViewModel", "视觉理解失败，继续发送不带分析的消息", e)
                    null
                }

                val analysisText = visionAnalysis?.toNaturalLanguage()
                Logger.d("ChatViewModel", "视觉理解结果: ${analysisText ?: "失败"}")

                // === 2. 组装完整的用户消息（文字 + 图片引用 + 视觉理解）===
                val userMessage = Message(
                    id = "${timestamp}_${java.util.UUID.randomUUID()}_user",
                    userId = userId,
                    companionId = companionId,
                    role = MessageRole.USER,
                    content = text,
                    timestamp = timestamp,
                    imageUrl = imageUri.toString(),  // 始终保存图片 URL
                    imageAnalysis = analysisText      // 可能为 null（如果视觉理解失败）
                )

                // 保存用户消息（带图片）
                messageRepository.saveMessage(userMessage)
                _messages.value = _messages.value + userMessage

                // === 3. 构建增强的对话内容（注入视觉信息）===
                val enhancedUserMessage = if (visionAnalysis != null) {
                    buildEnhancedMessageWithVision(text, visionAnalysis)
                } else {
                    // 视觉理解失败时，使用纯文字 + 简单的图片提示
                    if (text.isNotBlank()) {
                        "用户发送了一张图片，并说：「$text」"
                    } else {
                        "用户发送了一张图片"
                    }
                }

                // === 4. 调用主对话系统（复用现有逻辑）===
                // 情感分析
                emotionalEngine.detectEmotion(enhancedUserMessage)
                val currentEmotion = emotionalEngine.currentState.value

                // 上下文管理
                contextManager.setCurrentSession(userId, companionId)
                contextManager.addMessage(enhancedUserMessage, MessageRole.USER)

                // 多层记忆系统
                val memoryContext = memoryLayerManager.getMemoryContext(
                    userId = userId,
                    companionId = companionId,
                    currentMessage = enhancedUserMessage
                )

                // 人格配置
                val systemPrompt = appendMemory2Context(personalityManager.generateSystemPrompt(
                    companionId = companionId,
                    userId = userId,
                    memoryContext = memoryContext
                ), companionId, enhancedUserMessage)

                // API 参数
                val personality = personalityManager.getCompanionConfig(companionId)
                val apiParams = if (personality != null) {
                    com.companion.cc.domain.manager.ApiParameters(
                        temperature = personality.apiParameters.temperature.toDouble(),
                        topP = personality.apiParameters.topP.toDouble(),
                        frequencyPenalty = personality.apiParameters.frequencyPenalty.toDouble(),
                        presencePenalty = personality.apiParameters.presencePenalty.toDouble(),
                        maxTokens = personality.apiParameters.maxTokens
                    )
                } else {
                    com.companion.cc.domain.manager.ApiParameters(
                        temperature = 0.8, topP = 0.9, frequencyPenalty = 0.0,
                        presencePenalty = 0.0, maxTokens = 500
                    )
                }

                // 构建对话历史
                val conversationHistory = buildSmartConversationHistory(_messages.value, maxMessages = 10)

                // === 5. 流式调用主对话模型 ===
                val assistantMessageId = "${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}_assistant"
                var assistantContent = ""

                _latestStreamingMessageId.value = assistantMessageId

                streamSendMessageUseCase(
                    systemPrompt = systemPrompt,
                    conversationHistory = conversationHistory,
                    apiParams = apiParams
                ).catch { e ->
                    Logger.e("ChatViewModel", "API调用失败", e)
                    _error.value = e.toChatError()
                    _isLoading.value = false
                }.collect { chunk ->
                    assistantContent += chunk

                    val currentMessages = _messages.value.toMutableList()
                    val existingIndex = currentMessages.indexOfFirst { it.id == assistantMessageId }

                    val assistantMessage = Message(
                        id = assistantMessageId,
                        userId = userId,
                        companionId = companionId,
                        role = MessageRole.ASSISTANT,
                        content = assistantContent,
                        timestamp = System.currentTimeMillis()
                    )

                    if (existingIndex >= 0) {
                        currentMessages[existingIndex] = assistantMessage
                    } else {
                        currentMessages.add(assistantMessage)
                    }

                    _messages.value = currentMessages
                }

                // === 6. 保存助手消息（应用风格调整 + 对白/动作分离）===
                val finalAssistantMessage = _messages.value.find { it.id == assistantMessageId }
                finalAssistantMessage?.let { msg ->
                    contextManager.addMessage(msg.content, MessageRole.ASSISTANT)

                    val adjustedContent = personalityManager.adjustResponse(
                        response = msg.content,
                        companionId = companionId
                    )

                    val (dialogue, action) = parseMessageContent(adjustedContent)

                    val finalMessage = if (adjustedContent != msg.content || action != null) {
                        msg.copy(content = dialogue, action = action)
                    } else {
                        msg
                    }

                    messageRepository.saveMessage(finalMessage)

                    if (adjustedContent != msg.content || action != null) {
                        val currentMessages = _messages.value.toMutableList()
                        val index = currentMessages.indexOfFirst { it.id == assistantMessageId }
                        if (index >= 0) {
                            currentMessages[index] = finalMessage
                            _messages.value = currentMessages
                        }
                    }
                }

                // === 7. TTS 播报 ===
                if (_autoPlayTTS.value && finalAssistantMessage != null) {
                    voiceManager.speak(finalAssistantMessage.content)
                }

                // 更新统计
                updateConversationStats(companionId, _messages.value)

                // === 8. 记录交互时间（成功完成后才记录）===
                timeContextManager.recordInteraction(
                    userId = userId,
                    companionId = companionId,
                    sessionId = sessionId
                )

                // 延迟清空流式标记
                val animationDelay = (finalAssistantMessage?.content?.length ?: 0) * 30L + 500L
                launch {
                    delay(animationDelay)
                    _latestStreamingMessageId.value = null
                }

                _replyingCompanions.value = _replyingCompanions.value - companionId
                onlineStatusManager.setOffline(companionId)
                _isLoading.value = false

            } catch (e: CancellationException) {
                _latestStreamingMessageId.value = null
                _replyingCompanions.value = _replyingCompanions.value - companionId
                onlineStatusManager.setOffline(companionId)
                _isLoading.value = false
                throw e
            } catch (e: Exception) {
                Logger.e("ChatViewModel", "发送带图片消息失败", e)
                _latestStreamingMessageId.value = null
                _error.value = e.toChatError()
                _replyingCompanions.value = _replyingCompanions.value - companionId
                onlineStatusManager.setOffline(companionId)
                _isLoading.value = false
            }
        }
    }

    /**
     * 构建增强的用户消息（注入视觉理解结果）
     *
     * 格式：用户原话 + [图片中看到的内容: 视觉理解]
     */
    private fun buildEnhancedMessageWithVision(userText: String, visionAnalysis: com.companion.cc.domain.model.VisionAnalysis): String {
        val visionPart = visionAnalysis.toNaturalLanguage()

        return if (userText.isNotBlank()) {
            "$userText\n\n[图片中看到的内容: $visionPart]"
        } else {
            "[发送了一张图片]\n\n[图片中看到的内容: $visionPart]"
        }
    }

    fun sendMessage(companionId: String, content: String) {
        if (content.isBlank() || _isLoading.value) return

        // 检查网络状态
        if (!networkMonitor.isOnline()) {
            Logger.w("ChatViewModel", "【离线】网络未连接，无法发送消息")
            _error.value = ChatError.NetworkError(
                message = "网络未连接",
                userMessage = "当前处于离线状态，请检查网络连接后重试"
            )
            // 保存失败的消息用于重试
            viewModelScope.launch {
                val userId = getUserId()
                lastFailedMessage = Triple(userId, companionId, content)
            }
            return
        }

        // 使用 applicationScope + SupervisorJob，确保即使退出页面也能继续执行
        applicationScope.launch(Dispatchers.Main + SupervisorJob()) {
            _isLoading.value = true
            _error.value = null

            // 标记该角色正在回复（持久化状态）
            _replyingCompanions.value = _replyingCompanions.value + companionId
            onlineStatusManager.setOnline(companionId)  // 同步到全局状态

            Logger.d("ChatViewModel", "=== 开始发送消息 ===")

            try {
                val userId = getUserId()
                val timestamp = System.currentTimeMillis()
                val sessionId = "${userId}_${companionId}_${timestamp}"

                // === 1. 记录事件（不记录交互时间，因为需要先获取"之前"的时间上下文）===
                eventTracker.trackEvent(
                    userId = userId,
                    companionId = companionId,
                    eventType = com.companion.cc.domain.model.EventType.MESSAGE_SENT,
                    metadata = mapOf("contentLength" to content.length.toString())
                )

                // === 2. 情感分析 ===
                emotionalEngine.detectEmotion(content)
                val currentEmotion = emotionalEngine.currentState.value

                // === 3. 上下文管理 ===
                contextManager.setCurrentSession(userId, companionId)
                contextManager.addMessage(content, MessageRole.USER)

                // === 4. 创建用户消息 ===
                val userMessage = Message(
                    id = "${timestamp}_${java.util.UUID.randomUUID()}_user",
                    userId = userId,
                    companionId = companionId,
                    role = MessageRole.USER,
                    content = content,
                    timestamp = timestamp,
                    emotion = currentEmotion.mood.name.lowercase()
                )

                // 保存用户消息
                messageRepository.saveMessage(userMessage)
                _messages.value = _messages.value + userMessage

                // === 4. 多层记忆系统 ===
                val memoryContext = memoryLayerManager.getMemoryContext(
                    userId = userId,
                    companionId = companionId,
                    currentMessage = content
                )

                // === 5. 人格配置 ===
                val personality = personalityManager.getCompanionConfig(companionId)

                // 使用完整记忆上下文生成 SystemPrompt
                val systemPrompt = appendMemory2Context(personalityManager.generateSystemPrompt(
                    companionId = companionId,
                    userId = userId,
                    memoryContext = memoryContext
                ), companionId, content)

                // === 6. 思考链（可选，失败不影响主流程）===
                try {
                    steppedThinkingUseCase(
                        companionId = companionId,
                        userMessage = content,
                        currentState = emotionalEngine.currentState.value,
                        recentMemories = formatMemoryContext(memoryContext)
                    )
                } catch (e: Exception) {
                    Logger.e("ChatViewModel", "思考链失败", e)
                }

                // === 7. 获取API参数（使用人格配置）===
                val apiParams = if (personality != null) {
                    // 使用人格配置的 API 参数
                    com.companion.cc.domain.manager.ApiParameters(
                        temperature = personality.apiParameters.temperature.toDouble(),
                        topP = personality.apiParameters.topP.toDouble(),
                        frequencyPenalty = personality.apiParameters.frequencyPenalty.toDouble(),
                        presencePenalty = personality.apiParameters.presencePenalty.toDouble(),
                        maxTokens = personality.apiParameters.maxTokens
                    )
                } else {
                    // 降级到默认参数
                    com.companion.cc.domain.manager.ApiParameters(
                        temperature = 0.8,
                        topP = 0.9,
                        frequencyPenalty = 0.0,
                        presencePenalty = 0.0,
                        maxTokens = 500
                    )
                }

                // === 8. 构建对话历史（智能筛选）===
                val conversationHistory = buildSmartConversationHistory(
                    messages = _messages.value,
                    maxMessages = 10
                )

                // === 9. 调用API（流式）===
                val assistantMessageId = "${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}_assistant"
                var assistantContent = ""

                // 设置为当前流式显示的消息（UI 据此播放逐字动画）
                _latestStreamingMessageId.value = assistantMessageId

                streamSendMessageUseCase(
                    systemPrompt = systemPrompt,
                    conversationHistory = conversationHistory,
                    apiParams = apiParams
                ).catch { e ->
                    Logger.e("ChatViewModel", "API调用失败: ${e.message}", e)
                    _error.value = e.toChatError()
                    _isLoading.value = false
                }.collect { chunk ->
                    assistantContent += chunk

                    // 实时更新消息列表
                    val currentMessages = _messages.value.toMutableList()
                    val existingIndex = currentMessages.indexOfFirst { it.id == assistantMessageId }

                    val assistantMessage = Message(
                        id = assistantMessageId,
                        userId = userId,
                        companionId = companionId,
                        role = MessageRole.ASSISTANT,
                        content = assistantContent,
                        timestamp = System.currentTimeMillis()
                    )

                    if (existingIndex >= 0) {
                        currentMessages[existingIndex] = assistantMessage
                    } else {
                        currentMessages.add(assistantMessage)
                    }

                    _messages.value = currentMessages
                }

                // === 10. 空响应检测 ===
                if (assistantContent.isEmpty()) {
                    Logger.e("ChatViewModel", "API 返回空响应，创建错误提示消息")
                    val errorMessage = Message(
                        id = assistantMessageId,
                        userId = userId,
                        companionId = companionId,
                        role = MessageRole.ASSISTANT,
                        content = "（服务暂时无响应，请稍后重试）",
                        timestamp = System.currentTimeMillis()
                    )

                    // 更新 UI
                    val currentMessages = _messages.value.toMutableList()
                    val existingIndex = currentMessages.indexOfFirst { it.id == assistantMessageId }
                    if (existingIndex >= 0) {
                        currentMessages[existingIndex] = errorMessage
                    } else {
                        currentMessages.add(errorMessage)
                    }
                    _messages.value = currentMessages

                    // 保存到数据库
                    messageRepository.saveMessage(errorMessage)

                    // 清空标记并返回
                    _latestStreamingMessageId.value = null
                    _replyingCompanions.value = _replyingCompanions.value - companionId
                    onlineStatusManager.setOffline(companionId)
                    _isLoading.value = false
                    return@launch
                }

                // === 11. 保存助手消息（风格调整 + 对白/动作分离）===
                val finalAssistantMessage = _messages.value.find { it.id == assistantMessageId }
                finalAssistantMessage?.let { msg ->
                    // 添加到上下文管理器
                    contextManager.addMessage(msg.content, MessageRole.ASSISTANT)

                    // 应用说话风格调整
                    val adjustedContent = personalityManager.adjustResponse(
                        response = msg.content,
                        companionId = companionId
                    )

                    // 解析对白和动作
                    val (dialogue, action) = parseMessageContent(adjustedContent)

                    val finalMessage = if (adjustedContent != msg.content || action != null) {
                        msg.copy(content = dialogue, action = action)
                    } else {
                        msg
                    }

                    // 保存调整后的消息
                    messageRepository.saveMessage(finalMessage)

                    // 更新 UI
                    if (adjustedContent != msg.content || action != null) {
                        val currentMessages = _messages.value.toMutableList()
                        val index = currentMessages.indexOfFirst { it.id == assistantMessageId }
                        if (index >= 0) {
                            currentMessages[index] = finalMessage
                            _messages.value = currentMessages
                        }
                    }
                }

                // === 12. 语音输出 ===
                if (_autoPlayTTS.value && finalAssistantMessage != null) {
                    voiceManager.speak(finalAssistantMessage.content)
                }

                // === 13. 更新统计 ===
                updateConversationStats(companionId, _messages.value)

                // === 14. 记录交互时间（成功完成后才记录）===
                timeContextManager.recordInteraction(
                    userId = userId,
                    companionId = companionId,
                    sessionId = sessionId
                )

                Logger.d("ChatViewModel", "=== 消息发送完成 ===")

                // 延迟清空流式显示标记，给 UI 时间播放完打字机动画
                // 延迟时间：30ms * 字符数 + 500ms 缓冲
                val animationDelay = (finalAssistantMessage?.content?.length ?: 0) * 30L + 500L
                launch {
                    delay(animationDelay)
                    _latestStreamingMessageId.value = null
                }

                // 移除正在回复标记
                _replyingCompanions.value = _replyingCompanions.value - companionId
                onlineStatusManager.setOffline(companionId)  // 同步到全局状态

                _isLoading.value = false

            } catch (e: CancellationException) {
                // 协程取消（用户退出页面等），不记录为错误
                _latestStreamingMessageId.value = null
                _replyingCompanions.value = _replyingCompanions.value - companionId
                onlineStatusManager.setOffline(companionId)
                _isLoading.value = false
                throw e
            } catch (e: Exception) {
                Logger.e("ChatViewModel", "发送消息异常", e)

                // 清空流式标记
                _latestStreamingMessageId.value = null

                // 保存失败的消息，用于重试
                val userId = getUserId()
                lastFailedMessage = Triple(userId, companionId, content)

                // 使用类型化的错误处理
                _error.value = e.toChatError()

                // 发生错误时也要移除标记
                _replyingCompanions.value = _replyingCompanions.value - companionId
                onlineStatusManager.setOffline(companionId)

                _isLoading.value = false
            }
        }
    }

    // ==================== 语音功能 ====================

    fun startVoiceInput() {
        voiceManager.startVoiceInput()
    }

    fun stopVoiceInput() {
        voiceManager.stopVoiceInput()
    }

    fun speak(text: String, pitch: Float = 1.0f, speed: Float = 1.0f) {
        voiceManager.speak(text, pitch, speed)
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    fun setAutoPlayTTS(enabled: Boolean) {
        _autoPlayTTS.value = enabled
    }

    // ==================== 消息内容解析 ====================

    /**
     * 解析消息内容，提取对白和动作（对所有角色通用）
     *
     * 支持两种格式：
     * 1. 标准格式：对白内容\n\n[动作: 动作描述]
     * 2. 兜底格式：（动作）对白 —— 兼容模型沿用的括号写法
     */
    private fun parseMessageContent(content: String): Pair<String, String?> {
        // 标准格式：[动作: xxx] 或 [ACTION: xxx]
        val actionRegex = """\[(?:动作|ACTION)[:：]\s*([^\]]+)\]""".toRegex(RegexOption.IGNORE_CASE)
        // 兜底格式：（xxx）或 (xxx)
        val parenRegex = """[（(]([^（）()]+)[）)]""".toRegex()

        val actions = mutableListOf<String>()

        // 1. 提取标准格式动作
        actionRegex.findAll(content).forEach { m ->
            m.groupValues[1].trim().takeIf { it.isNotBlank() }?.let { actions.add(it) }
        }
        var dialogue = content.replace(actionRegex, "")

        // 2. 提取括号格式动作（兜底，不依赖模型遵守 prompt 格式）
        parenRegex.findAll(dialogue).forEach { m ->
            m.groupValues[1].trim().takeIf { it.isNotBlank() }?.let { actions.add(it) }
        }
        dialogue = cleanDialogue(dialogue.replace(parenRegex, ""))

        // 整条消息都是动作时，用省略号占位，避免出现空气泡
        if (dialogue.isBlank() && actions.isNotEmpty()) {
            dialogue = "……"
        }

        val action = actions.takeIf { it.isNotEmpty() }?.joinToString("，")
        return Pair(dialogue, action)
    }

    /**
     * 清理对白文本：
     * 1. 合并因移除动作标记而断开的引号段（"段1""段2" → 段1 段2）
     * 2. 剥离包裹整句的中英文引号（气泡本身就是说的话，不需要引号）
     */
    private fun cleanDialogue(raw: String): String {
        var text = raw.trim()

        // 相邻的 闭引号+开引号 是被拆开的两段话，用空格衔接
        text = text.replace("\"\\s*\"".toRegex(), " ")
        text = text.replace("”\\s*“".toRegex(), " ")

        // 反复剥离包裹整句的成对引号（内部还有同类引号时不剥离，避免误伤）
        val quotePairs = listOf('"' to '"', '“' to '”', '\'' to '\'', '‘' to '’')
        var changed = true
        while (changed && text.length >= 2) {
            changed = false
            for ((open, close) in quotePairs) {
                if (text.first() == open && text.last() == close) {
                    val inner = text.substring(1, text.length - 1)
                    if (!inner.contains(open) && !inner.contains(close)) {
                        text = inner.trim()
                        changed = true
                    }
                }
            }
        }
        return text
    }

    // ==================== 消息管理 ====================

    /**
     * 删除消息
     */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                // 从数据库删除
                messageRepository.deleteMessage(messageId)

                // 从 UI 列表移除
                val currentMessages = _messages.value.toMutableList()
                currentMessages.removeAll { it.id == messageId }
                _messages.value = currentMessages
            } catch (e: Exception) {
                Logger.e("ChatViewModel", "删除消息失败", e)
                _error.value = ChatError.LocalStorageError("删除消息失败: ${e.message}")
            }
        }
    }

    fun updateMessageImportance(messageId: String, importance: Int) {
        viewModelScope.launch {
            try {
                messageRepository.updateMessageImportance(messageId, importance)

                // 更新 UI 列表中的消息
                val currentMessages = _messages.value.toMutableList()
                val index = currentMessages.indexOfFirst { it.id == messageId }
                if (index != -1) {
                    currentMessages[index] = currentMessages[index].copy(importance = importance)
                    _messages.value = currentMessages
                }
            } catch (e: Exception) {
                Logger.e("ChatViewModel", "更新消息重要度失败", e)
            }
        }
    }

    fun toggleMessageFavorite(messageId: String) {
        viewModelScope.launch {
            try {
                // 获取当前消息状态
                val currentMessage = _messages.value.find { it.id == messageId }
                val newFavoriteState = !(currentMessage?.isFavorited ?: false)

                messageRepository.toggleMessageFavorite(messageId, newFavoriteState)

                // 更新 UI 列表中的消息
                val currentMessages = _messages.value.toMutableList()
                val index = currentMessages.indexOfFirst { it.id == messageId }
                if (index != -1) {
                    currentMessages[index] = currentMessages[index].copy(isFavorited = newFavoriteState)
                    _messages.value = currentMessages
                }
            } catch (e: Exception) {
                Logger.e("ChatViewModel", "切换收藏状态失败", e)
            }
        }
    }

    /**
     * 编辑消息（重新发送）
     */
    fun editMessage(messageId: String, newContent: String, companionId: String) {
        viewModelScope.launch {
            try {
                // 找到要编辑的消息
                val message = _messages.value.find { it.id == messageId }
                if (message == null || message.role != MessageRole.USER) {
                    _error.value = ChatError.ConfigError("只能编辑用户消息")
                    return@launch
                }

                // 删除该消息及之后的所有消息
                val messageIndex = _messages.value.indexOf(message)
                val messagesToDelete = _messages.value.subList(messageIndex, _messages.value.size)
                messagesToDelete.forEach { msg ->
                    messageRepository.deleteMessage(msg.id)
                }

                // 更新 UI
                _messages.value = _messages.value.take(messageIndex)

                // 重新发送新内容
                sendMessage(companionId, newContent)
            } catch (e: Exception) {
                Logger.e("ChatViewModel", "编辑消息失败", e)
                _error.value = ChatError.LocalStorageError("编辑消息失败: ${e.message}")
            }
        }
    }

    /**
     * 重发消息
     */
    fun resendMessage(messageId: String, companionId: String) {
        viewModelScope.launch {
            try {
                val message = _messages.value.find { it.id == messageId }
                if (message == null || message.role != MessageRole.USER) {
                    _error.value = ChatError.ConfigError("只能重发用户消息")
                    return@launch
                }

                // 删除原消息及之后的消息并重新发送
                editMessage(messageId, message.content, companionId)
            } catch (e: Exception) {
                Logger.e("ChatViewModel", "重发消息失败", e)
                _error.value = ChatError.LocalStorageError("重发消息失败: ${e.message}")
            }
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建智能对话历史
     *
     * 优先级：
     * 1. 最近3条消息（保持连贯）
     * 2. 重要的历史消息（根据 importance）
     * 3. 填充到 maxMessages 条
     */
    private fun buildSmartConversationHistory(
        messages: List<Message>,
        maxMessages: Int = 10
    ): List<Map<String, String>> {
        if (messages.isEmpty()) return emptyList()

        val result = mutableListOf<Message>()

        // 1. 最近3条消息（必须包含）
        val recentMessages = messages.takeLast(3)
        result.addAll(recentMessages)

        // 2. 如果还有空间，添加重要的历史消息
        val remainingSlots = maxMessages - recentMessages.size
        if (remainingSlots > 0) {
            val olderMessages = messages.dropLast(3)

            // 按重要性排序
            val importantMessages = olderMessages
                .sortedByDescending { it.importance }
                .take(remainingSlots)
                .sortedBy { it.timestamp } // 恢复时间顺序

            result.addAll(0, importantMessages)
        }

        // 3. 转换为 API 格式
        return result.map { msg ->
            mapOf(
                "role" to if (msg.role == MessageRole.USER) "user" else "assistant",
                "content" to msg.content
            )
        }
    }

    private fun updateConversationStats(companionId: String, messages: List<Message>) {
        val userMessages = messages.count { it.role == MessageRole.USER }
        val assistantMessages = messages.count { it.role == MessageRole.ASSISTANT }
        val rounds = minOf(userMessages, assistantMessages)

        val topics = contextManager.currentTopics.value
        val vectorMemoryCount = 0 // 向量记忆未实现

        // 从人格配置获取主要特质
        val topTraits = personalityManager.getCompanionConfig(companionId)?.personality?.coreTraits?.take(5) ?: emptyList()

        val emotionSum = messages.mapNotNull { msg ->
            when (msg.emotion?.lowercase()) {
                "happy", "excited" -> 1f
                "sad", "anxious" -> -1f
                else -> 0f
            }
        }.sum()

        val emotionalScore = if (messages.isNotEmpty()) emotionSum / messages.size else 0f

        _conversationStats.value = ConversationStats(
            totalMessages = messages.size,
            conversationRounds = rounds,
            currentTopics = topics,
            topTraits = topTraits,
            sessionStartTime = messages.firstOrNull()?.timestamp ?: System.currentTimeMillis(),
            vectorMemoryCount = vectorMemoryCount,
            emotionalScore = emotionalScore
        )
    }

    private fun formatMemoryContext(context: com.companion.cc.domain.model.CompleteMemoryContext): String {
        val sb = StringBuilder()

        // 短期记忆
        if (context.shortTerm.isNotEmpty()) {
            sb.append("【最近对话】\n")
            context.shortTerm.takeLast(5).forEach { memory ->
                sb.append("- $memory\n")
            }
        }

        // 中期记忆
        if (context.midTerm.isNotEmpty()) {
            sb.append("\n【近期总结】\n")
            context.midTerm.forEach { summary ->
                sb.append("- $summary\n")
            }
        }

        // 长期记忆
        if (context.longTerm.isNotEmpty()) {
            sb.append("\n【重要记忆】\n")
            context.longTerm.forEach { memory ->
                sb.append("- $memory\n")
            }
        }

        return sb.toString()
    }

    /**
     * 获取情感历史时间线数据
     * 从消息记录中提取情感状态变化
     */
    suspend fun getEmotionalTimeline(companionId: String): List<com.companion.cc.ui.chat.components.EmotionalTimelinePoint> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = getUserId()
                val messages = messageRepository.getMessages(userId, companionId, limit = 1000)
                    .first() // 收集 Flow 的第一个值

                // 从消息中采样，构建时间线
                // 每隔 10 条消息采样一次，或者至少间隔 30 分钟
                val timeline = mutableListOf<com.companion.cc.ui.chat.components.EmotionalTimelinePoint>()
                var lastTimestamp = 0L
                val minInterval = 30 * 60 * 1000L // 30 分钟

                messages.forEachIndexed { index, message ->
                    val shouldSample = index % 10 == 0 ||
                                      (message.timestamp - lastTimestamp > minInterval)

                    if (shouldSample && message.emotion != null) {
                        // 从 emotion 字符串解析情感状态
                        val emotionParts = message.emotion.split(",")
                        val mood = emotionParts.getOrNull(0)?.let { moodStr ->
                            try {
                                com.companion.cc.domain.model.Mood.valueOf(moodStr.uppercase())
                            } catch (e: Exception) {
                                com.companion.cc.domain.model.Mood.CALM
                            }
                        } ?: com.companion.cc.domain.model.Mood.CALM

                        // 计算当前情感值（基于消息重要度和时间权重）
                        val baseAffection = 50 + (message.importance - 50) / 2
                        val baseTrust = 50 + (message.importance - 50) / 3
                        val baseInterest = message.importance

                        timeline.add(
                            com.companion.cc.ui.chat.components.EmotionalTimelinePoint(
                                timestamp = message.timestamp,
                                affection = baseAffection,
                                trust = baseTrust,
                                interest = baseInterest,
                                mood = mood
                            )
                        )

                        lastTimestamp = message.timestamp
                    }
                }

                // 如果没有足够的数据，使用当前情感状态作为唯一点
                if (timeline.isEmpty()) {
                    val currentState = emotionalState.value
                    timeline.add(
                        com.companion.cc.ui.chat.components.EmotionalTimelinePoint(
                            timestamp = System.currentTimeMillis(),
                            affection = (currentState.affection * 100).toInt(),
                            trust = 50, // 默认中等信任度
                            interest = 50, // 默认中等兴趣度
                            mood = currentState.mood
                        )
                    )
                }

                timeline.sortedBy { it.timestamp }
            } catch (e: Exception) {
                Logger.e("ChatViewModel", "获取情感历史失败", e)
                emptyList()
            }
        }
    }

    // ==================== 标签系统 ====================

    /**
     * 获取收藏的消息
     */
    fun getFavoriteMessages(companionId: String): Flow<List<Message>> {
        val userId = getUserId()
        return messageRepository.getFavoritedMessages(userId)
            .map { messages -> messages.filter { it.companionId == companionId } }
    }

    /**
     * 切换消息收藏状态
     */
    fun toggleFavorite(messageId: String, isFavorited: Boolean) {
        viewModelScope.launch {
            try {
                messageRepository.toggleMessageFavorite(messageId, isFavorited)
            } catch (e: Exception) {
                Logger.e("ChatViewModel", "切换收藏状态失败", e)
            }
        }
    }

    /**
     * 获取用户的所有标签
     */
    fun getUserTags(): Flow<List<com.companion.cc.data.local.entity.TagEntity>> {
        val userId = getUserId()
        return tagDao.getTags(userId)
    }

    /**
     * 获取消息的标签
     */
    fun getMessageTags(messageId: String): Flow<List<com.companion.cc.data.local.entity.TagEntity>> {
        return tagDao.getMessageTags(messageId)
    }

    /**
     * 创建新标签
     */
    fun createTag(name: String, color: String) {
        viewModelScope.launch {
            try {
                val userId = getUserId()
                val tag = com.companion.cc.data.local.entity.TagEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = userId,
                    name = name,
                    color = color,
                    createdAt = System.currentTimeMillis()
                )
                tagDao.insertTag(tag)
            } catch (e: Exception) {
                Logger.e("ChatViewModel", "创建标签失败", e)
            }
        }
    }

    /**
     * 删除标签
     */
    fun deleteTag(tag: com.companion.cc.data.local.entity.TagEntity) {
        viewModelScope.launch {
            try {
                tagDao.deleteTag(tag)
            } catch (e: Exception) {
                Logger.e("ChatViewModel", "删除标签失败", e)
            }
        }
    }

    /**
     * 为消息添加标签
     */
    fun addTagToMessage(messageId: String, tagId: String) {
        viewModelScope.launch {
            try {
                val messageTag = com.companion.cc.data.local.entity.MessageTagEntity(
                    messageId = messageId,
                    tagId = tagId,
                    createdAt = System.currentTimeMillis()
                )
                tagDao.addTagToMessage(messageTag)
            } catch (e: Exception) {
                Logger.e("ChatViewModel", "添加标签失败", e)
            }
        }
    }

    /**
     * 从消息移除标签
     */
    fun removeTagFromMessage(messageId: String, tagId: String) {
        viewModelScope.launch {
            try {
                tagDao.removeTagFromMessage(messageId, tagId)
            } catch (e: Exception) {
                Logger.e("ChatViewModel", "移除标签失败", e)
            }
        }
    }

    /**
     * 获取带有特定标签的消息
     */
    fun getMessagesByTag(tagId: String): Flow<List<com.companion.cc.data.local.entity.MessageEntity>> {
        val userId = getUserId()
        return tagDao.getMessagesByTag(userId, tagId)
    }

    // ==================== 头像管理 ====================

    /**
     * 保存用户头像
     */
    fun saveUserAvatar(avatarUrl: String?) {
        viewModelScope.launch {
            settingsManager.saveUserAvatar(avatarUrl)
            Logger.d("ChatViewModel", "用户头像已保存")
        }
    }

    /**
     * 保存 AI 伴侣头像
     */
    fun saveCompanionAvatar(companionId: String, avatarUrl: String?) {
        viewModelScope.launch {
            settingsManager.saveCompanionAvatar(companionId, avatarUrl)
            Logger.d("ChatViewModel", "伴侣 $companionId 头像已保存")
        }
    }

    override fun onCleared() {
        super.onCleared()
        // VoiceManager 会自动清理
    }
}
