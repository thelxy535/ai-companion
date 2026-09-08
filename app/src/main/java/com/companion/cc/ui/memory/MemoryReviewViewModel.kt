package com.companion.cc.ui.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.companion.cc.data.local.entity.MemoryReviewEntity
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.data.local.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import com.companion.cc.domain.memory.ReviewInboxGroup
import com.companion.cc.domain.memory.ReviewInboxPolicy

sealed interface MemoryReviewState {
    data object Loading : MemoryReviewState
    data class Content(
        val reviews: List<MemoryReviewEntity>,
        val deferred: List<MemoryReviewEntity> = emptyList()
    ) : MemoryReviewState
    data class Error(val previous: List<MemoryReviewEntity>, val message: String) : MemoryReviewState
}

@HiltViewModel
class MemoryReviewViewModel @Inject constructor(
    private val repository: MemoryRepository,
    private val currentUserProvider: CurrentUserProvider,
    private val commitmentRepository: com.companion.cc.domain.commitment.CommitmentRepository
) : ViewModel() {
    private val _state = MutableStateFlow<MemoryReviewState>(MemoryReviewState.Loading)
    val state: StateFlow<MemoryReviewState> = _state.asStateFlow()
    private var scopeKey: String? = null
    private var observeJob: Job? = null
    private val mutationMutex = Mutex()

    fun setCompanion(companionId: String) {
        viewModelScope.launch {
            val userId = runCatching { currentUserProvider.requireUserId() }
                .getOrElse { error ->
                    _state.value = MemoryReviewState.Error(emptyList(), error.message ?: "无法识别当前用户")
                    return@launch
                }
            val next = scopeFor(userId, companionId)
            if (next != scopeKey) {
                scopeKey = next
                observe()
            }
        }
    }

    fun accept(review: MemoryReviewEntity, subjectRole: String = "user", subjectKey: String = "user") {
        viewModelScope.launch {
            runMutation { acceptOne(review, subjectRole, subjectKey) }
        }
    }

    fun acceptGroup(group: ReviewInboxGroup) = acceptAll(listOf(group))
    fun rejectGroup(group: ReviewInboxGroup) = rejectAll(listOf(group))
    fun deferGroup(group: ReviewInboxGroup) = deferAll(listOf(group))

    fun acceptAll(groups: List<ReviewInboxGroup>) = batch(groups.flatMap { it.items }.filter(ReviewInboxPolicy::isBatchable)) {
        acceptOne(it, "user", "user")
    }

    fun rejectAll(groups: List<ReviewInboxGroup>) = batch(groups.flatMap { it.items }.filter(ReviewInboxPolicy::isBatchable)) {
        resolveOne(it, "rejected", "user rejected")
    }

    fun deferAll(groups: List<ReviewInboxGroup>) = batch(groups.flatMap { it.items }.filter(ReviewInboxPolicy::isBatchable)) {
        resolveOne(it, "deferred", "deferred by user")
    }

    private fun batch(reviews: List<MemoryReviewEntity>, action: suspend (MemoryReviewEntity) -> Unit) {
        viewModelScope.launch {
            runMutation {
                reviews.forEach { action(it) }
            }
        }
    }

    private suspend fun runMutation(action: suspend () -> Unit) {
        mutationMutex.withLock {
            runCatching { action() }
                .onFailure { error ->
                    _state.value = MemoryReviewState.Error(
                        currentItems(),
                        error.message ?: "无法更新记忆收件箱"
                    )
                }
        }
    }

    private suspend fun acceptOne(
        review: MemoryReviewEntity,
        subjectRole: String,
        subjectKey: String
    ) {
        val scope = scopeKey ?: run {
            _state.value = MemoryReviewState.Error(currentItems(), "未选择角色")
            return
        }
        val isNarrative = review.kind == com.companion.cc.domain.memory.NarrativeKinds.RELATIONSHIP
        repository.acceptReview(
            scopeKey = scope,
            reviewId = review.id,
            subjectRole = if (isNarrative) "relationship" else subjectRole,
            subjectKey = if (isNarrative) "pair" else subjectKey
        ).getOrThrow().also {
            if (review.kind == "commitment") {
                val parts = scope.split(":")
                if (parts.size >= 4 && parts[0] == "user") {
                    commitmentRepository.create(
                        userId = parts[1], characterId = parts[3], promise = review.content,
                        dueAt = System.currentTimeMillis() + 24L * 3_600_000L
                    )
                }
            }
        }
    }

    fun reject(review: MemoryReviewEntity) {
        resolve(review, "rejected", "user rejected")
    }

    fun defer(review: MemoryReviewEntity) {
        resolve(review, "deferred", "deferred by user")
    }

    fun restore(review: MemoryReviewEntity) {
        viewModelScope.launch {
            runMutation {
                val scope = requireNotNull(scopeKey) { "未选择角色" }
                repository.restoreReview(scope, review.id).getOrThrow()
            }
        }
    }

    private fun resolve(review: MemoryReviewEntity, status: String, note: String) {
        viewModelScope.launch {
            runMutation { resolveOne(review, status, note) }
        }
    }

    private suspend fun resolveOne(review: MemoryReviewEntity, status: String, note: String) {
        val scope = requireNotNull(scopeKey) { "未选择角色" }
        repository.resolveReview(scope, review.id, status, note).getOrThrow()
    }

    private fun observe() {
        val scope = scopeKey ?: return
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            // 打开审核页时，延后超过冷却期的候选自动回到待审核队列
            runCatching { repository.restoreDueDeferredReviews(scope) }
            combine(
                repository.observePendingReviews(scope),
                repository.observeDeferredReviews(scope)
            ) { pending, deferred -> MemoryReviewState.Content(pending, deferred) }
                .catch { error -> _state.value = MemoryReviewState.Error(currentItems(), error.message ?: "无法加载审核列表") }
                .collect { _state.value = it }
        }
    }

    private fun currentItems(): List<MemoryReviewEntity> = when (val value = _state.value) {
        is MemoryReviewState.Content -> value.reviews
        is MemoryReviewState.Error -> value.previous
        MemoryReviewState.Loading -> emptyList()
    }

    companion object {
        fun scopeFor(userId: String, companionId: String): String =
            com.companion.cc.domain.memory.MemoryScopeKey.forCharacter(userId, companionId)
    }
}
