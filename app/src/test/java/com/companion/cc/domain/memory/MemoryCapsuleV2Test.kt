package com.companion.cc.domain.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryCapsuleV2Test {
    private val scope = "user:user-1:companion:character-1"

    @Test
    fun codecRoundTripsTypedCapsule() {
        val source = MemoryCapsuleSource(
            id = "source-1",
            scopeKey = scope,
            messageId = "message-1",
            contentSnapshot = "Likes tea",
            sourceType = "message",
            occurredAt = 1L,
            contentHash = "hash-1",
            createdAt = 1L
        )
        val node = MemoryCapsuleNode(
            id = "node-1",
            scopeKey = scope,
            kind = "preference",
            title = "Tea",
            content = "Likes tea",
            validFrom = 1L,
            createdAt = 1L,
            updatedAt = 1L
        )
        val capsule = MemoryCapsuleV2(
            scopeKey = scope,
            sources = listOf(source),
            nodes = listOf(node),
            evidence = listOf(MemoryCapsuleEvidence("node-1", "source-1", createdAt = 1L))
        )

        val json = MemoryCapsuleV2Codec().encode(capsule)
        val decoded = MemoryCapsuleV2Codec().decode(json).getOrThrow()

        assertEquals(capsule, decoded)
    }

    @Test
    fun codecRejectsUnsupportedSchemaVersion() {
        val result = MemoryCapsuleV2Codec().decode(
            "{\"format\":\"cc-switch.memory-capsule\",\"schemaVersion\":99,\"scopeKey\":\"$scope\"}"
        )

        assertTrue(result.isFailure)
        assertEquals("Unsupported memory capsule schema version", result.exceptionOrNull()?.message)
    }

    @Test
    fun codecRejectsMissingFormatInsteadOfApplyingV2Default() {
        val result = MemoryCapsuleV2Codec().decode(
            "{\"schemaVersion\":2,\"scopeKey\":\"$scope\"}"
        )

        assertTrue(result.isFailure)
        assertEquals("Memory capsule format is required", result.exceptionOrNull()?.message)
    }

    @Test
    fun codecRejectsMissingSchemaVersionInsteadOfApplyingV2Default() {
        val result = MemoryCapsuleV2Codec().decode(
            "{\"format\":\"cc-switch.memory-capsule\",\"scopeKey\":\"$scope\"}"
        )

        assertTrue(result.isFailure)
        assertEquals("Memory capsule schema version is required", result.exceptionOrNull()?.message)
    }

    @Test
    fun codecRejectsLegacyArrayPayload() {
        val result = MemoryCapsuleV2Codec().decode("[]")

        assertTrue(result.isFailure)
        assertEquals("Memory capsule must be a JSON object", result.exceptionOrNull()?.message)
    }

    @Test
    fun codecRejectsCredentialsAtTheTopLevel() {
        val result = MemoryCapsuleV2Codec().decode(
            "{\"format\":\"cc-switch.memory-capsule\",\"schemaVersion\":2,\"scopeKey\":\"$scope\",\"apiKey\":\"secret\"}"
        )

        assertTrue(result.isFailure)
        assertEquals(
            "Memory capsule must not contain credentials or settings",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun validatorRejectsMalformedScopeKey() {
        val result = MemoryCapsuleV2Validator.validate(
            MemoryCapsuleV2(scopeKey = "companion:character-1")
        )

        assertTrue(result.isFailure)
        assertEquals(
            "Memory capsule scope must identify a user and companion",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun validatorRejectsSourcesOutsideTheDeclaredScope() {
        val capsule = MemoryCapsuleV2(
            scopeKey = scope,
            sources = listOf(
                MemoryCapsuleSource(
                    id = "source-1",
                    scopeKey = "user:user-2:companion:character-1",
                    messageId = null,
                    contentSnapshot = "cross-scope source",
                    sourceType = "message",
                    occurredAt = 1L,
                    contentHash = "hash-1",
                    createdAt = 1L
                )
            )
        )

        val result = MemoryCapsuleV2Validator.validate(capsule)

        assertTrue(result.isFailure)
        assertEquals(
            "Memory capsule source does not belong to declared scope",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun validatorRejectsNodesOutsideTheDeclaredScope() {
        val capsule = MemoryCapsuleV2(
            scopeKey = scope,
            nodes = listOf(
                MemoryCapsuleNode(
                    id = "node-1",
                    scopeKey = "user:user-2:companion:character-1"
                )
            )
        )

        val result = MemoryCapsuleV2Validator.validate(capsule)

        assertTrue(result.isFailure)
        assertEquals(
            "Memory capsule node does not belong to declared scope",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun validatorRejectsUnknownNodeStatus() {
        val result = MemoryCapsuleV2Validator.validate(
            MemoryCapsuleV2(
                scopeKey = scope,
                nodes = listOf(MemoryCapsuleNode(id = "node-1", scopeKey = scope, status = "unknown"))
            )
        )

        assertTrue(result.isFailure)
        assertEquals("Memory capsule node has an invalid status", result.exceptionOrNull()?.message)
    }

    @Test
    fun validatorRejectsEvidenceThatReferencesMissingRecords() {
        val result = MemoryCapsuleV2Validator.validate(
            MemoryCapsuleV2(
                scopeKey = scope,
                evidence = listOf(
                    MemoryCapsuleEvidence(
                        nodeId = "missing-node",
                        sourceId = "missing-source",
                        createdAt = 1L
                    )
                )
            )
        )

        assertTrue(result.isFailure)
        assertEquals(
            "Memory capsule evidence references a missing node",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun validatorRejectsBlankSourceIds() {
        val result = MemoryCapsuleV2Validator.validate(
            MemoryCapsuleV2(
                scopeKey = scope,
                sources = listOf(
                    MemoryCapsuleSource(
                        id = " ",
                        scopeKey = scope,
                        messageId = null,
                        contentSnapshot = "source",
                        sourceType = "message",
                        occurredAt = 1L,
                        contentHash = "hash",
                        createdAt = 1L
                    )
                )
            )
        )

        assertTrue(result.isFailure)
        assertEquals("Memory capsule source ID is required", result.exceptionOrNull()?.message)
    }

    @Test
    fun validatorRejectsDuplicateSourceIds() {
        val source = MemoryCapsuleSource(
            id = "source-1",
            scopeKey = scope,
            messageId = null,
            contentSnapshot = "same source",
            sourceType = "message",
            occurredAt = 1L,
            contentHash = "hash-1",
            createdAt = 1L
        )

        val result = MemoryCapsuleV2Validator.validate(
            MemoryCapsuleV2(scopeKey = scope, sources = listOf(source, source))
        )

        assertTrue(result.isFailure)
        assertEquals("Memory capsule contains duplicate source IDs", result.exceptionOrNull()?.message)
    }

    @Test
    fun validatorRejectsRelationWithMissingNodeEndpoint() {
        val result = MemoryCapsuleV2Validator.validate(
            MemoryCapsuleV2(
                scopeKey = scope,
                nodes = listOf(MemoryCapsuleNode(id = "node-1", scopeKey = scope)),
                relations = listOf(
                    MemoryCapsuleRelation(
                        fromNodeId = "node-1",
                        toNodeId = "missing-node",
                        relationType = "supports",
                        scopeKey = scope,
                        createdAt = 1L,
                        updatedAt = 1L
                    )
                )
            )
        )

        assertTrue(result.isFailure)
        assertEquals(
            "Memory capsule relation references a missing node",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun validatorRejectsFeedbackForMissingTraceOrNode() {
        val result = MemoryCapsuleV2Validator.validate(
            MemoryCapsuleV2(
                scopeKey = scope,
                retrievalFeedback = listOf(
                    MemoryCapsuleRetrievalFeedback(
                        traceId = "missing-trace",
                        nodeId = "missing-node",
                        feedback = "positive",
                        createdAt = 1L
                    )
                )
            )
        )

        assertTrue(result.isFailure)
        assertEquals(
            "Memory capsule feedback references a missing trace",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun validatorRejectsFeedbackNotSelectedByItsTrace() {
        val result = MemoryCapsuleV2Validator.validate(
            MemoryCapsuleV2(
                scopeKey = scope,
                nodes = listOf(
                    MemoryCapsuleNode(id = "node-1", scopeKey = scope),
                    MemoryCapsuleNode(id = "node-2", scopeKey = scope)
                ),
                retrievalTraces = listOf(
                    MemoryCapsuleRetrievalTrace(
                        id = "trace-1",
                        scopeKey = scope,
                        query = "tea",
                        selectedNodeIdsJson = "[\"node-1\"]",
                        explanationJson = "{}",
                        createdAt = 1L,
                        durationMs = 1L
                    )
                ),
                retrievalFeedback = listOf(
                    MemoryCapsuleRetrievalFeedback(
                        traceId = "trace-1",
                        nodeId = "node-2",
                        feedback = "negative",
                        createdAt = 1L
                    )
                )
            )
        )

        assertTrue(result.isFailure)
        assertEquals(
            "Memory capsule feedback node was not selected by its trace",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun validatorRejectsReviewWithSourceOutsideCapsule() {
        val result = MemoryCapsuleV2Validator.validate(
            MemoryCapsuleV2(
                scopeKey = scope,
                reviews = listOf(
                    MemoryCapsuleReview(
                        id = "review-1",
                        scopeKey = scope,
                        kind = "preference",
                        title = "Tea",
                        content = "Likes tea",
                        confidence = 0.8,
                        sourceIds = listOf("missing-source"),
                        proposalHash = "hash-1",
                        createdAt = 1L
                    )
                )
            )
        )

        assertTrue(result.isFailure)
        assertEquals(
            "Memory capsule review references a missing source",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun validatorRejectsDuplicateReviewIds() {
        val review = MemoryCapsuleReview(
            id = "review-1",
            scopeKey = scope,
            kind = "preference",
            title = "Tea",
            content = "Likes tea",
            confidence = 0.8,
            proposalHash = "hash-1",
            createdAt = 1L
        )

        val result = MemoryCapsuleV2Validator.validate(
            MemoryCapsuleV2(scopeKey = scope, reviews = listOf(review, review))
        )

        assertTrue(result.isFailure)
        assertEquals("Memory capsule contains duplicate review IDs", result.exceptionOrNull()?.message)
    }

    @Test
    fun validatorRejectsDuplicateReviewProposalHashes() {
        val first = MemoryCapsuleReview(
            id = "review-1",
            scopeKey = scope,
            kind = "preference",
            title = "Tea",
            content = "Likes tea",
            confidence = 0.8,
            proposalHash = "hash-1",
            createdAt = 1L
        )

        val result = MemoryCapsuleV2Validator.validate(
            MemoryCapsuleV2(scopeKey = scope, reviews = listOf(first, first.copy(id = "review-2")))
        )

        assertTrue(result.isFailure)
        assertEquals(
            "Memory capsule contains duplicate review proposal hashes",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun validatorRejectsVersionForMissingNode() {
        val result = MemoryCapsuleV2Validator.validate(
            MemoryCapsuleV2(
                scopeKey = scope,
                versions = listOf(
                    MemoryCapsuleVersion(
                        nodeId = "missing-node",
                        version = 1,
                        kind = "preference",
                        title = "Tea",
                        content = "Likes tea",
                        importance = 50,
                        confidence = 0.8,
                        validFrom = 1L,
                        validUntil = null,
                        changeReason = "accepted",
                        actor = "user",
                        createdAt = 1L
                    )
                )
            )
        )

        assertTrue(result.isFailure)
        assertEquals(
            "Memory capsule version references a missing node",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun validatorRejectsMalformedTraceSelectedNodeIds() {
        val result = MemoryCapsuleV2Validator.validate(
            MemoryCapsuleV2(
                scopeKey = scope,
                retrievalTraces = listOf(
                    MemoryCapsuleRetrievalTrace(
                        id = "trace-1",
                        scopeKey = scope,
                        query = "tea",
                        selectedNodeIdsJson = "not-json",
                        explanationJson = "{}",
                        createdAt = 1L,
                        durationMs = 1L
                    )
                )
            )
        )

        assertTrue(result.isFailure)
    }
}
