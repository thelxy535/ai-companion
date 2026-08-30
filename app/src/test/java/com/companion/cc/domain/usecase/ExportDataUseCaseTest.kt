package com.companion.cc.domain.usecase

import com.companion.cc.data.local.SettingsManager
import com.companion.cc.domain.model.ExportData
import com.companion.cc.domain.repository.MessageRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ExportDataUseCaseTest {
    private val repository = mock<MessageRepository>()
    private val settings = mock<SettingsManager>()
    private val useCase = ExportDataUseCase(repository, settings)

    @Test
    fun exportDoesNotIncludeApiKey() = runTest {
        whenever(settings.userIdFlow).thenReturn(flowOf("user-1"))
        whenever(settings.apiKeyFlow).thenReturn(flowOf("secret-api-key"))
        whenever(settings.baseUrlFlow).thenReturn(flowOf("https://example.test"))
        whenever(settings.modelFlow).thenReturn(flowOf("model"))
        whenever(repository.getAllMessages("user-1")).thenReturn(flowOf(emptyList()))

        val export = useCase()
        val payload = Json.decodeFromString(ExportData.serializer(), export)

        assertEquals("", payload.settings.apiKey)
    }
}
