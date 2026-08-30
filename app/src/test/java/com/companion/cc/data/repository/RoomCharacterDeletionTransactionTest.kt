package com.companion.cc.data.repository

import androidx.room.Room
import com.companion.cc.data.local.database.AppDatabase
import com.companion.cc.data.local.entity.CharacterCleanupTaskEntity
import com.companion.cc.data.local.entity.CharacterMemoryCapsuleEntity
import com.companion.cc.data.mapper.CustomCharacterMapper
import com.companion.cc.domain.model.CustomCharacter
import com.companion.cc.domain.model.PersonalityTraits
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RoomCharacterDeletionTransactionTest {
    private lateinit var database: AppDatabase
    private lateinit var transaction: RoomCharacterDeletionTransaction

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        transaction = RoomCharacterDeletionTransaction(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun capsuleFactoryRunsInsideDeletionTransaction() = runTest {
        database.customCharacterDao().insert(CustomCharacterMapper.toEntity(character()))
        var factoryRanInsideTransaction = false

        val result = transaction.deleteAtomically(
            userId = "user-1",
            characterId = "character-1",
            capsuleFactory = {
                factoryRanInsideTransaction = database.inTransaction()
                capsule()
            },
            cleanupTask = cleanupTask()
        )

        assertTrue(result)
        assertTrue(factoryRanInsideTransaction)
    }

    private fun capsule() = CharacterMemoryCapsuleEntity(
        id = "capsule-1",
        userId = "user-1",
        sourceCharacterId = "character-1",
        schemaVersion = 2,
        revivalTokenHash = "hash",
        encryptedRevivalToken = "token",
        encryptedPayload = "payload",
        createdAt = 1L,
        deletedAt = 1L
    )

    private fun cleanupTask() = CharacterCleanupTaskEntity(
        id = "cleanup-1",
        userId = "user-1",
        characterId = "character-1",
        createdAt = 1L,
        updatedAt = 1L
    )

    private fun character() = CustomCharacter(
        id = "character-1",
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
