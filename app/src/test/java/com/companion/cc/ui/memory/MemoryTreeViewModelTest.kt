package com.companion.cc.ui.memory

import com.companion.cc.MainDispatcherRule
import com.companion.cc.data.local.SettingsManager
import com.companion.cc.data.local.dao.DateCountRow
import com.companion.cc.data.local.dao.StatsDao
import com.companion.cc.data.local.entity.MessageEntity
import com.companion.cc.domain.identity.CurrentUserProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryTreeViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun loadMessagesPreservesFavoriteState() = runTest(main.dispatcher) {
        val statsDao = mock<StatsDao>()
        val settings = mock<SettingsManager>()
        val users = mock<CurrentUserProvider>()
        whenever(users.requireUserId()).thenReturn("user-1")
        whenever(statsDao.getTotalMessagesForCompanion("user-1", "companion-1")).thenReturn(1)
        whenever(statsDao.getTotalDaysForCompanion("user-1", "companion-1")).thenReturn(1)
        whenever(statsDao.getMessageCountByDateForCompanion("user-1", "companion-1"))
            .thenReturn(listOf(DateCountRow("2026-08-26", 1)))
        whenever(statsDao.getMessagesByDateForCompanion("user-1", "companion-1", "2026-08-26"))
            .thenReturn(
                listOf(
                    MessageEntity(
                        id = "message-1",
                        userId = "user-1",
                        companionId = "companion-1",
                        role = "user",
                        content = "收藏的消息",
                        timestamp = 1L,
                        isFavorited = true
                    )
                )
            )

        val viewModel = MemoryTreeViewModel(statsDao, settings, users)
        viewModel.loadMessages("companion-1")
        advanceUntilIdle()

        assertTrue(viewModel.messagesByDate.value.single().messages.single().isFavorited)
    }

    @Test
    fun loadMessagesReadsOnlySelectedCompanionScope() = runTest(main.dispatcher) {
        val statsDao = mock<StatsDao>()
        val settings = mock<SettingsManager>()
        val users = mock<CurrentUserProvider>()
        whenever(users.requireUserId()).thenReturn("user-1")
        whenever(statsDao.getTotalMessagesForCompanion("user-1", "companion-1")).thenReturn(1)
        whenever(statsDao.getTotalDaysForCompanion("user-1", "companion-1")).thenReturn(1)
        whenever(statsDao.getMessageCountByDateForCompanion("user-1", "companion-1"))
            .thenReturn(listOf(DateCountRow("2026-08-26", 1)))
        whenever(statsDao.getMessagesByDateForCompanion("user-1", "companion-1", "2026-08-26"))
            .thenReturn(emptyList())

        val viewModel = MemoryTreeViewModel(statsDao, settings, users)
        viewModel.loadMessages("companion-1")
        advanceUntilIdle()

        verify(statsDao).getTotalMessagesForCompanion("user-1", "companion-1")
        verify(statsDao).getTotalDaysForCompanion("user-1", "companion-1")
        verify(statsDao).getMessageCountByDateForCompanion("user-1", "companion-1")
        verify(statsDao).getMessagesByDateForCompanion("user-1", "companion-1", "2026-08-26")
    }
}
