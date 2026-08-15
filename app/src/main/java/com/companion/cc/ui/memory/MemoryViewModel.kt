package com.companion.cc.ui.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.companion.cc.domain.manager.MemoryLayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val memoryLayerManager: MemoryLayerManager
) : ViewModel() {

    private val _memories = MutableStateFlow(
        com.companion.cc.domain.model.CompleteMemoryContext(
            shortTerm = emptyList(),
            midTerm = emptyList(),
            longTerm = emptyList(),
            permanent = emptyList()
        )
    )
    val memories: StateFlow<com.companion.cc.domain.model.CompleteMemoryContext> = _memories.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadMemories(companionId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val context = memoryLayerManager.getMemoryContext(
                    userId = "default",
                    companionId = companionId,
                    currentMessage = ""
                )
                _memories.value = context
            } catch (e: Exception) {
                android.util.Log.e("MemoryViewModel", "加载记忆失败", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshMemories(companionId: String) {
        loadMemories(companionId)
    }
}
