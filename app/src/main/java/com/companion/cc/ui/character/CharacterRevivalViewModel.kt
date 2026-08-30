package com.companion.cc.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.companion.cc.domain.character.CharacterRevivalMode
import com.companion.cc.domain.character.CharacterRevivalService
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.model.CustomCharacter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CharacterRevivalState {
    data object Idle : CharacterRevivalState
    data object Reviving : CharacterRevivalState
    data class Success(val character: CustomCharacter) : CharacterRevivalState
    data class Failure(val message: String) : CharacterRevivalState
}

@HiltViewModel
class CharacterRevivalViewModel @Inject constructor(
    private val revivalService: CharacterRevivalService,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {
    private val _state = MutableStateFlow<CharacterRevivalState>(CharacterRevivalState.Idle)
    val state: StateFlow<CharacterRevivalState> = _state.asStateFlow()

    fun revive(token: String, mode: CharacterRevivalMode) {
        if (_state.value is CharacterRevivalState.Reviving) return
        viewModelScope.launch {
            _state.value = CharacterRevivalState.Reviving
            _state.value = try {
                val userId = currentUserProvider.requireUserId()
                CharacterRevivalState.Success(
                    revivalService.revive(userId, token.trim(), mode).getOrThrow()
                )
            } catch (error: Exception) {
                CharacterRevivalState.Failure("恢复失败: ${error.message}")
            }
        }
    }
}
