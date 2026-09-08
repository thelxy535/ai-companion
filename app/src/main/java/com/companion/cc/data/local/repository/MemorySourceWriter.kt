package com.companion.cc.data.local.repository

import com.companion.cc.data.local.dao.MemorySourceDao
import com.companion.cc.data.local.entity.MemorySourceEntity
import com.companion.cc.domain.memory.MemoryScopeKey
import com.companion.cc.domain.model.Message
import java.security.MessageDigest
import javax.inject.Inject

/** Persists immutable conversation episodes as auditable memory sources. */
class MemorySourceWriter @Inject constructor(
    private val sourceDao: MemorySourceDao
) {
    suspend fun retain(message: Message, now: Long = System.currentTimeMillis()): MemorySourceEntity {
        require(message.userId.isNotBlank()) { "message.userId must not be blank" }
        require(message.companionId.isNotBlank()) { "message.companionId must not be blank" }
        require(message.id.isNotBlank()) { "message.id must not be blank" }

        val scopeKey = MemoryScopeKey.forCharacter(message.userId, message.companionId)
        val snapshot = messageSnapshot(message)
        require(snapshot.isNotBlank()) { "message snapshot must not be blank" }
        val contentHash = sha256("$scopeKey\n${message.id}\n$snapshot")

        sourceDao.findByContentHashInScope(scopeKey, contentHash)?.let { return it }

        val source = MemorySourceEntity(
            id = "message:$contentHash",
            scopeKey = scopeKey,
            messageId = message.id,
            contentSnapshot = snapshot,
            sourceType = if (message.imageUrl != null) "message_image" else "message",
            occurredAt = message.timestamp,
            contentHash = contentHash,
            createdAt = now
        )
        sourceDao.insert(source)
        return sourceDao.findByContentHashInScope(scopeKey, contentHash) ?: source
    }

    private fun messageSnapshot(message: Message): String = buildString {
        append(message.role.name.lowercase())
        append(": ")
        append(message.content)
        message.action?.takeIf { it.isNotBlank() }?.let {
            append("\n[action] ")
            append(it)
        }
        message.imageUrl?.let {
            append("\n[image] ")
            append(it)
        }
        message.imageAnalysis?.takeIf { it.isNotBlank() }?.let {
            append("\n[image-analysis] ")
            append(it)
        }
    }

    private fun sha256(value: String): String = MessageDigest
        .getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
}
