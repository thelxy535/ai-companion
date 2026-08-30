package com.companion.cc.data.repository

import androidx.room.Room
import com.companion.cc.data.local.database.AppDatabase
import com.companion.cc.data.local.entity.CharacterMemoryCapsuleEntity
import com.companion.cc.domain.memory.MemoryCapsuleNode
import com.companion.cc.domain.memory.MemoryCapsuleSource
import com.companion.cc.domain.memory.MemoryCapsuleV2
import com.companion.cc.domain.model.CustomCharacter
import com.companion.cc.domain.model.PersonalityTraits
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RoomCharacterRevivalTransactionTest {
    private lateinit var database: AppDatabase
    private lateinit var transaction: RoomCharacterRevivalTransaction

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        transaction = RoomCharacterRevivalTransaction(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun crossTypeIdCollisionRollsBackCharacterAndCapsuleStatus() = runTest {
        val capsule = capsule()
        database.characterMemoryCapsuleDao().insert(capsule)
        val scope = "user:user-1:companion:old-id"
        val snapshot = MemoryCapsuleV2(
            scopeKey = scope,
            sources = listOf(
                MemoryCapsuleSource(
                    id = "shared-id",
                    scopeKey = scope,
                    messageId = null,
                    contentSnapshot = "source",
                    sourceType = "conversation",
                    occurredAt = 1L,
                    contentHash = "source-hash",
                    createdAt = 1L
                )
            ),
            nodes = listOf(
                MemoryCapsuleNode(
                    id = "shared-id",
                    scopeKey = scope,
                    title = "Memory",
                    content = "content",
                    validFrom = 1L,
                    createdAt = 1L,
                    updatedAt = 1L
                )
            )
        )

        val result = transaction.reviveAtomically(capsule, character("new-id"), 3L, snapshot)

        assertFalse(result)
        assertNull(database.customCharacterDao().getCharacterByIdForUser("new-id", "user-1"))
        assertEquals(
            "available",
            database.characterMemoryCapsuleDao().findById("user-1", "capsule-1")?.status
        )
    }

    @Test
    fun sameUserSnapshotForDifferentCharacterRollsBackRevival() = runTest {
        val capsule = capsule()
        database.characterMemoryCapsuleDao().insert(capsule)
        val wrongScope = "user:user-1:companion:different-character"
        val snapshot = MemoryCapsuleV2(
            scopeKey = wrongScope,
            nodes = listOf(
                MemoryCapsuleNode(
                    id = "node-1",
                    scopeKey = wrongScope,
                    title = "Wrong character memory",
                    content = "content",
                    validFrom = 1L,
                    createdAt = 1L,
                    updatedAt = 1L
                )
            )
        )

        val result = transaction.reviveAtomically(capsule, character("new-id"), 3L, snapshot)

        assertFalse(result)
        assertNull(database.customCharacterDao().getCharacterByIdForUser("new-id", "user-1"))
        assertEquals(
            "available",
            database.characterMemoryCapsuleDao().findById("user-1", "capsule-1")?.status
        )
    }

    @Test
    fun restorationClearsSourceMessageReferenceAndCannotBeRepeated() = runTest {
        val capsule = capsule()
        database.characterMemoryCapsuleDao().insert(capsule)
        val scope = "user:user-1:companion:old-id"
        val snapshot = MemoryCapsuleV2(
            scopeKey = scope,
            sources = listOf(
                MemoryCapsuleSource(
                    id = "source-1",
                    scopeKey = scope,
                    messageId = "old-message",
                    contentSnapshot = "source",
                    sourceType = "conversation",
                    occurredAt = 1L,
                    contentHash = "source-hash",
                    createdAt = 1L
                )
            ),
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
            )
        )

        assertTrue(transaction.reviveAtomically(capsule, character("new-id"), 3L, snapshot))
        assertFalse(transaction.reviveAtomically(capsule, character("second-id"), 4L, snapshot))

        val newScope = "user:user-1:companion:new-id"
        val restoredSource = database.memorySourceDao().findAllInScope(newScope).single()
        val restoredNode = database.memoryNodeDao().findAllInScope(newScope).single()
        assertNull(restoredSource.messageId)
        assertNotEquals("source-1", restoredSource.id)
        assertNotEquals("node-1", restoredNode.id)
        assertNull(database.customCharacterDao().getCharacterByIdForUser("second-id", "user-1"))
        assertEquals(
            "new-id",
            database.characterMemoryCapsuleDao().findById("user-1", "capsule-1")?.restoredCharacterId
        )
    }

    private fun capsule() = CharacterMemoryCapsuleEntity(
        id = "capsule-1",
        userId = "user-1",
        sourceCharacterId = "old-id",
        schemaVersion = 2,
        revivalTokenHash = "hash",
        encryptedRevivalToken = "token",
        encryptedPayload = "payload",
        createdAt = 1L,
        deletedAt = 2L
    )

    private fun character(id: String) = CustomCharacter(
        id = id,
        userId = "user-1",
        name = "Moss",
        avatar = null,
        description = "quiet",
        personality = PersonalityTraits.default(),
        backstory = "story",
        greetingMessage = "hello",
        exampleDialogues = emptyList(),
        voiceConfig = null,
        behaviorRules = null
    )
}
