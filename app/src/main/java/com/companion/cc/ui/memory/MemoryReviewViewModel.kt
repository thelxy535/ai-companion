package com.companion.cc.ui.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.companion.cc.data.local.entity.MemoryReviewEntity
import com.companion.cc.data.local.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MemoryReviewState {
    data object Loading : MemoryReviewState
    data class Content(val reviews: List<MemoryReviewEntity>) : MemoryReviewState
    data class Error(val previous: List<MemoryReviewEntity>, val message: String) : MemoryReviewState
}

@HiltViewModel
class MemoryReviewViewModel @Inject constructor(
    private val repository: MemoryRepository
) : ViewModel() {
    private val _state = MutableStateFlow<MemoryReviewState>(MemoryReviewState.Loading)
    val state: StateFlow<MemoryReviewState> = _state.asStateFlow()
    private var scopeKey: String = scopeFor("xiaocan")

    init { observe() }

    fun setCompanion(companionId: String) {
        val next = scopeFor(companionId)
        if (next != scopeKey) {
            scopeKey = next
            observe()
        }
    }

    fun accept(review: MemoryReviewEntity, subjectRole: String = "user", subjectKey: String = "user") {
        viewModelScope.launch {
            repository.acceptReview(review.id, subjectRole, subjectKey)
                .onFailure { error -> _state.value = MemoryReviewState.Error(currentItems(), error.message ?: "无法接受记忆") }
        }
    }

    fun reject(review: MemoryReviewEntity) {
        resolve(review, "rejected", "user rejected")
    }

    fun defer(review: MemoryReviewEntity) {
        resolve(review, "deferred", "deferred by user")
    }

    private fun resolve(review: MemoryReviewEntity, status: String, note: String) {
        viewModelScope.launch {
            repository.resolveReview(review.id, status, note)
                .onFailure { error -> _state.value = MemoryReviewState.Error(currentItems(), error.message ?: "无法更新审核状态") }
        }
    }

    private fun observe() {
        viewModelScope.launch {
            repository.observePendingReviews(scopeKey)
                .catch { error -> _state.value = MemoryReviewState.Error(currentItems(), error.message ?: "无法加载审核列表") }
                .collect { reviews -> _state.value = MemoryReviewState.Content(reviews) }
        }
    }

    private fun currentItems(): List<MemoryReviewEntity> = when (val value = _state.value) {
        is MemoryReviewState.Content -> value.reviews
        is MemoryReviewState.Error -> value.previous
        MemoryReviewState.Loading -> emptyList()
    }

    companion object {
        fun scopeFor(companionId: String): String = "companion:${companionId.trim()}"
    }
}
