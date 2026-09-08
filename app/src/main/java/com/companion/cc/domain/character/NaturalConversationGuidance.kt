package com.companion.cc.domain.character

import com.companion.cc.domain.model.EmotionalState
import com.companion.cc.domain.model.Mood

/** Soft conversational cadence cues; these shape rhythm without scripting content. */
object NaturalConversationGuidance {
    fun build(
        userMessage: String,
        temperament: TemperamentProfile,
        emotionalState: EmotionalState
    ): String {
        val shortInput = userMessage.trim().length <= 4
        val restrained = temperament.expressiveness <= 0.35f || temperament.interruptionCost >= 0.7f
        val lowEnergy = emotionalState.energy <= 0.35f || emotionalState.mood == Mood.TIRED
        val lines = mutableListOf<String>()

        if (restrained || lowEnergy || shortInput) {
            lines += "不用把话说满，可以只回应眼前这一点，留一点自然的停顿"
            lines += "不要为了完整而总结，也不要为了显得热情强行展开"
        }
        if (temperament.expressiveness >= 0.65f || temperament.shareImpulse >= 0.7f) {
            lines += "如果这句话勾起了真实兴致，可以顺着这个兴致多说一点"
            lines += "可以接住对方的细节，最多自然地问一个问题，不要把聊天变成采访"
        }
        if (lines.isEmpty()) {
            lines += "保持像真实聊天一样有收有放：不必每次解释完整，也不必每次追加问题"
        }
        return "【自然聊天的节奏】\n" + lines.joinToString("\n") { "- $it" } +
            "\n不要复述这些提示，不要把回复写成客服式的完整答复。"
    }
}
