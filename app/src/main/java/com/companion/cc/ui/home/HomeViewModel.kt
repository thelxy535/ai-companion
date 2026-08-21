package com.companion.cc.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.companion.cc.domain.character.CharacterCatalog
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.manager.OnlineStatusManager
import com.companion.cc.domain.model.ChatCharacter
import com.companion.cc.domain.model.Message
import com.companion.cc.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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
)

/**
 * Home 页面 ViewModel
 *
 * 响应式目录投影：从 CharacterCatalog 获取所有角色，
 * 结合每个角色的最新消息和在线状态，生成排序后的 UI 状态
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val currentUserProvider: CurrentUserProvider,
    val onlineStatusManager: OnlineStatusManager,
    private val characterCatalog: CharacterCatalog
) : ViewModel() {

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
            if (userId == null) {
                flowOf(HomeUiState.Loading)
            } else if (characters.isEmpty()) {
                flowOf(HomeUiState.Content(emptyList()))
            } else {
                // 为每个角色获取最新消息，并结合成 HomeCharacterItem
                combine(characters.map { character ->
                    messageRepository.observeLatestMessage(userId, character.id)
                        .map { lastMessage ->
                            HomeCharacterItem(
                                character = character,
                                lastMessage = lastMessage,
                                isOnline = online.contains(character.id)
                            )
                        }
                }) { items ->
                    // 按最新消息时间倒序排序（没有消息的排在最后）
                    HomeUiState.Content(
                        items.sortedByDescending { it.lastMessage?.timestamp ?: 0L }
                    ) as HomeUiState
                }
            }
        }
        .catch { error ->
            emit(HomeUiState.Failure(error.message ?: "加载失败"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState.Loading
        )

    /**
     * 获取当前用户ID
     */
    suspend fun getUserId(): String {
        return currentUserProvider.requireUserId()
    }
}
