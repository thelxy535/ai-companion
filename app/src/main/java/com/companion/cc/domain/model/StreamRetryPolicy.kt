package com.companion.cc.domain.model

object StreamRetryPolicy {
    private const val MAX_RETRIES = 2
    private const val DEFAULT_DELAY_MILLIS = 1_000L
    private const val MAX_DELAY_MILLIS = 30_000L

    fun shouldRetry(error: StreamHttpException, attempt: Int): Boolean =
        attempt in 0 until MAX_RETRIES && error.isRetryable()

    fun delayMillis(error: StreamHttpException): Long =
        ((error.retryAfterSeconds ?: 1L) * 1_000L).coerceIn(
            DEFAULT_DELAY_MILLIS,
            MAX_DELAY_MILLIS
        )
}
