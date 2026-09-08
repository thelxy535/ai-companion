package com.companion.cc.data.local.repository

import com.companion.cc.data.local.dao.MemoryReviewDao
import com.companion.cc.data.local.dao.MemoryNodeDao
import com.companion.cc.data.local.entity.MemoryReviewEntity
import com.companion.cc.domain.memory.ReflectionCandidate
import com.companion.cc.domain.memory.MemoryConflictDetector
import java.security.MessageDigest
import org.json.JSONArray
import javax.inject.Inject

/** Writes reflection results as user-reviewable proposals only. */
class ReflectionProposalWriter @Inject constructor(
    private val reviewDao: MemoryReviewDao,
    private val nodeDao: MemoryNodeDao
) {
    suspend fun write(
        scopeKey: String,
        sourceIds: List<String>,
        candidates: List<ReflectionCandidate>,
        now: Long = System.currentTimeMillis()
    ): List<MemoryReviewEntity> {
        val written = mutableListOf<MemoryReviewEntity>()
        candidates.forEach { candidate ->
            val sourceJson = JSONArray(sourceIds).toString()
            // 去重哈希不含证据窗口：窗口滑动时相同内容的候选只保留一条审核，
            // 避免对话级批量反思的窗口重叠造成候选刷屏；证据取首次出现时的来源
            val proposalHash = sha256("$scopeKey|${candidate.kind}|${candidate.title}|${candidate.content}")
            val draft = MemoryReviewEntity(
                id = "reflection:$proposalHash",
                scopeKey = scopeKey,
                kind = candidate.kind,
                title = candidate.title,
                content = candidate.content,
                confidence = candidate.confidence,
                sourceIdsJson = sourceJson,
                proposalHash = proposalHash,
                createdAt = now
            )
            val existing = nodeDao.findActiveByKind(scopeKey, candidate.kind, now, 30)
            val conflict = MemoryConflictDetector.detect(draft, existing)
            val status = if (conflict is com.companion.cc.domain.memory.MemoryConflictResult.Conflict) "conflict" else "pending"
            val stored = draft.copy(
                status = status,
                resolutionNote = (conflict as? com.companion.cc.domain.memory.MemoryConflictResult.Conflict)?.reason.orEmpty()
            )
            reviewDao.insert(stored)
            written += stored
        }
        return written
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
