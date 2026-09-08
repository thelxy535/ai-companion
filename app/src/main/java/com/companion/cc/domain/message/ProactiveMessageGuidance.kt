package com.companion.cc.domain.message

object ProactiveMessageGuidance {
    fun build(motivation: String, interruptionCost: Float, continuity: String): String {
        val lines = mutableListOf(
            "把这条消息写成生活里刚冒出来的一点念头，而不是安排给用户的事项",
            "不要像提醒、打卡或任务通知，也不要使用客服式的关心清单",
            "只说一个具体的小片段或感受，不要为了显得主动而堆多个话题"
        )
        if (continuity.isBlank()) lines += "即使没有连续性，也要从当下的一个真实细节说起，不要发空泛的‘在吗’"
        if (interruptionCost >= 0.7f) lines += "语气轻一点，给对方留下可以随时不回复的空间"
        else lines += "可以自然地多说半句，但不要强迫对方接话"
        lines += "这次主动的动机是：$motivation。不要直接解释动机。"
        if (continuity.isNotBlank()) lines += "可以轻轻接住这段连续性，但不要像复述记录：$continuity"
        return "【主动消息的感觉】\n" + lines.joinToString("\n") { "- $it" }
    }
}
