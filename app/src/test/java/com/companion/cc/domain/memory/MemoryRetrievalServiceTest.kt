package com.companion.cc.domain.memory

import com.companion.cc.data.local.entity.MemoryNodeEntity
import com.google.gson.Gson
import com.companion.cc.data.local.repository.MemoryRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class MemoryRetrievalServiceTest {
    @Test
    fun rankingPrefersTitleMatch() {
        val now = 1000L
        val title = node("a", "茶偏好", "用户喜欢喝茶")
        val other = node("b", "旅行", "用户去过很多地方")
        val result = MemoryRetrievalService.rank("茶", listOf(other, title), now)
        assertEquals("a", result.first().node.id)
    }

    @Test
    fun rankingAddsRecencyToScore() {
        val old = node("old", "偏好", "内容", updatedAt = 0L)
        val recent = node("recent", "偏好", "内容", updatedAt = 900L)

        val result = MemoryRetrievalService.rank("偏好", listOf(old, recent), now = 1000L)

        assertEquals("recent", result.first().node.id)
        assertTrue(result.first().score > result[1].score)
    }
    @Test
    fun rankingExcludesExpiredNodes() {
        val expired = node("expired", "茶偏好", "已过期的偏好").copy(validUntil = 999L)
        val current = node("current", "旅行", "仍然有效").copy(validUntil = 1001L)

        val result = MemoryRetrievalService.rank(
            query = "茶",
            nodes = listOf(expired, current),
            now = 1000L
        )

        assertEquals(listOf("current"), result.map { it.node.id })
    }
    @Test
    fun rankingExcludesNodesNotYetValid() {
        val future = node("future", "茶偏好", "未来才生效").copy(validFrom = 1001L)
        val current = node("current", "旅行", "现在有效").copy(validFrom = 999L)

        val result = MemoryRetrievalService.rank(
            query = "茶",
            nodes = listOf(future, current),
            now = 1000L
        )

        assertEquals(listOf("current"), result.map { it.node.id })
    }
    @Test
    fun rankingBreaksCompleteScoreTiesByNodeId() {
        val first = node("node-b", "same", "same")
        val second = node("node-a", "same", "same")

        val result = MemoryRetrievalService.rank("same", listOf(first, second), now = 1000L)

        assertEquals(listOf("node-a", "node-b"), result.map { it.node.id })
    }
    @Test
    fun rankingPenalizesNegativeFeedback() {
        val titleMatch = node("rejected", "茶偏好", "用户喜欢喝茶")
        val fallback = node("fallback", "茶", "普通内容")

        val result = MemoryRetrievalService.rank(
            query = "茶",
            nodes = listOf(titleMatch, fallback),
            now = 1000L,
            feedback = mapOf(
                "rejected" to RetrievalFeedbackSummary(negativeCount = 5)
            )
        )

        assertEquals("fallback", result.first().node.id)
        assertTrue(result.last().explanation.contains("negative=5"))
    }


    @Test
    fun retrieveUsesQueryWhenLoadingCandidates() = runTest {
        val repository = mock<MemoryRepository>()
        val matching = node("tea", "茶偏好", "用户喜欢喝茶")
        whenever(repository.observeRecallCandidates("companion:a", now = 1000L, query = "茶", limit = 1))
            .thenReturn(flowOf(listOf(matching)))
        val service = MemoryRetrievalService(repository, now = { 1000L })

        val result = service.retrieve("茶", "companion:a", limit = 1)

        assertEquals(listOf("tea"), result.memories.map { it.node.id })
        verify(repository).observeRecallCandidates("companion:a", now = 1000L, query = "茶", limit = 1)
    }



    @Test
    fun retrievalTraceStoresValidJsonForNodeIds() = runTest {
        val repository = mock<MemoryRepository>()
        val nodeId = "node:\"quoted\nvalue"
        whenever(repository.observeRecallCandidates("companion:a", now = 10L, query = "tea", limit = 1))
            .thenReturn(flowOf(listOf(node(nodeId, "tea", "content"))))
        val service = MemoryRetrievalService(repository, now = { 10L })

        service.retrieve("tea", "companion:a", limit = 1)

        verify(repository).recordRetrieval(org.mockito.kotlin.check {
            assertEquals(listOf(nodeId), Gson().fromJson(it.selectedNodeIdsJson, Array<String>::class.java).toList())
        })
    }
    @Test
    fun retrievalTraceRecordsElapsedDuration() = runTest {
        val repository = mock<MemoryRepository>()
        whenever(repository.observeRecallCandidates("companion:a", now = 10L, query = "tea", limit = 1))
            .thenReturn(flowOf(listOf(node("tea", "tea", "content"))))
        val elapsed = ArrayDeque(listOf(0L, 7L))
        val service = MemoryRetrievalService(
            repository = repository,
            now = { 10L },
            elapsedMillis = { elapsed.removeFirst() }
        )

        service.retrieve("tea", "companion:a", limit = 1)

        verify(repository).recordRetrieval(org.mockito.kotlin.check {
            assertEquals(7L, it.durationMs)
        })
    }

    private fun node(
        id: String,
        title: String,
        content: String,
        validFrom: Long = 0L,
        updatedAt: Long = 0L
    ) = MemoryNodeEntity(
        id,
        "companion:a",
        "preference",
        "user",
        "user",
        title,
        content,
        validFrom = validFrom,
        createdAt = 0,
        updatedAt = updatedAt
    )
}