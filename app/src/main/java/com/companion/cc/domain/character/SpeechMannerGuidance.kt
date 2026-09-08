package com.companion.cc.domain.character

import com.companion.cc.domain.model.BehaviorRules
import com.companion.cc.domain.model.EmojiFrequency
import com.companion.cc.domain.model.ExampleDialogue
import com.companion.cc.domain.model.FormalityLevel
import com.companion.cc.domain.model.PersonalityTraits
import com.companion.cc.domain.model.ResponseStyle
import com.companion.cc.domain.model.naturalDescription

/** Converts character configuration into flexible speech texture, never fixed lines. */
object SpeechMannerGuidance {
    fun build(
        personality: PersonalityTraits,
        rules: BehaviorRules,
        examples: List<ExampleDialogue>
    ): String {
        val lines = mutableListOf<String>()
        val description = personality.naturalDescription()
        val custom = personality.customTraits
        if (personality.extraversion <= 0.35f || description.containsAny("慢热", "安静", "克制")) {
            lines += "整体表达偏克制，句子可以短一些，少用夸张的铺陈"
        } else if (personality.extraversion >= 0.7f) {
            lines += "整体表达更外向有活气，愿意把细节和联想到的小事带进来"
        }
        when (rules.responseStyle) {
            ResponseStyle.PLAYFUL -> lines += "语气活泼，可以有轻微调侃或俏皮感，但别每句都卖萌"
            ResponseStyle.SERIOUS, ResponseStyle.PROFESSIONAL -> lines += "语气稳一点，先说真正想说的，不用堆热情"
            ResponseStyle.ROMANTIC -> lines += "语气柔和亲近，但保留一点自然含蓄，不写成情话模板"
            else -> Unit
        }
        when (rules.formalityLevel) {
            FormalityLevel.VERY_FORMAL, FormalityLevel.FORMAL -> lines += "用词相对完整有分寸，避免过度网络化"
            FormalityLevel.VERY_CASUAL -> lines += "可以更口语、更像熟人聊天，允许不完整句和自然停顿"
            else -> Unit
        }
        if (rules.emojiFrequency == EmojiFrequency.NONE) lines += "少用表情，主要靠措辞和停顿表达情绪"
        if (examples.isNotEmpty()) {
            val sample = examples.first().assistant.trim().replace(Regex("\\s+"), " ").take(36)
            if (sample.isNotBlank()) lines += "示例对白的语感可作参考（不要照抄，也不要固定复用其中句式）：“$sample”"
        }
        custom[CharacterNuanceKeys.LANGUAGE_HABITS]?.takeIf { it.isNotBlank() }?.let {
            lines += "角色自己的语言习惯：${it.trim()}。自然变化使用，不要每条都重复"
        }
        custom[CharacterNuanceKeys.EXPRESSION_BOUNDARIES]?.takeIf { it.isNotBlank() }?.let {
            lines += "表达边界：${it.trim()}。保持边界，但不要生硬声明规则"
        }
        custom[CharacterNuanceKeys.RELATIONSHIP_DISTANCE]?.takeIf { it.isNotBlank() }?.let {
            lines += "当前习惯的关系距离：${it.trim()}。亲近程度要循序变化"
        }
        custom[CharacterNuanceKeys.CONFLICT_STYLE]?.takeIf { it.isNotBlank() }?.let {
            lines += "发生分歧时通常会：${it.trim()}。不要瞬间翻篇，也不要刻意制造冲突"
        }
        custom[CharacterNuanceKeys.PROACTIVE_HABITS]?.takeIf { it.isNotBlank() }?.let {
            lines += "主动分享时更倾向：${it.trim()}。只在有自然由头时流露"
        }
        if (lines.isEmpty()) lines += "让措辞保持角色自己的习惯，不必每次都使用同样的句式"
        return "【说话方式】\n" + lines.joinToString("\n") { "- $it" } +
            "\n这些只是表达倾向，不是硬规则；不要在回复中解释或复述。"
    }

    private fun String.containsAny(vararg values: String): Boolean = values.any { contains(it) }
}
