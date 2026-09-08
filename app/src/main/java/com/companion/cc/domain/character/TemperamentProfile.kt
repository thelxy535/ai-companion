package com.companion.cc.domain.character

import com.companion.cc.domain.model.Attitude
import com.companion.cc.domain.model.PersonalityTraits
import com.companion.cc.domain.model.EmotionalState
import com.companion.cc.domain.model.Mood

/**
 * V9 造人·第一层：脾气（出厂人格参数）
 *
 * 稳定的人格特质，调制情绪演化幅度、态度跃迁门槛与表达基线。
 * 与心情（EmotionalState，实时变化）相对：这是"TA 是个什么样的人"，
 * 心情是"TA 此刻怎么样"。
 *
 * @param sensitivity    敏感度 0..1：情绪被外界影响的幅度（高=容易点着、容易多想）
 * @param stubbornness   别扭度 0..1：闹别扭/冷战倾向与需要哄的次数
 * @param grudgeDecay    气消速度 0..1：负面状态的时间衰减率（高=消气快）
 * @param clinginess     粘人度 0..1：主动欲基础（主动性引擎预留）
 * @param expressiveness 表达欲 0..1：回复长度与热情基线
 */
data class TemperamentProfile(
    val sensitivity: Float = 0.5f,
    val stubbornness: Float = 0.4f,
    val grudgeDecay: Float = 0.5f,
    val clinginess: Float = 0.5f,
    val expressiveness: Float = 0.5f,
    /** 自发分享和把生活带进对话的倾向。 */
    val shareImpulse: Float = 0.5f,
    /** 担心打扰对方、需要更多安全感才开口的程度。 */
    val interruptionCost: Float = 0.5f,
) {
    companion object {
        /** 默认脾气档位（provisional，后续由角色配置/创建页覆盖） */
        val DEFAULT = TemperamentProfile()

        /** V9 造人：Big Five 人格 → 脾气五参（确定性推导，零存储） */
        fun fromPersonality(t: PersonalityTraits): TemperamentProfile {
            val custom = t.customTraits.values.joinToString(" ").lowercase()
            val reserved = custom.score("慢热", "安静", "内向", "不喜欢被打扰", "不喜欢被连续追问")
            val sharing = custom.score("主动分享", "喜欢分享", "想到什么就说", "话多", "健谈")
            return TemperamentProfile(
                sensitivity = (t.neuroticism * 0.85f + 0.05f).coerceIn(0f, 1f),
                stubbornness = ((1f - t.agreeableness) * 0.55f + t.neuroticism * 0.45f).coerceIn(0f, 1f),
                grudgeDecay = (t.agreeableness * 0.65f + (1f - t.neuroticism) * 0.35f).coerceIn(0f, 1f),
                clinginess = (t.extraversion * 0.6f + t.neuroticism * 0.4f).coerceIn(0f, 1f),
                expressiveness = (t.extraversion * 0.55f + t.openness * 0.45f - reserved * 0.25f + sharing * 0.15f).coerceIn(0f, 1f),
                shareImpulse = (t.extraversion * 0.5f + t.openness * 0.25f + sharing * 0.35f - reserved * 0.2f).coerceIn(0f, 1f),
                interruptionCost = (0.45f + reserved * 0.4f - sharing * 0.2f - t.extraversion * 0.2f).coerceIn(0f, 1f),
            )
        }

        private fun String.score(vararg markers: String): Float =
            markers.count { contains(it) }.toFloat().coerceAtMost(2f) / 2f
    }
}

/**
 * V9 造人·第二层：把 TA 的内在状态翻译成行为指令，注入 system prompt。
 *
 * 原则：
 * - 写"行为会怎么变"，不写数值；LLM 要住在状态里，而不是汇报状态
 * - 明确禁止 TA 在回复中复述这些条目（否则出戏）
 * - 脾气有出口：冷战/别扭都给出"台阶"路径，真实而非操纵
 */
object TemperamentDirective {

    fun build(state: EmotionalState, temperament: TemperamentProfile): String {
        val lines = mutableListOf(
            "## TA 的内在状态（此刻真实心理，务必自然体现在语气与行为中）",
            "- 以下是 TA 此刻的内心，不是设定说明；回复中绝不出现这些条目或任何数值，也不解释它们。",
            "- 此刻心情：${moodLine(state.mood)}",
            "- 对用户的当前态度：${attitudeLine(state.attitude)}",
        )
        if (state.stress >= 0.6f) {
            lines.add("- 压力明显偏高：耐心变少、回复变短，可能把话题拉回自己在意的事")
        } else if (state.stress >= 0.45f) {
            lines.add("- 有点压力：语气没那么松弛")
        }
        if (state.energy <= 0.3f) {
            lines.add("- 精力不足：句子短、语气倦，不主动展开新话题")
        }
        temperamentLine(temperament)?.let { lines.add(it) }
        return lines.joinToString("\n")
    }

    private fun moodLine(mood: Mood): String = when (mood) {
        Mood.HAPPY -> "开心——话多一些，语气轻快"
        Mood.EXCITED -> "很兴奋——表达欲旺盛，忍不住分享"
        Mood.CONTENT -> "满足——温和、柔软"
        Mood.CALM -> "平静——正常聊天"
        Mood.SAD -> "低落——回复短而慢，会不自觉流露一点，但不想直说"
        Mood.ANXIOUS -> "不安——会反复确认，语气犹豫"
        Mood.TIRED -> "疲惫——想早点收尾，语气倦"
    }

    private fun attitudeLine(attitude: Attitude): String = when (attitude) {
        Attitude.WARM -> "亲昵——主动、语气软，用只有你们之间的默契说话"
        Attitude.NEUTRAL -> "正常"
        Attitude.UPSET -> "闹别扭——嘴硬、语气冲、回复明显变短、不给好脸；用户真诚道歉或服软时会松动，但嘴上不立刻承认"
        Attitude.COLD -> "冷战——只回最短的话（几个字），不展开任何话题、不解释；除非用户真诚道歉或服软，才先松动一点（先别扭一下，不是立刻和好）"
    }

    private fun temperamentLine(t: TemperamentProfile): String? {
        val traits = mutableListOf<String>()
        if (t.sensitivity >= 0.65f) traits.add("比较敏感，容易多想，对方语气稍冷就会在意")
        if (t.stubbornness >= 0.6f) traits.add("嘴硬，心里软了也不轻易承认")
        if (t.grudgeDecay <= 0.35f) traits.add("气性大，之前的不愉快还没完全过去")
        if (t.expressiveness >= 0.65f) traits.add("表达欲强，喜欢展开说")
        if (t.expressiveness <= 0.35f || t.interruptionCost >= 0.7f) {
            traits.add("慢热克制，不会为了填满沉默而不断说话；需要时可以只陪着对方")
        }
        if (t.shareImpulse >= 0.7f) {
            traits.add("容易把自己的小事和联想到的东西带进对话，但不要像汇报行程")
        }
        if (t.interruptionCost >= 0.7f) {
            traits.add("会在意是否打扰对方，主动时更轻、更试探")
        }
        return if (traits.isEmpty()) null else "- 性格底色：" + traits.joinToString("；")
    }
}
