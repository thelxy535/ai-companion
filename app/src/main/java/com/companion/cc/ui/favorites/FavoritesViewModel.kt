package com.companion.cc.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.model.Message
import com.companion.cc.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FavoritesUiState {
    data object Loading : FavoritesUiState
    data object Empty : FavoritesUiState
    data class Content(val messages: List<Message>) : FavoritesUiState
    data class Failure(val message: String) : FavoritesUiState
}

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val currentUserProvider: CurrentUserProvider,
    private val messageRepository: MessageRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<FavoritesUiState>(FavoritesUiState.Loading)
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private var loadJob: Job? = null

    fun setCompanion(companionId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = FavoritesUiState.Loading
            try {
                val userId = currentUserProvider.requireUserId()
                messageRepository.getFavoritedMessages(userId).collect { messages ->
                    val filtered = messages.filter { it.companionId == companionId }
                    _uiState.value = if (filtered.isEmpty()) {
                        FavoritesUiState.Empty
                    } else {
                        FavoritesUiState.Content(filtered)
                    }
                }
            } catch (error: Exception) {
                _uiState.value = FavoritesUiState.Failure(error.message ?: "无法加载珍藏消息")
            }
        }
    }

    fun removeFavorite(messageId: String) {
        viewModelScope.launch {
            try {
                messageRepository.toggleMessageFavorite(messageId, false)
            } catch (error: Exception) {
                _actionError.value = error.message ?: "移出珍藏失败"
            }
        }
    }

    fun clearActionError() {
        _actionError.value = null
    }
}
