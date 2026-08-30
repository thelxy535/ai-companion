package com.companion.cc.domain.usecase

import com.companion.cc.data.local.SettingsManager
import com.companion.cc.domain.model.ExportData
import com.companion.cc.domain.model.ExportMessage
import com.companion.cc.domain.model.ExportSettings
import com.companion.cc.domain.repository.MessageRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ImportDataUseCaseTest {
    private val repository = mock<MessageRepository>()
    private val settings = mock<SettingsManager>()
    private val useCase = ImportDataUseCase(repository, settings)

    @Test
    fun validPayloadRebindsMessagesToCurrentUser() = runTest {
        whenever(settings.userIdFlow).thenReturn(flowOf("local-user"))

        val result = useCase(payload(owner = "local-user"))

        assertTrue(result is ImportResult.Success)
        val saved = argumentCaptor<List<com.companion.cc.domain.model.Message>>()
        verify(repository).saveMessages(saved.capture())
        assertEquals(listOf("local-user"), saved.firstValue.map { it.userId }.distinct())
        assertEquals("custom-character", saved.firstValue.single().companionId)
    }

    @Test
    fun foreignOwnerIsRejectedWithoutWrites() = runTest {
        whenever(settings.userIdFlow).thenReturn(flowOf("local-user"))

        val result = useCase(payload(owner = "other-user"))

        assertTrue(result is ImportResult.Error)
        assertTrue((result as ImportResult.Error).message.contains("其他用户"))
        verify(repository, never()).saveMessages(any())
        verify(settings, never()).saveBaseUrl(any())
        verify(settings, never()).saveModel(any())
    }

    @Test
    fun unsupportedVersionIsRejectedBeforeAnyWrites() = runTest {
        whenever(settings.userIdFlow).thenReturn(flowOf("local-user"))

        val result = useCase(payload(owner = "local-user", version = "2.0"))

        assertTrue(result is ImportResult.Error)
        assertTrue((result as ImportResult.Error).message.contains("版本"))
        verify(repository, never()).saveMessages(any())
        verify(settings, never()).saveBaseUrl(any())
        verify(settings, never()).saveModel(any())
    }

    @Test
    fun malformedJsonDoesNotWriteAnything() = runTest {
        val result = useCase("{not-json")

        assertTrue(result is ImportResult.Error)
        verify(repository, never()).saveMessages(any())
        verify(settings, never()).saveApiKey(any())
        verify(settings, never()).saveBaseUrl(any())
        verify(settings, never()).saveModel(any())
    }

    private fun payload(owner: String, version: String = "1.0"): String = """
        {
          "version": "$version",
          "exportDate": "2026-08-27",
          "userId": "$owner",
          "messages": [
            {
              "id": "message-1",
              "userId": "$owner",
              "companionId": "custom-character",
              "role": "user",
              "content": "hello",
              "timestamp": 1
            }
          ],
          "settings": {
            "baseURL": "https://example.test",
            "model": "model"
          }
        }
    """.trimIndent()
}
