package com.companion.cc.data.identity

import com.companion.cc.data.local.SettingsManager
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class DataStoreCurrentUserProviderTest {
    @Test
    fun `requireUserId returns the persisted nonblank id`() = runTest {
        val settings = mock<SettingsManager>()
        whenever(settings.userIdFlow).thenReturn(flowOf("user-real"))
        val provider = DataStoreCurrentUserProvider(settings)

        assertEquals("user-real", provider.requireUserId())
    }

    @Test
    fun `userId ignores blank values and removes duplicates`() = runTest {
        val settings = mock<SettingsManager>()
        whenever(settings.userIdFlow).thenReturn(
            flowOf("", "user-real", "user-real", "user-next")
        )
        val provider = DataStoreCurrentUserProvider(settings)

        assertEquals(listOf("user-real", "user-next"), provider.userId.toList())
    }
}
