package com.companion.cc.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.companion.cc.domain.character.CharacterCatalog
import com.companion.cc.domain.character.CharacterDeletionResult
import com.companion.cc.domain.character.CharacterDeletionService
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.model.ChatCharacter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CharacterFarewellState {
    data object Idle : CharacterFarewellState
    data object Loading : CharacterFarewellState
    data class Ready(val character: ChatCharacter.Custom) : CharacterFarewellState
    data object Deleting : CharacterFarewellState
    data class Deleted(val result: CharacterDeletionResult) : CharacterFarewellState
    data class Failure(val message: String) : CharacterFarewellState
}

@HiltViewModel
class CharacterFarewellViewModel @Inject constructor(
    private val deletionService: CharacterDeletionService,
    private val characterCatalog: CharacterCatalog,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {
    private val _state = MutableStateFlow<CharacterFarewellState>(CharacterFarewellState.Idle)
    val state: StateFlow<CharacterFarewellState> = _state.asStateFlow()

    private var characterId: String? = null

    fun load(characterId: String) {
        this.characterId = characterId
        viewModelScope.launch {
            _state.value = CharacterFarewellState.Loading
            _state.value = try {
                when (val character = characterCatalog.getCharacter(characterId)) {
                    is ChatCharacter.Custom -> CharacterFarewellState.Ready(character)
                    else -> CharacterFarewellState.Failure("只能删除自定义角色")
                }
            } catch (error: Exception) {
                CharacterFarewellState.Failure("加载角色失败: ${error.message}")
            }
        }
    }

    fun confirmDeletion() {
        val ready = _state.value as? CharacterFarewellState.Ready ?: return
        if (_state.value is CharacterFarewellState.Deleting) return

        viewModelScope.launch {
            _state.value = CharacterFarewellState.Deleting
            _state.value = try {
                val userId = currentUserProvider.requireUserId()
                val id = characterId ?: ready.character.id
                CharacterFarewellState.Deleted(
                    deletionService.delete(userId, id).getOrThrow()
                )
            } catch (error: Exception) {
                CharacterFarewellState.Failure("删除角色失败: ${error.message}")
            }
        }
    }
}
