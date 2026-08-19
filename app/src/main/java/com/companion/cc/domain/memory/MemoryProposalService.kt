package com.companion.cc.domain.memory

import com.companion.cc.data.local.entity.MemorySourceEntity
import com.companion.cc.data.local.repository.MemoryRepository
import com.companion.cc.data.local.repository.MemoryReviewDraft
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

class MemoryProposalService @Inject constructor(private val repository: MemoryRepository) {
    suspend fun createProposal(scopeKey: String, source: MemorySourceEntity, kind: String, title: String, content: String, confidence: Double): Result<String> = runCatching {
        repository.addSource(source)
        val hash = sha256("$scopeKey|$kind|$content|${source.contentHash}")
        val id = "review:$hash"
        repository.createReview(MemoryReviewDraft(id, scopeKey, kind, title, content, confidence.coerceIn(0.0, 1.0), hash, sourceIdsJson = "[\"${source.id}\"]"))
        id
    }

    companion object {
        fun kindFor(type: String): String = when (type.uppercase()) {
            "PREFERENCE" -> "preference"
            "EVENT" -> "event"
            "EMOTION" -> "emotion"
            "USER_INFO" -> "identity"
            else -> "observation"
        }

        private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
