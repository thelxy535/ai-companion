package com.companion.cc.domain.manager

import com.companion.cc.domain.character.CharacterDeletionService
import com.companion.cc.domain.character.CharacterDeletionResult
import com.companion.cc.domain.model.PersonalityTraits
import com.companion.cc.domain.repository.CustomCharacterRepository
import com.companion.cc.domain.repository.MessageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CharacterCascadeDeletionTest {
    private lateinit var manager: CharacterCustomizationManager
    private lateinit var characterRepository: CustomCharacterRepository
    private lateinit var personalityManager: PersonalityManager
    private lateinit var messageRepository: MessageRepository
    private lateinit var memoryLayerManager: MemoryLayerManager
    private lateinit var vectorMemoryManager: VectorMemoryManager
    private lateinit var deletionService: CharacterDeletionService

    private val testUserId = "user-123"
    private val testCharacterId = "char-456"

    @Before
    fun setup() {
        characterRepository = mock()
        personalityManager = mock()
        messageRepository = mock()
        memoryLayerManager = mock()
        vectorMemoryManager = mock()
        deletionService = mock()
        manager = CharacterCustomizationManager(
            characterRepository = characterRepository,
            personalityManager = personalityManager,
            messageRepository = messageRepository,
            memoryLayerManager = memoryLayerManager,
            vectorMemoryManager = vectorMemoryManager,
            deletionService = deletionService
        )
    }

    @Test
    fun `deleteCharacter delegates to atomic service before clearing runtime caches`() = runTest {
        whenever(deletionService.delete(testUserId, testCharacterId)).thenReturn(
            Result.success(CharacterDeletionResult("capsule-1", "token"))
        )

        manager.deleteCharacter(testUserId, testCharacterId)

        inOrder(deletionService, personalityManager, memoryLayerManager, vectorMemoryManager) {
            verify(deletionService).delete(testUserId, testCharacterId)
            verify(personalityManager).removeResolvedConfig(testCharacterId)
            verify(memoryLayerManager).invalidateCharacter(testUserId, testCharacterId)
            verify(vectorMemoryManager).clear(testUserId, testCharacterId)
        }
    }

    @Test
    fun `deleteCharacter preserves runtime caches when atomic service fails`() = runTest {
        whenever(deletionService.delete(testUserId, testCharacterId)).thenReturn(
            Result.failure(IllegalStateException("capsule failed"))
        )

        runCatching { manager.deleteCharacter(testUserId, testCharacterId) }

        verify(personalityManager, org.mockito.kotlin.never()).removeResolvedConfig(testCharacterId)
        verify(memoryLayerManager, org.mockito.kotlin.never()).invalidateCharacter(testUserId, testCharacterId)
        verify(vectorMemoryManager, org.mockito.kotlin.never()).clear(testUserId, testCharacterId)
    }
}
