package com.companion.cc.domain.model

/**
 * 聊天错误类型
 *
 * 用于区分不同类型的错误，提供更精准的错误提示和处理策略
 */
sealed class ChatError {
    abstract val message: String
    abstract val userMessage: String  // 用户友好的错误提示
    abstract val canRetry: Boolean    // 是否可以重试

    /**
     * 网络错误
     */
    data class NetworkError(
        override val message: String,
        override val userMessage: String = "网络连接失败，请检查网络设置",
        override val canRetry: Boolean = true
    ) : ChatError()

    /**
     * API 错误
     */
    data class ApiError(
        val code: Int,
        override val message: String,
        override val userMessage: String = getApiErrorMessage(code),
        override val canRetry: Boolean = code in 500..599  // 5xx 服务器错误可重试
    ) : ChatError() {
        companion object {
            fun getApiErrorMessage(code: Int): String = when (code) {
                400 -> "请求格式错误"
                401 -> "API Key 无效，请在设置中检查"
                403 -> "无权访问此服务"
                404 -> "服务不存在"
                429 -> "请求过于频繁，请稍后再试"
                500 -> "服务器错误，请稍后重试"
                502, 503 -> "服务暂时不可用，请稍后重试"
                504 -> "服务器响应超时，请稍后重试"
                else -> "服务器错误 ($code)"
            }
        }
    }

    /**
     * 限流错误（429 Too Many Requests）
     */
    data class RateLimitError(
        val retryAfter: Long,  // 建议重试时间（秒）
        override val message: String = "请求过于频繁",
        override val userMessage: String = "请求太频繁了，请等待 ${retryAfter} 秒后再试",
        override val canRetry: Boolean = true
    ) : ChatError()

    /**
     * 超时错误
     */
    data class TimeoutError(
        override val message: String = "请求超时",
        override val userMessage: String = "请求超时，请检查网络或稍后重试",
        override val canRetry: Boolean = true
    ) : ChatError()

    /**
     * 本地存储错误
     */
    data class LocalStorageError(
        override val message: String,
        override val userMessage: String = "本地存储失败：$message",
        override val canRetry: Boolean = false
    ) : ChatError()

    /**
     * 内容过滤错误（内容违规）
     */
    data class ContentFilterError(
        override val message: String = "内容包含敏感信息",
        override val userMessage: String = "抱歉，您的消息包含不当内容，请修改后重试",
        override val canRetry: Boolean = false
    ) : ChatError()

    /**
     * 配置错误（API Key 未设置等）
     */
    data class ConfigError(
        override val message: String,
        override val userMessage: String = message,
        override val canRetry: Boolean = false
    ) : ChatError()

    /**
     * 未知错误
     */
    data class UnknownError(
        override val message: String,
        override val userMessage: String = "发生未知错误：$message",
        override val canRetry: Boolean = true
    ) : ChatError()
}

/**
 * 错误处理扩展函数
 */
fun Throwable.toChatError(): ChatError {
    return when (this) {
        is java.net.SocketTimeoutException -> ChatError.TimeoutError()
        is java.net.UnknownHostException -> ChatError.NetworkError("无法连接到服务器")
        is java.net.ConnectException -> ChatError.NetworkError("连接失败")
        is java.io.IOException -> ChatError.NetworkError("网络错误：${this.message}")

        // Retrofit HTTP 错误
        is retrofit2.HttpException -> {
            val code = this.code()
            when (code) {
                429 -> {
                    val retryAfter = this.response()?.headers()?.get("Retry-After")?.toLongOrNull() ?: 60
                    ChatError.RateLimitError(retryAfter)
                }
                else -> ChatError.ApiError(code, this.message ?: "HTTP $code")
            }
        }

        else -> ChatError.UnknownError(this.message ?: "未知错误")
    }
}
