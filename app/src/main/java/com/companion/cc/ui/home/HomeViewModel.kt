package com.companion.cc.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.companion.cc.domain.character.CharacterCatalog
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.manager.OnlineStatusManager
import com.companion.cc.domain.model.ChatCharacter
import com.companion.cc.domain.model.Message
import com.companion.cc.domain.model.MessageRole
import com.companion.cc.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import javax.inject.Inject

/**
 * Home 页面 UI 状态
 */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Content(val items: List<HomeCharacterItem>) : HomeUiState
    data class Failure(val message: String) : HomeUiState
}

/**
 * Home 页面角色项
 */
data class HomeCharacterItem(
    val character: ChatCharacter,
    val lastMessage: Message?,
    val isOnline: Boolean,
    val hasUnreadMessage: Boolean = false,
)

internal fun hasUnreadAssistantMessage(message: Message?, readAt: Long): Boolean =
    message?.role == MessageRole.ASSISTANT &&
        message.timestamp > readAt

/**
 * Home 页面 ViewModel
 *
 * 响应式目录投影：从 CharacterCatalog 获取所有角色，
 * 结合每个角色的最新消息和在线状态，生成排序后的 UI 状态
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val currentUserProvider: CurrentUserProvider,
    val onlineStatusManager: OnlineStatusManager,
    private val characterCatalog: CharacterCatalog,
    private val settingsManager: com.companion.cc.data.local.SettingsManager,
) : ViewModel() {

    fun markConversationRead(companionId: String) {
        val lastMessage = (uiState.value as? HomeUiState.Content)
            ?.items
            ?.firstOrNull { it.character.id == companionId }
            ?.lastMessage
        viewModelScope.launch {
            val userId = currentUserProvider.requireUserId()
            settingsManager.saveConversationReadAt(
                userId = userId,
                companionId = companionId,
                // 以打开时刻为准，避免点击后异步回复恰好落库导致旧的“新消息”状态残留。
                timestamp = maxOf(lastMessage?.timestamp ?: 0L, System.currentTimeMillis())
            )
        }
    }

    /**
     * Home UI 状态
     *
     * 实现响应式投影：
     * 1. 从 CharacterCatalog 获取所有角色（内置+自定义）
     * 2. 为每个角色获取最新消息
     * 3. 结合在线状态
     * 4. 按最新消息时间倒序排序
     */
    val uiState: StateFlow<HomeUiState> = combine(
        currentUserProvider.userId,
        characterCatalog.observeCharacters(),
        onlineStatusManager.onlineCompanions,
    ) { userId, characters, online -> Triple(userId, characters, online) }
        .flatMapLatest { (userId, characters, online) ->
            if (userId.isBlank()) {
                flowOf(HomeUiState.Loading)
            } else if (characters.isEmpty()) {
                flowOf(HomeUiState.Content(emptyList()))
            } else {
                // 为每个角色获取最新消息，并结合成 HomeCharacterItem
                combine(characters.map { character ->
                    combine(
                        messageRepository.observeLatestMessage(userId, character.id),
                        settingsManager.conversationReadAtFlow(userId, character.id)
                    ) { lastMessage, readAt ->
                            HomeCharacterItem(
                                character = character,
                                lastMessage = lastMessage,
                                hasUnreadMessage = hasUnreadAssistantMessage(lastMessage, readAt),
                                // 在线 = 显式标记 或 5 分钟内活跃 或 内置角色常驻在线（V7 伴侣语义：AI 永远"在"）
                                isOnline = online.contains(character.id)
                                    || character.id in setOf("xiaocan", "muse")
                                    || (lastMessage?.timestamp?.let {
                                        System.currentTimeMillis() - it < 5 * 60 * 1000L
                                    } ?: false)
                            )
                    }
                }) { items ->
                    // 按最新消息时间倒序排序（没有消息的排在最后）
                    HomeUiState.Content(
                        items.sortedByDescending { it.lastMessage?.timestamp ?: 0L }
                    )
                }
            }
        }
        .catch { error ->
            emit(HomeUiState.Failure(error.message ?: "加载失败"))
        }
        .stateIn(
            scope = viewModelScope,
            // V7：Lazily 常驻内存——切 Tab 回来直接显示缓存数据，不再闪 Loading
            started = SharingStarted.Lazily,
            initialValue = HomeUiState.Loading
        )

    /**
     * 获取当前用户ID
     */
    /** V9PM：自定义头像覆盖（companionId -> url）——主页行与解析器共用 */
    val avatarOverrides: StateFlow<Map<String, String>> = settingsManager.companionAvatarOverridesFlow
        .stateIn(scope = viewModelScope, started = SharingStarted.Lazily, initialValue = emptyMap())

    suspend fun getUserId(): String {
        return currentUserProvider.requireUserId()
    }

    private var prewarmJob: Job? = null
    private val prewarmedCompanions = mutableSetOf<String>()

    fun prewarmMessageCaches(items: List<HomeCharacterItem>) {
        val companionIds = items.mapTo(linkedSetOf()) { it.character.id }
        if (companionIds.isEmpty()) return
        prewarmJob?.cancel()
        prewarmJob = viewModelScope.launch {
            val userId = getUserId()
            companionIds.forEach { cid ->
                currentCoroutineContext().ensureActive()
                if (cid in prewarmedCompanions || com.companion.cc.ui.chat.ChatMessageMemory.lastMessages[cid] != null) {
                    prewarmedCompanions += cid
                    return@forEach
                }
                runCatching {
                    messageRepository.getMessages(userId, cid).first()
                }.getOrNull()?.let { list ->
                    com.companion.cc.ui.chat.ChatMessageMemory.lastMessages[cid] = list
                    prewarmedCompanions += cid
                }
            }
        }
    }
}
