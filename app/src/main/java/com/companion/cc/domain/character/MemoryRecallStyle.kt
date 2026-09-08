package com.companion.cc.domain.character

import com.companion.cc.domain.model.Attitude
import com.companion.cc.domain.model.EmotionalState
import com.companion.cc.domain.model.Mood

/** Lets different characters surface the same memory with different social instincts. */
object MemoryRecallStyle {
    fun build(
        temperament: TemperamentProfile,
        rhythm: CompanionRhythm,
        emotionalState: EmotionalState
    ): String {
        val cautious = temperament.interruptionCost >= 0.7f ||
            temperament.expressiveness <= 0.35f ||
            emotionalState.attitude == Attitude.COLD
        val eager = temperament.expressiveness >= 0.7f &&
            rhythm.memoryStickiness >= 0.65f &&
            emotionalState.mood != Mood.SAD && emotionalState.mood != Mood.ANXIOUS

        val style = when {
            cautious -> "谨慎确认：涉及私密、敏感或可能已经变化的记忆，先接住当前话题；确实要提及时，用‘我记得你以前提过……还这样吗？’这类留有退路的说法。"
            eager -> "主动提起：记忆和当前话题明显相关时，可以像想到一件小事一样顺手提到，不要郑重宣布‘我记得’。"
            else -> "自然带过：只在当前话题需要时轻轻带到，像熟人聊天中的联想，不要专门转题展示记忆。"
        }
        val moodNote = when (emotionalState.mood) {
            Mood.HAPPY, Mood.EXCITED -> "情绪轻快时可以多分享一点由记忆带出的细节。"
            Mood.SAD, Mood.ANXIOUS -> "情绪低落时不要借记忆强行煽情，宁可只留半句。"
            else -> "记忆只服务于当下交流，不要变成档案播报。"
        }
        return "【记忆提及方式】\n- $style\n- $moodNote\n- 不要为了证明记得而转题，不要编造记忆中没有的细节。"
    }
}
