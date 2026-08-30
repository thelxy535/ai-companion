package com.companion.cc.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.companion.cc.domain.character.CharacterMemoryCapsuleRepository
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.data.local.entity.CharacterMemoryCapsuleEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CapsuleSummary(
    val id: String,
    val sourceCharacterId: String,
    val createdAt: Long,
    val status: String
)

sealed interface MemoryCapsuleVaultState {
    data object Idle : MemoryCapsuleVaultState
    data object Loading : MemoryCapsuleVaultState
    data class Ready(val capsules: List<CapsuleSummary>) : MemoryCapsuleVaultState
    data class Failure(val message: String) : MemoryCapsuleVaultState
}

@HiltViewModel
class MemoryCapsuleVaultViewModel @Inject constructor(
    private val capsules: CharacterMemoryCapsuleRepository,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {
    private val _state = MutableStateFlow<MemoryCapsuleVaultState>(MemoryCapsuleVaultState.Idle)
    val state: StateFlow<MemoryCapsuleVaultState> = _state.asStateFlow()

    private val _visibleCapsules = MutableStateFlow<List<CapsuleSummary>>(emptyList())
    val visibleCapsules: StateFlow<List<CapsuleSummary>> = _visibleCapsules.asStateFlow()

    private var observeJob: Job? = null

    fun load() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _state.value = MemoryCapsuleVaultState.Loading
            try {
                val userId = currentUserProvider.requireUserId()
                capsules.observe(userId).collect { records ->
                    val summaries = records.map { it.toSummary() }
                    _visibleCapsules.value = summaries
                    _state.value = MemoryCapsuleVaultState.Ready(summaries)
                }
            } catch (error: Exception) {
                _visibleCapsules.value = emptyList()
                _state.value = MemoryCapsuleVaultState.Failure("加载胶囊失败: ${error.message}")
            }
        }
    }
}

private fun CharacterMemoryCapsuleEntity.toSummary() = CapsuleSummary(
    id = id,
    sourceCharacterId = sourceCharacterId,
    createdAt = createdAt,
    status = status
)
