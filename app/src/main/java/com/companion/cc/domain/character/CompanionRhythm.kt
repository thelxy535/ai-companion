package com.companion.cc.domain.character

import com.companion.cc.domain.model.EmotionalState
import kotlinx.serialization.Serializable

/** Soft preferences, not a schedule: they nudge energy and willingness to speak. */
@Serializable
data class CompanionRhythm(
    val wakeHour: Float = 8f,
    val sleepHour: Float = 23f,
    val socialBattery: Float = 0.65f,
    val memoryStickiness: Float = 0.5f,
    val recoverySpeed: Float = 0.5f
) {
    companion object {
        fun earlyRiser() = CompanionRhythm(wakeHour = 6.5f, sleepHour = 22f, socialBattery = 0.65f, recoverySpeed = 0.65f)
        fun nightOwl() = CompanionRhythm(wakeHour = 10f, sleepHour = 2f, socialBattery = 0.7f, memoryStickiness = 0.65f)
    }
}

data class MindAdvanceResult(val state: InnerState, val cue: String)

object CompanionMindEngine {
    fun advance(
        previous: InnerState,
        emotionalState: EmotionalState,
        rhythm: CompanionRhythm,
        nowHour: Double,
        elapsedMs: Long
    ): MindAdvanceResult {
        val hour = nowHour.toFloat()
        val isRestWindow = if (rhythm.sleepHour > rhythm.wakeHour) {
            hour >= rhythm.sleepHour || hour < rhythm.wakeHour
        } else hour >= rhythm.sleepHour && hour < rhythm.wakeHour
        val elapsedHours = (elapsedMs.coerceAtLeast(0L) / 3_600_000f).coerceAtMost(24f)
        val targetEnergy = if (isRestWindow) 0.28f else 0.72f
        val energy = (emotionalState.energy * 0.55f + targetEnergy * 0.45f - elapsedHours * 0.008f)
            .coerceIn(0.08f, 1f)
        val batteryTarget = if (isRestWindow) rhythm.socialBattery * 0.65f else rhythm.socialBattery
        val battery = (previous.socialBattery * 0.75f + batteryTarget * 0.25f + rhythm.recoverySpeed * 0.01f)
            .coerceIn(0f, 1f)
        val cue = when {
            isRestWindow && energy < 0.45f -> "现在更想安静一点，慢慢说"
            !isRestWindow && rhythm.wakeHour >= 9f && hour >= 21f -> "夜里反而有一点自己的节奏"
            !isRestWindow -> "今天的精神慢慢回来了"
            else -> ""
        }
        return MindAdvanceResult(
            previous.copy(
                socialBattery = battery,
                energy = energy,
                sleepDebt = if (isRestWindow) (previous.sleepDebt * 0.98f) else (previous.sleepDebt + 0.01f).coerceAtMost(1f),
                updatedAt = System.currentTimeMillis()
            ),
            cue
        )
    }
}

data class MemoryResonanceResult(val state: InnerState, val cue: String)

object MemoryResonanceEngine {
    fun resonate(previous: InnerState, memory: String, relevance: Float, rhythm: CompanionRhythm): MemoryResonanceResult {
        if (memory.isBlank() || relevance < 0.65f) return MemoryResonanceResult(previous, "")
        val remembered = memory.trim().take(100)
        val state = previous.copy(
            caringAbout = remembered,
            lastMemoryResonance = remembered,
            shareImpulse = (previous.shareImpulse + rhythm.memoryStickiness * relevance * 0.08f).coerceIn(0f, 1f),
            updatedAt = System.currentTimeMillis()
        )
        val cue = if (rhythm.memoryStickiness >= 0.65f) {
            "这件事还在心里，刚刚又想起了和你有关的一点事"
        } else {
            "刚好联想到了一点和你有关的事"
        }
        return MemoryResonanceResult(state, cue)
    }
}
