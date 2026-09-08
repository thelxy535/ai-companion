package com.companion.cc.domain.memory

import com.companion.cc.domain.model.PersonalityTraits

/** A small, human-readable preference surface for each character's memory style. */
enum class MemoryCapturePreference(
    val notificationCooldownMs: Long,
    val notificationThreshold: Int
) {
    ASK_FIRST(24L * 60L * 60L * 1000L, 3),
    AUTO_DAILY_PREFERENCES(24L * 60L * 60L * 1000L, 3),
    SHARED_EXPERIENCES(24L * 60L * 60L * 1000L, 3),
    LOW_DISTURBANCE(72L * 60L * 60L * 1000L, 5);

    fun mayAutoCapture(kind: String, confidence: Double): Boolean {
        if (confidence < 0.88 || kind == "commitment" || kind == NarrativeKinds.RELATIONSHIP) return false
        return when (this) {
            ASK_FIRST, LOW_DISTURBANCE -> false
            AUTO_DAILY_PREFERENCES -> kind in setOf("fact", "preference", "event")
            SHARED_EXPERIENCES -> kind == "event"
        }
    }

    companion object {
        fun from(customTraits: Map<String, String>): MemoryCapturePreference {
            val value = customTraits[PersonalityTraits.MEMORY_PREFERENCE_KEY].orEmpty().trim()
            return when {
                value.contains("自动记住") -> AUTO_DAILY_PREFERENCES
                value.contains("共同经历") -> SHARED_EXPERIENCES
                value.contains("低打扰") -> LOW_DISTURBANCE
                else -> ASK_FIRST
            }
        }
    }
}
