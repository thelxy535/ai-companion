package com.companion.cc.ui.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.companion.cc.data.local.entity.MemoryNodeEntity
import com.companion.cc.data.local.repository.MemoryRepository
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.memory.MemoryScopeKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MemoryLibraryViewModel @Inject constructor(
    private val repository: MemoryRepository,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {
    private val scopeKey = MutableStateFlow<String?>(null)
    private val kind = MutableStateFlow("")
    private val query = MutableStateFlow("")
    private val status = MutableStateFlow("active")
    val queryText: StateFlow<String> = query.asStateFlow()
    val selectedStatus: StateFlow<String> = status.asStateFlow()
    private val _nodes = MutableStateFlow<List<MemoryNodeEntity>>(emptyList())
    val nodes: StateFlow<List<MemoryNodeEntity>> = _nodes.asStateFlow()

    init {
        viewModelScope.launch {
            combine(scopeKey, kind, query, status) { scope, memoryKind, search, memoryStatus ->
                LibraryQuery(scope, memoryKind, search, memoryStatus)
            }
                .flatMapLatest { request ->
                    request.scopeKey?.let {
                        repository.observeNodes(
                            scopeKey = it,
                            kind = request.kind,
                            query = request.query,
                            status = request.status
                        )
                    } ?: emptyFlow()
                }
                .collect { _nodes.value = it }
        }
    }

    fun setCompanion(companionId: String) {
        viewModelScope.launch {
            val userId = runCatching { currentUserProvider.requireUserId() }.getOrNull()
            scopeKey.value = userId?.let { MemoryScopeKey.forCharacter(it, companionId) }
        }
    }
    fun setKind(value: String) { kind.value = normalizeKind(value) }
    fun setQuery(value: String) { query.value = value.trim() }
    fun setStatus(value: String) { status.value = normalizeStatus(value) }

    private data class LibraryQuery(
        val scopeKey: String?,
        val kind: String,
        val query: String,
        val status: String
    )

    companion object {
        fun normalizeKind(value: String): String = if (value == "全部") "" else value
        fun normalizeStatus(value: String): String = when (value) {
            "已禁止召回" -> "do_not_recall"
            "已删除" -> "deleted"
            else -> "active"
        }
    }
}
