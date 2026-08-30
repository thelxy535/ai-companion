package com.companion.cc.ui.companion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.companion.cc.data.local.SettingsManager
import com.companion.cc.domain.character.CharacterCatalog
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.model.ChatCharacter
import com.companion.cc.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed interface CompanionDetailUiState {
    data object Loading : CompanionDetailUiState
    data object Missing : CompanionDetailUiState
    data class Content(
        val character: ChatCharacter,
        val avatar: String?,
        val messageCount: Int,
        val firstMetTimestamp: Long?
    ) : CompanionDetailUiState

    data class Failure(val message: String) : CompanionDetailUiState
}

@HiltViewModel
class CompanionDetailViewModel @Inject constructor(
    private val characterCatalog: CharacterCatalog,
    private val currentUserProvider: CurrentUserProvider,
    private val messageRepository: MessageRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {
    private val _uiState = MutableStateFlow<CompanionDetailUiState>(CompanionDetailUiState.Loading)
    val uiState: StateFlow<CompanionDetailUiState> = _uiState.asStateFlow()

    private var selectedCompanionId: String? = null
    private var loadJob: Job? = null

    fun setCompanion(companionId: String) {
        if (selectedCompanionId == companionId && loadJob?.isActive == true) return
        selectedCompanionId = companionId
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = CompanionDetailUiState.Loading
            try {
                val character = characterCatalog.getCharacter(companionId)
                    ?: run {
                        _uiState.value = CompanionDetailUiState.Missing
                        return@launch
                    }
                val userId = currentUserProvider.requireUserId()
                combine(
                    messageRepository.getMessages(userId, companionId, Int.MAX_VALUE, 0),
                    settingsManager.getCompanionAvatarFlow(companionId)
                ) { messages, avatar ->
                    CompanionDetailUiState.Content(
                        character = character,
                        avatar = avatar,
                        messageCount = messages.size,
                        firstMetTimestamp = messages.minOfOrNull { it.timestamp }
                    )
                }.collect { _uiState.value = it }
            } catch (error: Exception) {
                _uiState.value = CompanionDetailUiState.Failure(error.message ?: "无法加载角色详情")
            }
        }
    }

    fun saveAvatar(avatarUrl: String?) {
        val companionId = selectedCompanionId ?: return
        viewModelScope.launch {
            try {
                settingsManager.saveCompanionAvatar(companionId, avatarUrl)
            } catch (error: Exception) {
                _uiState.value = CompanionDetailUiState.Failure(error.message ?: "头像保存失败")
            }
        }
    }
}
