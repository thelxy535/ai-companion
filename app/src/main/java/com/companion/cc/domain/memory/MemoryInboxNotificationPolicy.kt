package com.companion.cc.domain.memory

object MemoryInboxNotificationPolicy {
    const val DEFAULT_COOLDOWN_MS = 24L * 60L * 60L * 1000L
    const val DEFAULT_THRESHOLD = 3

    fun shouldNotify(
        pendingCount: Int,
        now: Long,
        lastNotifiedAt: Long,
        threshold: Int = DEFAULT_THRESHOLD,
        cooldownMs: Long = DEFAULT_COOLDOWN_MS
    ): Boolean = pendingCount > 0 &&
        (pendingCount >= threshold || now - lastNotifiedAt >= cooldownMs)
}
