package com.companion.cc.ui.chat

import com.companion.cc.domain.character.CharacterCatalog
import com.companion.cc.domain.character.CharacterPromptResolver
import com.companion.cc.domain.character.ResolvedCharacterPrompt
import com.companion.cc.domain.character.CharacterPromptSource
import com.companion.cc.domain.character.UnknownCharacterException
import com.companion.cc.domain.manager.*
import com.companion.cc.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime

/**
 * ChatViewModel resolver 集成测试
 *
 * 重点验证：
 * 1. setCharacter 成功解析后缓存配置并失效记忆
 * 2. UnknownCharacterException 设置 CharacterNotFound 错误
 * 3. catalog 返回 null 设置 CharacterNotFound 错误
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelResolverTest {
    private lateinit var viewModel: ChatViewModel
    private lateinit var characterPromptResolver: CharacterPromptResolver
    private lateinit var personalityManager: PersonalityManager
    private lateinit var memoryLayerManager: MemoryLayerManager
    private lateinit var characterCatalog: CharacterCatalog
    private lateinit var streamSendMessageUseCase: com.companion.cc.domain.usecase.StreamSendMessageUseCase
    private lateinit var memoryRetrievalService: com.companion.cc.domain.memory.MemoryRetrievalService
    private lateinit var networkMonitor: com.companion.cc.util.NetworkMonitor
    private lateinit var typingStateManager: TypingStateManager
    private lateinit var applicationScope: TestScope

    private val testDispatcher = StandardTestDispatcher()
    private val testUserId = "test-user-123"
    private val testCharacterId = "test-char-456"

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        characterPromptResolver = mock()
        personalityManager = mock()
        memoryLayerManager = mock()
        characterCatalog = mock()
        streamSendMessageUseCase = mock()
        memoryRetrievalService = mock()
        networkMonitor = mock()
        typingStateManager = mock {
            on { typingCharacters } doReturn MutableStateFlow(emptySet())
        }
        applicationScope = TestScope(testDispatcher)

        val stableEmotionalState = MutableStateFlow(
            EmotionalState(Mood.CALM, 1.0f, 0.5f, 0.5f)
        )
        val mockEmotionalEngine = mock<com.companion.cc.domain.engine.EmotionalEngine>()
        whenever(mockEmotionalEngine.currentState).thenReturn(stableEmotionalState)
        whenever(mockEmotionalEngine.temperamentFor(any()))
            .thenReturn(com.companion.cc.domain.character.TemperamentProfile.DEFAULT)
        whenever(mockEmotionalEngine.getEmotionalState(any(), any()))
            .thenReturn(stableEmotionalState.value)

        val mockSettingsManager = mock<com.companion.cc.data.local.SettingsManager>()
        whenever(mockSettingsManager.materialStyleFlow).thenReturn(flowOf("GLASS"))
        whenever(mockSettingsManager.userAvatarFlow).thenReturn(flowOf(null))
        whenever(mockSettingsManager.getCompanionAvatarFlow(any())).thenReturn(flowOf(null))

        viewModel = ChatViewModel(
            getMessagesUseCase = mock {
                onBlocking { invoke(any(), any()) } doReturn flowOf(emptyList())
            },
            sendMessageUseCase = mock(),
            streamSendMessageUseCase = streamSendMessageUseCase,
            memoryRetrievalService = memoryRetrievalService,
            steppedThinkingUseCase = mock(),
            vectorMemoryManagementUseCase = mock(),
            detectEmotionUseCase = mock(),
            messageRepository = mock(),
            memoryManagementUseCase = mock(),
            memorySourceWriter = mock(),
            reflectionJobScheduler = mock(),
            memoryLayerManager = memoryLayerManager,
            emotionalEngine = mockEmotionalEngine,
            personalityManager = personalityManager,
            contextManager = mock {
                on { currentTopics } doReturn MutableStateFlow(emptyList())
                on { conversationRounds } doReturn MutableStateFlow(0)
            },
            voiceManager = mock {
                on { recognitionResult } doReturn MutableStateFlow(null)
            },
            settingsManager = mockSettingsManager,
            networkMonitor = networkMonitor,
            onlineStatusManager = mock(),
            visionManager = mock(),
            tagDao = mock(),
            timeContextManager = mock {
                onBlocking { getTimeContext(any(), any()) } doReturn TimeContext(
                    currentTime = LocalDateTime.now(),
                    lastInteractionTime = null,
                    intervalDuration = null,
                    todayInteractionCount = 0,
                    weeklyInteractionCount = 0,
                    timePeriod = TimePeriod.AFTERNOON,
                    dayOfWeek = DayOfWeek.MONDAY,
                    isWeekend = false
                )
            },
            eventTracker = mock {
                onBlocking { getEventContext(any(), any()) } doReturn EventContext(
                    recentEvents = emptyList(),
                    messageRhythm = MessageRhythm(
                        recentMessageCount = 0,
                        averageInterval = 0L,
                        isRapidFire = false,
                        isPaused = false,
                        urgencyLevel = UrgencyLevel.NORMAL
                    ),
                    userState = UserState(
                        isActive = true,
                        urgencyLevel = UrgencyLevel.NORMAL,
                        attentionLevel = AttentionLevel.FOCUSED
                    )
                )
            },
            customCharacterRepository = mock(),
            characterCatalog = characterCatalog,
            currentUserProvider = mock {
                onBlocking { requireUserId() } doReturn testUserId
                on { userId } doReturn flowOf(testUserId)
            },
            characterPromptResolver = characterPromptResolver,
            typingStateManager = typingStateManager,
            characterReplyAssembler = mock {
                // 保持旧语义：透传角色 system prompt；若有 onTrace 则回调，模拟真实召回后上报
                onBlocking { assemble(any(), any(), any(), any(), any(), any(), any()) } doAnswer { invocation ->
                    val base = invocation.getArgument<String>(0)
                    val onTrace = invocation.getArgument<(String) -> Unit>(5)
                    onTrace("trace-1")
                    base
                }
            },
            innerStateRepository = mock(),
            applicationScope = applicationScope
        )
    }

    /** 供"召回失败"场景使用：让 mock 组装器触发 onRetrievalError。 */
    private fun installFailingRetrievalAssembler() {
        val failingAssembler = mock<com.companion.cc.domain.character.CharacterReplyAssembler> {
            onBlocking { assemble(any(), any(), any(), any(), any(), any(), any()) } doAnswer { invocation ->
                val base = invocation.getArgument<String>(0)
                val onRetrievalError = invocation.getArgument<(Throwable) -> Unit>(6)
                onRetrievalError(IllegalStateException("retrieval unavailable"))
                base
            }
        }
        val field = ChatViewModel::class.java.getDeclaredField("characterReplyAssembler")
        field.isAccessible = true
        field.set(viewModel, failingAssembler)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setCharacter - success resolves and caches config`() = runTest(testDispatcher) {
        // 订阅状态流以避免 WhileSubscribed 陷阱
        val job = viewModel.currentCharacter.launchIn(this)

        val mockCharacter = ChatCharacter.Custom(
            id = testCharacterId,
            name = "Test Character",
            avatar = null,
            description = "Test",
            personality = "Test",
            userId = testUserId
        )
        val mockConfig = CompanionConfig(
            id = testCharacterId,
            name = "Test Character",
            emoji = "✨",
            avatar = "",
            enabled = true,
            prompts = Prompts(
                system = "Test system prompt",
                greeting = listOf("Hello"),
                farewell = listOf("Bye"),
                fallback = listOf("Sorry")
            ),
            personality = PersonalityConfig(
                coreTraits = listOf("friendly"),
                background = "Test background",
                speakingStyle = SpeakingStyle(
                    tone = "friendly",
                    vocabularyLevel = "casual",
                    sentenceLength = "medium",
                    useEmoji = true,
                    useExclamation = true,
                    formality = "casual"
                ),
                interests = emptyList(),
                values = emptyList(),
                relationship = Relationship(
                    role = "companion",
                    distance = "moderate",
                    interactionStyle = "caring",
                    addressUser = "you"
                ),
                behaviorPatterns = emptyList()
            ),
            emotionalModel = EmotionalModelConfig(
                defaultMood = Mood.CALM,
                moodStability = 0.7f,
                energyRecoveryRate = 0.1f,
                stressThreshold = 0.7f,
                affectionGrowthRate = 0.05f,
                moodTransitions = emptyMap()
            ),
            memoryPreferences = MemoryPreferences(
                importanceThreshold = 0.5f,
                summaryFrequency = "daily",
                rememberTopics = emptyList(),
                forgetTopics = emptyList()
            ),
            apiParameters = ApiParametersConfig(
                temperature = 0.75f,
                topP = 0.85f,
                maxTokens = 1500
            )
        )
        val mockResolved = ResolvedCharacterPrompt(
            config = mockConfig,
            source = CharacterPromptSource.Custom
        )

        whenever(characterCatalog.getCharacter(testCharacterId)).thenReturn(mockCharacter)
        whenever(characterPromptResolver.resolve(testCharacterId)).thenReturn(mockResolved)

        viewModel.setCharacter(testCharacterId)
        runCurrent()
        Thread.sleep(100)
        runCurrent()

        verify(characterPromptResolver).resolve(testCharacterId)
        verify(personalityManager).cacheResolvedConfig(mockConfig)
        verify(memoryLayerManager).invalidateCharacter(testUserId, testCharacterId)

        job.cancel()
    }

    @Test
    fun `setCharacter - UnknownCharacterException sets CharacterNotFound error`() = runTest(testDispatcher) {
        // 订阅状态流
        val characterJob = viewModel.currentCharacter.launchIn(this)
        val errorJob = viewModel.error.launchIn(this)

        val mockCharacter = ChatCharacter.Custom(
            id = testCharacterId,
            name = "Test",
            avatar = null,
            description = "Test",
            personality = "Test",
            userId = testUserId
        )

        whenever(characterCatalog.getCharacter(testCharacterId)).thenReturn(mockCharacter)
        whenever(characterPromptResolver.resolve(testCharacterId))
            .thenThrow(UnknownCharacterException(testCharacterId))

        viewModel.setCharacter(testCharacterId)
        runCurrent()
        Thread.sleep(100)
        runCurrent()

        val error = viewModel.error.value
        assertEquals(ChatError.CharacterNotFound, error)

        characterJob.cancel()
        errorJob.cancel()
    }

    @Test
    fun `setCharacter - catalog returns null sets CharacterNotFound`() = runTest(testDispatcher) {
        val characterJob = viewModel.currentCharacter.launchIn(this)
        val errorJob = viewModel.error.launchIn(this)

        whenever(characterCatalog.getCharacter(testCharacterId)).thenReturn(null)

        viewModel.setCharacter(testCharacterId)
        runCurrent()
        Thread.sleep(100)
        runCurrent()

        assertEquals(ChatError.CharacterNotFound, viewModel.error.value)

        characterJob.cancel()
        errorJob.cancel()
    }

    @Test
    fun `stopSending preserves partial response and clears runtime flags`() = runTest(testDispatcher) {
        whenever(networkMonitor.isOnline()).thenReturn(true)
        whenever(characterPromptResolver.resolve(testCharacterId)).thenReturn(
            ResolvedCharacterPrompt(
                config = testConfig(),
                source = CharacterPromptSource.Custom
            )
        )
        whenever(memoryLayerManager.getMemoryContext(any(), any(), any())).thenReturn(
            CompleteMemoryContext(emptyList(), emptyList(), emptyList(), emptyList())
        )
        whenever(personalityManager.generateSystemPrompt(any(), any(), any())).thenReturn("prompt")
        whenever(streamSendMessageUseCase(any(), any(), any(), any(), any())).thenReturn(
            kotlinx.coroutines.flow.flow {
                emit("部分回复")
                kotlinx.coroutines.awaitCancellation()
            }
        )

        viewModel.sendMessage(testCharacterId, "hello")
        advanceUntilIdle()
        viewModel.stopSending()
        advanceUntilIdle()

        assertEquals(
            ChatSendState.Cancelled(testCharacterId, "部分回复"),
            viewModel.sendState.value
        )
        assertFalse(viewModel.isLoading.value)
        assertEquals(null, viewModel.activeStream.value)
        assertTrue(viewModel.messages.value.none { it.role == MessageRole.ASSISTANT })
        verify(typingStateManager).stopTyping(testCharacterId)
    }

    @Test
    fun `stopSending preserves partial image response and clears runtime flags`() = runTest(testDispatcher) {
        whenever(networkMonitor.isOnline()).thenReturn(true)
        whenever(characterPromptResolver.resolve(testCharacterId)).thenReturn(
            ResolvedCharacterPrompt(
                config = testConfig(),
                source = CharacterPromptSource.Custom
            )
        )
        whenever(memoryLayerManager.getMemoryContext(any(), any(), any())).thenReturn(
            CompleteMemoryContext(emptyList(), emptyList(), emptyList(), emptyList())
        )
        whenever(personalityManager.generateSystemPrompt(any(), any(), any())).thenReturn("prompt")
        whenever(streamSendMessageUseCase(any(), any(), any(), any(), any())).thenReturn(
            kotlinx.coroutines.flow.flow {
                emit("图片部分回复")
                kotlinx.coroutines.awaitCancellation()
            }
        )

        viewModel.sendMessageWithImage(
            testCharacterId,
            "hello",
            android.net.Uri.parse("content://test/image")
        )
        advanceUntilIdle()
        viewModel.stopSending()
        advanceUntilIdle()

        assertEquals(
            ChatSendState.Cancelled(testCharacterId, "图片部分回复"),
            viewModel.sendState.value
        )
        assertFalse(viewModel.isLoading.value)
        assertEquals(null, viewModel.activeStream.value)
        assertTrue(viewModel.messages.value.none { it.role == MessageRole.ASSISTANT })
        verify(typingStateManager).stopTyping(testCharacterId)
    }

    @Test
    fun `image stream failure reports retryable state and clears runtime flags`() = runTest(testDispatcher) {
        whenever(networkMonitor.isOnline()).thenReturn(true)
        whenever(characterPromptResolver.resolve(testCharacterId)).thenReturn(
            ResolvedCharacterPrompt(
                config = testConfig(),
                source = CharacterPromptSource.Custom
            )
        )
        whenever(memoryLayerManager.getMemoryContext(any(), any(), any())).thenReturn(
            CompleteMemoryContext(emptyList(), emptyList(), emptyList(), emptyList())
        )
        whenever(personalityManager.generateSystemPrompt(any(), any(), any())).thenReturn("prompt")
        whenever(streamSendMessageUseCase(any(), any(), any(), any(), any())).thenReturn(
            kotlinx.coroutines.flow.flow {
                throw IllegalStateException("service unavailable")
            }
        )

        viewModel.sendMessageWithImage(
            testCharacterId,
            "hello",
            android.net.Uri.parse("content://test/image")
        )
        advanceUntilIdle()

        assertEquals(
            ChatSendState.RetryableFailure(
                companionId = testCharacterId,
                partialResponse = "",
                message = "service unavailable"
            ),
            viewModel.sendState.value
        )
        assertFalse(viewModel.isLoading.value)
        assertEquals(null, viewModel.activeStream.value)
        assertTrue(viewModel.messages.value.none { it.role == MessageRole.ASSISTANT })
        verify(typingStateManager).stopTyping(testCharacterId)
    }

    @Test
    fun `successful retrieval exposes its trace for inspection`() = runTest(testDispatcher) {
        whenever(networkMonitor.isOnline()).thenReturn(true)
        whenever(characterPromptResolver.resolve(testCharacterId)).thenReturn(
            ResolvedCharacterPrompt(testConfig(), CharacterPromptSource.Custom)
        )
        whenever(memoryLayerManager.getMemoryContext(any(), any(), any())).thenReturn(
            CompleteMemoryContext(emptyList(), emptyList(), emptyList(), emptyList())
        )
        whenever(personalityManager.generateSystemPrompt(any(), any(), any())).thenReturn("prompt")
        whenever(memoryRetrievalService.retrieve(any(), any(), any())).thenReturn(
            com.companion.cc.domain.memory.RetrievalResult("trace-1", emptyList())
        )
        whenever(streamSendMessageUseCase(any(), any(), any(), any(), any())).thenReturn(flowOf("reply"))

        viewModel.sendMessage(testCharacterId, "hello")
        advanceUntilIdle()

        assertEquals(
            MemoryRetrievalTraceReference(testCharacterId, "trace-1"),
            viewModel.latestMemoryRetrievalTrace.value
        )
    }

    @Test
    fun `memory retrieval failure keeps chat request usable and exposes diagnostic warning`() = runTest(testDispatcher) {
        whenever(networkMonitor.isOnline()).thenReturn(true)
        whenever(characterPromptResolver.resolve(testCharacterId)).thenReturn(
            ResolvedCharacterPrompt(
                config = testConfig(),
                source = CharacterPromptSource.Custom
            )
        )
        whenever(memoryLayerManager.getMemoryContext(any(), any(), any())).thenReturn(
            CompleteMemoryContext(emptyList(), emptyList(), emptyList(), emptyList())
        )
        whenever(personalityManager.generateSystemPrompt(any(), any(), any())).thenReturn("prompt")
        whenever(streamSendMessageUseCase(any(), any(), any(), any(), any())).thenReturn(
            kotlinx.coroutines.flow.emptyFlow()
        )
        whenever(memoryRetrievalService.retrieve(any(), any(), any()))
            .thenThrow(IllegalStateException("retrieval unavailable"))
        installFailingRetrievalAssembler()

        viewModel.sendMessage(testCharacterId, "hello")
        advanceUntilIdle()

        assertEquals(
            ChatError.UnknownError(
                message = "Memory retrieval unavailable: retrieval unavailable",
                userMessage = "长期记忆暂不可用，本次回复未使用长期记忆",
                canRetry = false
            ),
            viewModel.error.value
        )
        verify(streamSendMessageUseCase)(any(), any(), any(), any(), any())
    }

    @Test
    fun `successful text send returns to idle and clears loading`() = runTest(testDispatcher) {
        whenever(networkMonitor.isOnline()).thenReturn(true)
        whenever(characterPromptResolver.resolve(testCharacterId)).thenReturn(
            ResolvedCharacterPrompt(testConfig(), CharacterPromptSource.Custom)
        )
        whenever(memoryLayerManager.getMemoryContext(any(), any(), any())).thenReturn(
            CompleteMemoryContext(emptyList(), emptyList(), emptyList(), emptyList())
        )
        whenever(personalityManager.generateSystemPrompt(any(), any(), any())).thenReturn("prompt")
        whenever(personalityManager.adjustResponse(any(), any())).thenAnswer { it.getArgument(0) }
        whenever(streamSendMessageUseCase(any(), any(), any(), any(), any())).thenReturn(flowOf("reply"))

        viewModel.sendMessage(testCharacterId, "hello")
        advanceUntilIdle()

        assertEquals(ChatSendState.Idle, viewModel.sendState.value)
        assertFalse(viewModel.isLoading.value)
    }
    private fun testConfig() = CompanionConfig(
        id = testCharacterId,
        name = "Test Character",
        emoji = "✨",
        avatar = "",
        enabled = true,
        prompts = Prompts(
            system = "Test system prompt",
            greeting = listOf("Hello"),
            farewell = listOf("Bye"),
            fallback = listOf("Sorry")
        ),
        personality = PersonalityConfig(
            coreTraits = listOf("friendly"),
            background = "Test background",
            speakingStyle = SpeakingStyle(
                tone = "friendly",
                vocabularyLevel = "casual",
                sentenceLength = "medium",
                useEmoji = true,
                useExclamation = true,
                formality = "casual"
            ),
            interests = emptyList(),
            values = emptyList(),
            relationship = Relationship(
                role = "companion",
                distance = "moderate",
                interactionStyle = "caring",
                addressUser = "you"
            ),
            behaviorPatterns = emptyList()
        ),
        emotionalModel = EmotionalModelConfig(
            defaultMood = Mood.CALM,
            moodStability = 0.7f,
            energyRecoveryRate = 0.1f,
            stressThreshold = 0.7f,
            affectionGrowthRate = 0.05f,
            moodTransitions = emptyMap()
        ),
        memoryPreferences = MemoryPreferences(
            importanceThreshold = 0.5f,
            summaryFrequency = "daily",
            rememberTopics = emptyList(),
            forgetTopics = emptyList()
        ),
        apiParameters = ApiParametersConfig(
            temperature = 0.75f,
            topP = 0.85f,
            maxTokens = 1500
        )
    )
}
