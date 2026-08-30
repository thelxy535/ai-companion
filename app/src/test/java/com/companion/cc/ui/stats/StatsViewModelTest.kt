package com.companion.cc.ui.stats

import com.companion.cc.MainDispatcherRule
import com.companion.cc.data.local.SettingsManager
import com.companion.cc.data.local.dao.MemoryDao
import com.companion.cc.data.local.dao.MemoryNodeDao
import com.companion.cc.data.local.dao.VectorMemoryDao
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.memory.MemoryScopeKey
import com.companion.cc.domain.repository.MessageRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun loadStatsUsesCurrentUserCharacterMemoryScope() = runTest(main.dispatcher) {
        val messages = mock<MessageRepository>()
        val memoryDao = mock<MemoryDao>()
        val memoryNodeDao = mock<MemoryNodeDao>()
        val vectorMemoryDao = mock<VectorMemoryDao>()
        val settingsManager = mock<SettingsManager>()
        val users = mock<CurrentUserProvider>()
        val userId = "user-1"
        val characterId = "character-1"
        val legacyScope = "companion:$characterId"
        val scopedMemory = MemoryScopeKey.forCharacter(userId, characterId)

        whenever(users.requireUserId()).thenReturn(userId)
        whenever(messages.getMessages(userId, characterId, limit = 1000))
            .thenReturn(flowOf(emptyList()))
        whenever(memoryDao.observeMemoryCount(userId)).thenReturn(flowOf(0))
        whenever(memoryNodeDao.observeFiltered(scopedMemory)).thenReturn(flowOf(emptyList()))
        whenever(memoryNodeDao.observeFiltered(legacyScope)).thenReturn(flowOf(emptyList()))
        whenever(vectorMemoryDao.observeCount(userId, characterId)).thenReturn(flowOf(0))

        val viewModel = StatsViewModel(
            messages,
            memoryDao,
            memoryNodeDao,
            vectorMemoryDao,
            settingsManager,
            users
        )
        viewModel.loadStats(characterId)
        advanceUntilIdle()

        verify(memoryNodeDao).observeFiltered(scopedMemory)
        verify(memoryNodeDao, never()).observeFiltered(legacyScope)
    }
}
