package com.companion.cc.ui.memory

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.companion.cc.data.local.entity.MemoryRetrievalFeedbackEntity
import com.companion.cc.data.local.repository.MemoryRepository
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.memory.MemoryScopeKey
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemoryRetrievalTraceSelection(
    val nodeId: String,
    val explanation: String
)

data class MemoryRetrievalTraceState(
    val traceId: String? = null,
    val query: String = "",
    val durationMs: Long = 0L,
    val selections: List<MemoryRetrievalTraceSelection> = emptyList()
)

@HiltViewModel
class MemoryRetrievalTraceViewModel @Inject constructor(
    private val repository: MemoryRepository,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {
    var state by mutableStateOf(MemoryRetrievalTraceState())
        private set
    var actionError by mutableStateOf<String?>(null)
        private set

    private var loadedCompanionId: String? = null

    fun load(companionId: String, traceId: String) {
        loadedCompanionId = companionId
        viewModelScope.launch {
            val userId = runCatching { currentUserProvider.requireUserId() }.getOrNull()
            if (userId == null) {
                state = MemoryRetrievalTraceState()
                return@launch
            }
            val trace = repository.getRetrievalTrace(
                MemoryScopeKey.forCharacter(userId, companionId),
                traceId
            ) ?: run {
                state = MemoryRetrievalTraceState()
                return@launch
            }
            state = MemoryRetrievalTraceState(
                traceId = trace.id,
                query = trace.query,
                durationMs = trace.durationMs,
                selections = parseSelections(trace.selectedNodeIdsJson, trace.explanationJson)
            )
        }
    }

    fun submitFeedback(
        nodeId: String,
        feedback: String,
        note: String = "",
        now: Long = System.currentTimeMillis()
    ) {
        val companionId = loadedCompanionId ?: return
        val traceId = state.traceId ?: return
        if (state.selections.none { it.nodeId == nodeId }) return
        viewModelScope.launch {
            val userId = runCatching { currentUserProvider.requireUserId() }.getOrNull() ?: return@launch
            repository.recordFeedback(
                MemoryScopeKey.forCharacter(userId, companionId),
                MemoryRetrievalFeedbackEntity(
                    traceId = traceId,
                    nodeId = nodeId,
                    feedback = feedback,
                    note = note,
                    createdAt = now
                )
            ).fold(
                onSuccess = { actionError = null },
                onFailure = { error ->
                    actionError = error.message ?: "记忆反馈失败，请重试"
                }
            )
        }
    }

    private fun parseSelections(
        selectedNodeIdsJson: String,
        explanationJson: String
    ): List<MemoryRetrievalTraceSelection> {
        val nodeIds = runCatching {
            Gson().fromJson(selectedNodeIdsJson, Array<String>::class.java)?.toList().orEmpty()
        }.getOrDefault(emptyList())
        val explanations = runCatching {
            Gson().fromJson(explanationJson, Array<String>::class.java)?.toList().orEmpty()
        }.getOrDefault(emptyList())
        return nodeIds.mapIndexed { index, nodeId ->
            MemoryRetrievalTraceSelection(nodeId, explanations.getOrElse(index) { "" })
        }
    }
}
