package com.companion.cc.domain.model

class StreamHttpException(
    val code: Int,
    message: String,
    val retryAfterSeconds: Long? = null
) : IllegalStateException(message) {
    fun isRetryable(): Boolean = code == 429 || code in 500..599
}
