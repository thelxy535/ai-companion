package com.companion.cc.ui.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.companion.cc.data.local.entity.MemoryNodeEntity
import com.companion.cc.data.local.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemoryLibraryViewModel @Inject constructor(
    private val repository: MemoryRepository
) : ViewModel() {
    private val scopeKey = MutableStateFlow(MemoryReviewViewModel.scopeFor("xiaocan"))
    private val kind = MutableStateFlow("")
    private val query = MutableStateFlow("")
    val queryText: StateFlow<String> = query.asStateFlow()
    private val _nodes = MutableStateFlow<List<MemoryNodeEntity>>(emptyList())
    val nodes: StateFlow<List<MemoryNodeEntity>> = _nodes.asStateFlow()

    init {
        viewModelScope.launch {
            combine(scopeKey, kind, query) { scope, memoryKind, search -> Triple(scope, memoryKind, search) }
                .flatMapLatest { (scope, memoryKind, search) -> repository.observeNodes(scope, memoryKind, search) }
                .collect { _nodes.value = it }
        }
    }

    fun setCompanion(companionId: String) { scopeKey.value = MemoryReviewViewModel.scopeFor(companionId) }
    fun setKind(value: String) { kind.value = normalizeKind(value) }
    fun setQuery(value: String) { query.value = value.trim() }

    companion object {
        fun normalizeKind(value: String): String = if (value == "全部") "" else value
    }
}
