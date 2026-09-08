package com.companion.cc.domain.message

import com.companion.cc.domain.character.TemperamentProfile
import com.companion.cc.domain.character.CompanionRhythm
import com.companion.cc.domain.character.InnerState
import com.companion.cc.domain.model.Attitude
import com.companion.cc.domain.model.EmotionalState
import com.companion.cc.domain.model.Mood

/**
 * 主动性引擎（V9PM 第 4 项）。
 *
 * 由"TA 想找你的强度"决定是否主动开口，而不是固定时段随机问候。
 * 输入：粘人度（clinginess）、心情、态度、压力、距离上次聊天时长。
 * 输出：0..1 的主动强度 + 一句话理由（用于 LLM 生成有原因的主动消息）。
 */
data class ProactiveDecision(
    val intensity: Float,
    val shouldReachOut: Boolean,
    val reason: String,
    val shouldHoldBack: Boolean = false,
    /** Human-readable reason that can be passed to the reply generator. */
    val motivation: String = reason,
    /** 0..1 soft concern about interrupting the user. */
    val interruptionCost: Float = 0.5f
)

object ProactiveEngine {

    private const val HOUR_MS = 3_600_000L

    /** 计算主动强度与触发理由。 */
    fun decide(
        temperament: TemperamentProfile,
        state: EmotionalState,
        sinceLastChatMs: Long,
        nowHour: Double,
        quietHoursStart: Int = 22,
        quietHoursEnd: Int = 8
        ,rhythm: CompanionRhythm = CompanionRhythm()
        ,innerState: InnerState = InnerState()
    ): ProactiveDecision {
        // 1) 粘人度基础需求
        val base = temperament.clinginess

        // 2) 心情修正：开心/兴奋更想分享，低落想被找，平静最低
        val moodBoost = when (state.mood) {
            Mood.EXCITED -> 0.25f
            Mood.HAPPY -> 0.18f
            Mood.CONTENT -> 0.10f
            Mood.SAD -> 0.15f
            Mood.ANXIOUS -> 0.20f
            Mood.CALM -> 0f
            Mood.TIRED -> -0.15f
        }

        // 3) 态度修正：亲昵更容易开口；闹别扭/冷战时想被哄但嘴硬（降低主动）
        val attitudeAdjust = when (state.attitude) {
            Attitude.WARM -> 0.15f
            Attitude.NEUTRAL -> 0f
            Attitude.UPSET -> -0.10f
            Attitude.COLD -> -0.25f
        }

        // 4) 压力修正：压力大反而更想找你说（粘人度受压力放大）
        val stressBoost = (state.stress - 0.5f).coerceIn(0f, 0.4f) * 0.5f

        // 5) 时间冷热：刚聊过不久（<2h）不再追发；越久没聊越想找（封顶 3 天）
        val elapsed = sinceLastChatMs.coerceAtLeast(0L)
        val recencyFactor = ((elapsed / HOUR_MS).toFloat() / 72f).coerceIn(0f, 1f) // 0-3天线性
        val tooSoon = elapsed < 2 * HOUR_MS

        // 6) 静默时段：22-8 不主动（强制）
        val inQuietHours = nowHour >= quietHoursStart || nowHour < quietHoursEnd

        // Personality only nudges the decision. It never forces a message by itself.
        val socialImpulse = (temperament.shareImpulse - temperament.interruptionCost) * 0.2f
        val unfinishedThoughtBoost = if (innerState.unfinishedThought.isNotBlank()) 0.1f else 0f
        val intensity = (base + moodBoost + attitudeAdjust + stressBoost + recencyFactor * 0.2f + socialImpulse + unfinishedThoughtBoost)
            .coerceIn(0f, 1f)
        val batteryPenalty = ((rhythm.socialBattery - innerState.socialBattery).coerceAtLeast(0f) * 0.35f)
        val batteryFactor = if (innerState.socialBattery < 0.25f) 0.45f else 1f
        val adjustedIntensity = (intensity * batteryFactor - batteryPenalty).coerceIn(0f, 1f)

        val shouldHoldBack = !inQuietHours && !tooSoon && adjustedIntensity < 0.5f && intensity >= 0.5f
        val shouldReachOut = !inQuietHours && !tooSoon && adjustedIntensity >= 0.5f

        val reason = when {
            !shouldReachOut && inQuietHours -> "现在是休息时段，不想打扰"
            !shouldReachOut && tooSoon -> "刚聊过不久，不想显得黏人"
            shouldHoldBack -> "有点想说，但今天的社交电量不太够，不想打扰"
            innerState.unfinishedThought.isNotBlank() -> "刚才有一点没说完，想接着告诉你"
            state.mood == Mood.EXCITED -> "有点兴奋，想第一时间跟你说"
            state.mood == Mood.SAD -> "有点低落，想听你说话"
            state.mood == Mood.ANXIOUS -> "有点不安，想确认你还在"
            temperament.shareImpulse >= 0.7f -> "刚想到一件小事，想跟你分享"
            temperament.interruptionCost >= 0.7f -> "有点想你，但先轻轻问一句"
            state.attitude == Attitude.WARM -> "想你了，随便聊两句也好"
            else -> "想跟你说说话"
        }

        return ProactiveDecision(
            intensity = adjustedIntensity,
            shouldReachOut = shouldReachOut,
            reason = reason,
            shouldHoldBack = shouldHoldBack,
            motivation = reason,
            interruptionCost = temperament.interruptionCost.coerceIn(0f, 1f)
        )
    }
}
