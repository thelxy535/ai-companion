package com.companion.cc.domain.memory

import com.companion.cc.data.local.dao.MemoryNodeDao
import com.companion.cc.data.local.dao.MemoryRelationDao
import com.companion.cc.data.local.entity.MemoryNodeEntity
import com.companion.cc.data.local.entity.MemoryRelationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomMemoryGraphRepositoryTest {
    private val scope = "user:u1:companion:c1"

    @Test
    fun `root projection expands bounded hops and reports orphans`() = runTest {
        val nodes = listOf(node("a", importance = 90), node("b"), node("c"), node("orphan"))
        val edges = listOf(edge("a", "b"), edge("b", "c"))
        val repository = repository(nodes, edges)

        val snapshot = repository.project(GraphQuery(scopeKey = scope, rootNodeId = "a", hop = 1))

        assertEquals(listOf("a", "b"), snapshot.nodes.map { it.id })
        assertEquals(1, snapshot.edges.size)
        assertEquals(listOf("a"), snapshot.roots)
        assertTrue(snapshot.orphans.isEmpty())
        assertEquals(2, snapshot.frontierCount)
    }

    @Test
    fun `projection filters expired nodes and caps nodes and edges`() = runTest {
        val nodes = listOf(
            node("a", importance = 90),
            node("b", importance = 80),
            node("c", importance = 70),
            node("expired", validUntil = 5)
        )
        val edges = listOf(edge("a", "b", weight = 0.9), edge("b", "c", weight = 0.8))
        val snapshot = repository(nodes, edges).project(
            GraphQuery(scopeKey = scope, asOf = 10, maxNodes = 2, maxEdges = 1)
        )

        assertEquals(listOf("a", "b"), snapshot.nodes.map { it.id })
        assertEquals(1, snapshot.edges.size)
        assertTrue(snapshot.truncated)
        assertTrue(snapshot.nodes.none { it.id == "expired" })
    }

    @Test
    fun `relations with missing or foreign endpoints are excluded`() = runTest {
        val nodes = listOf(node("a"), node("b"))
        val edges = listOf(edge("a", "b"), edge("a", "foreign"), edge("a", "b", scopeKey = "other"))

        val snapshot = repository(nodes, edges).project(GraphQuery(scopeKey = scope))

        assertEquals(1, snapshot.edges.size)
        assertEquals("a", snapshot.edges.single().fromNodeId)
    }

    private fun repository(nodes: List<MemoryNodeEntity>, edges: List<MemoryRelationEntity>) =
        RoomMemoryGraphRepository(
            nodeDao = object : MemoryNodeDao {
                override suspend fun insert(node: MemoryNodeEntity) = Unit
                override suspend fun deleteByScope(scopeKey: String) = 0
                override suspend fun findAllInScope(scopeKey: String) = nodes.filter { it.scopeKey == scopeKey }
                override suspend fun findByIdForScope(scopeKey: String, id: String) = nodes.find { it.scopeKey == scopeKey && it.id == id }
                override suspend fun findById(id: String) = nodes.find { it.id == id }
                override suspend fun findActiveByKindAndSubject(
                    scopeKey: String,
                    kind: String,
                    subjectRole: String,
                    subjectKey: String
                ) = nodes.filter {
                    it.scopeKey == scopeKey &&
                        it.status == "active" &&
                        it.kind == kind &&
                        it.subjectRole == subjectRole &&
                        it.subjectKey == subjectKey
                }
                override suspend fun findActiveByKind(scopeKey: String, kind: String, now: Long, limit: Int) =
                    nodes.filter {
                        val validUntil = it.validUntil
                        it.scopeKey == scopeKey &&
                            it.status == "active" &&
                            it.kind == kind &&
                            it.validFrom <= now &&
                            (validUntil == null || validUntil > now)
                    }.sortedByDescending { it.updatedAt }.take(limit)
                override fun observeActiveCount(userId: String): Flow<Int> = emptyFlow()
                override fun observeRecallCandidates(scopeKey: String, now: Long, query: String, limit: Int): Flow<List<MemoryNodeEntity>> = emptyFlow()
                override fun observeFiltered(scopeKey: String, status: String, kind: String, query: String, limit: Int): Flow<List<MemoryNodeEntity>> = emptyFlow()
                override suspend fun update(node: MemoryNodeEntity) = Unit
            },
            relationDao = object : MemoryRelationDao {
                override suspend fun insert(relation: MemoryRelationEntity) = Unit
                override fun observeActive(scopeKey: String): Flow<List<MemoryRelationEntity>> = emptyFlow()
                override suspend fun deleteByScope(scopeKey: String) = 0
                override suspend fun findAllInScope(scopeKey: String) = edges.filter { it.scopeKey == scopeKey }
                override suspend fun findForNodeInScopeIncludingArchived(scopeKey: String, nodeId: String): List<MemoryRelationEntity> = emptyList()
                override suspend fun findForKeyInScope(scopeKey: String, fromNodeId: String, toNodeId: String, relationType: String): MemoryRelationEntity? = null
                override suspend fun findForNode(nodeId: String): List<MemoryRelationEntity> = emptyList()
                override suspend fun update(relation: MemoryRelationEntity) = Unit
            }
        )

    private fun node(id: String, importance: Int = 50, validUntil: Long? = null) = MemoryNodeEntity(
        id = id, scopeKey = scope, kind = "fact", subjectRole = "user", subjectKey = "u1",
        title = id, content = id, importance = importance, confidence = 0.8,
        validFrom = 0, validUntil = validUntil, status = "active", createdAt = 0, updatedAt = 1
    )

    private fun edge(from: String, to: String, weight: Double = 0.5, scopeKey: String = scope) =
        MemoryRelationEntity(from, to, "supports", scopeKey, weight, "active", "[]", 0, 1)
}
