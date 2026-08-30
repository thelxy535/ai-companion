package com.companion.cc.domain.character

import com.companion.cc.domain.memory.MemoryCapsuleNode
import com.companion.cc.domain.memory.MemoryCapsuleV2
import com.companion.cc.domain.model.CustomCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterMemoryCapsuleBuilderTest {

    @Test
    fun payloadContainsCharacterConfigurationButNeverConversationHistory() {
        val character = CustomCharacter(
            id = "character-1",
            userId = "user-1",
            name = "Moss",
            avatar = "avatar://moss",
            description = "quiet companion",
            personality = com.companion.cc.domain.model.PersonalityTraits.default(),
            backstory = "forest story",
            greetingMessage = "hello",
            exampleDialogues = emptyList(),
            voiceConfig = null,
            behaviorRules = null
        )

        val payload = CharacterMemoryCapsuleBuilder().build(character)

        assertEquals("character-1", payload.sourceCharacterId)
        val restored = CharacterMemoryCapsuleBuilder().restore(
            payload.serialized,
            userId = "user-1",
            characterId = "new-character"
        )
        assertEquals("new-character", restored.id)
        assertEquals("user-1", restored.userId)
        assertEquals("Moss", restored.name)
        assertFalse(payload.serialized.contains("private chat"))
    }

    @Test
    fun withMemoriesPayloadRoundTripsVersionedMemorySnapshot() {
        val character = character()
        val memory = MemoryCapsuleV2(
            scopeKey = "user:user-1:companion:character-1",
            nodes = listOf(
                MemoryCapsuleNode(
                    id = "node-1",
                    scopeKey = "user:user-1:companion:character-1",
                    title = "Favorite tree",
                    content = "The old oak",
                    validFrom = 1L,
                    createdAt = 1L,
                    updatedAt = 1L
                )
            )
        )

        val payload = CharacterMemoryCapsuleBuilder().build(character, memory)
        val decoded = CharacterMemoryCapsuleBuilder().restorePayload(
            payload.serialized,
            userId = "user-1",
            characterId = "new-character"
        )

        assertEquals("new-character", decoded.character.id)
        assertEquals("character-1", decoded.sourceCharacterId)
        assertEquals(memory, decoded.memorySnapshot)
    }

    @Test
    fun withMemoriesRejectsIdsSharedAcrossMemoryEntityTypes() {
        val character = character()
        val snapshot = MemoryCapsuleV2(
            scopeKey = "user:user-1:companion:character-1",
            sources = listOf(
                com.companion.cc.domain.memory.MemoryCapsuleSource(
                    id = "shared-id",
                    scopeKey = "user:user-1:companion:character-1",
                    messageId = null,
                    contentSnapshot = "source",
                    sourceType = "conversation",
                    occurredAt = 1L,
                    contentHash = "hash",
                    createdAt = 1L
                )
            ),
            nodes = listOf(
                MemoryCapsuleNode(
                    id = "shared-id",
                    scopeKey = "user:user-1:companion:character-1",
                    title = "Memory",
                    content = "content",
                    validFrom = 1L,
                    createdAt = 1L,
                    updatedAt = 1L
                )
            )
        )

        val result = runCatching {
            CharacterMemoryCapsuleBuilder().build(character, snapshot)
        }

        assertTrue(result.isFailure)
    }

    @Test
    fun withMemoriesRejectsDuplicateRetrievalFeedback() {
        val scope = "user:user-1:companion:character-1"
        val feedback = com.companion.cc.domain.memory.MemoryCapsuleRetrievalFeedback(
            traceId = "trace-1",
            nodeId = "node-1",
            feedback = "positive",
            createdAt = 1L
        )
        val snapshot = MemoryCapsuleV2(
            scopeKey = scope,
            nodes = listOf(
                MemoryCapsuleNode(
                    id = "node-1",
                    scopeKey = scope,
                    title = "Memory",
                    content = "content",
                    validFrom = 1L,
                    createdAt = 1L,
                    updatedAt = 1L
                )
            ),
            retrievalTraces = listOf(
                com.companion.cc.domain.memory.MemoryCapsuleRetrievalTrace(
                    id = "trace-1",
                    scopeKey = scope,
                    query = "memory",
                    selectedNodeIdsJson = "[\"node-1\"]",
                    explanationJson = "[]",
                    createdAt = 1L,
                    durationMs = 1L
                )
            ),
            retrievalFeedback = listOf(feedback, feedback.copy(feedback = "negative"))
        )

        val result = runCatching {
            CharacterMemoryCapsuleBuilder().build(character(), snapshot)
        }

        assertTrue(result.isFailure)
    }

    @Test
    fun withMemoriesRejectsMalformedRelationEvidence() {
        val scope = "user:user-1:companion:character-1"
        val snapshot = MemoryCapsuleV2(
            scopeKey = scope,
            nodes = listOf(
                MemoryCapsuleNode(
                    id = "node-1",
                    scopeKey = scope,
                    title = "Memory",
                    content = "content",
                    validFrom = 1L,
                    createdAt = 1L,
                    updatedAt = 1L
                )
            ),
            relations = listOf(
                com.companion.cc.domain.memory.MemoryCapsuleRelation(
                    fromNodeId = "node-1",
                    toNodeId = "node-1",
                    relationType = "supports",
                    scopeKey = scope,
                    evidenceJson = "not-json",
                    createdAt = 1L,
                    updatedAt = 1L
                )
            )
        )

        val result = runCatching {
            CharacterMemoryCapsuleBuilder().build(character(), snapshot)
        }

        assertTrue(result.isFailure)
    }

    private fun character() = CustomCharacter(
        id = "character-1",
        userId = "user-1",
        name = "Moss",
        avatar = "avatar://moss",
        description = "quiet companion",
        personality = com.companion.cc.domain.model.PersonalityTraits.default(),
        backstory = "forest story",
        greetingMessage = "hello",
        exampleDialogues = emptyList(),
        voiceConfig = null,
        behaviorRules = null
    )
}
