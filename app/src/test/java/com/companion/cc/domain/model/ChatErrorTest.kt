package com.companion.cc.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * ChatError 单元测试
 *
 * 测试错误类型转换和用户提示
 */
class ChatErrorTest {

    @Test
    fun `NetworkError should have correct user message`() {
        // Given
        val error = ChatError.NetworkError(message = "connection failed")

        // Then
        assertTrue("NetworkError 应该可重试", error.canRetry)
        assertEquals("网络连接失败，请检查网络设置", error.userMessage)
    }

    @Test
    fun `ApiError 401 should not be retryable`() {
        // Given
        val error = ChatError.ApiError(401, "Unauthorized")

        // Then
        assertFalse("401 错误不应该可重试", error.canRetry)
        assertEquals("应该有正确的 userMessage", "API Key 无效，请在设置中检查", error.userMessage)
    }

    @Test
    fun `ApiError 429 should be retryable with retry time`() {
        // Given
        val error = ChatError.RateLimitError(retryAfter = 60)

        // Then
        assertTrue("429 错误应该可重试", error.canRetry)
        assertTrue("应该包含等待时间", error.userMessage.contains("60"))
    }

    @Test
    fun `TimeoutError should be retryable`() {
        // Given
        val error = ChatError.TimeoutError(message = "timeout")

        // Then
        assertTrue("超时错误应该可重试", error.canRetry)
        assertTrue("应该提示超时", error.userMessage.contains("超时"))
    }

    @Test
    fun `LocalStorageError should not be retryable`() {
        // Given
        val error = ChatError.LocalStorageError("database error")

        // Then
        assertFalse("存储错误不应该可重试", error.canRetry)
        assertTrue("应该包含错误信息", error.userMessage.contains("存储"))
    }
}
