package com.companion.cc.domain.memory

import kotlinx.serialization.Serializable

/** 反思候选类型的领域常量。 */
object NarrativeKinds {
    const val RELATIONSHIP = "relationship_narrative"
}

@Serializable
data class ReflectionCandidate(
    val kind: String,
    val title: String,
    val content: String,
    val confidence: Double = 0.5
)

@Serializable
data class ReflectionOutput(
    val candidates: List<ReflectionCandidate> = emptyList()
)

object ReflectionResponseParser {
    fun parse(raw: String): Result<ReflectionOutput> = runCatching {
        val normalized = raw.trim().let { text ->
            if (text.startsWith("```") && text.endsWith("```")) {
                text.removePrefix("```").removePrefix("json").trim().removeSuffix("```").trim()
            } else text
        }
        val result = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }.decodeFromString<ReflectionOutput>(normalized)
        require(result.candidates.size <= 8) { "反思候选数量超出限制" }
        result.candidates.forEach { candidate ->
            require(candidate.kind in ALLOWED_KINDS) { "不支持的反思类型: ${candidate.kind}" }
            require(candidate.title.length in 1..120) { "反思标题长度无效" }
            require(candidate.content.length in 1..2000) { "反思内容长度无效" }
            require(candidate.confidence in 0.0..1.0) { "反思置信度无效" }
        }
        result
    }

    private val ALLOWED_KINDS = setOf(
            "fact", "event", "emotion", "preference", "relationship_narrative", "observation", "commitment"
    )
}

object ReflectionPromptBuilder {
    fun build(source: String): String = """
        你是一个严谨的长期记忆整理器。请只从给定对话来源中提取可被用户审核的候选记忆。
        来源可能是一段带时间戳的多轮对话；请关注跨轮次的模式：关系互动、重复出现的话题、
        对话中作出的承诺、情绪的变化轨迹。
        不要编造，不要把不确定推测写成事实；没有可靠候选时返回空数组。
        来源中形如 [image] / [image-analysis] 的行表示用户发送的图片及其视觉理解结果，请视为与文字同等重要的经历。
        只输出 JSON，不要 Markdown，不要解释，格式必须是：
        {"candidates":[{"kind":"fact|event|emotion|preference|relationship_narrative|observation|commitment","title":"简短标题","content":"候选内容","confidence":0.0}]}
        对话来源：
        $source
    """.trimIndent()
}

/** 叙事定期自演化（V9PM 深度②）的模型输出。 */
@Serializable
data class NarrativeEvolution(
    val changed: Boolean = false,
    val narrative: String = ""
)

object NarrativeEvolutionParser {
    fun parse(raw: String): Result<NarrativeEvolution> = runCatching {
        val normalized = raw.trim().let { text ->
            if (text.startsWith("```") && text.endsWith("```")) {
                text.removePrefix("```").removePrefix("json").trim().removeSuffix("```").trim()
            } else text
        }
        val result = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }.decodeFromString<NarrativeEvolution>(normalized)
        require(result.narrative.length <= 200) { "叙事长度超出限制" }
        result
    }

    fun evolutionPrompt(current: String, transcript: String): String = """
        你是这段 AI 陪伴关系的叙述者。基于"当前叙事"和"近期对话"，判断角色对这段关系的理解是否需要更新。
        只有当近期对话揭示了新的关系变化（更亲密/产生隔阂/新默契/边界变化等）才更新；
        没有实质变化时 changed 返回 false。更新后的叙事要保留仍然成立的部分，第一人称，80 字以内。
        只输出 JSON，不要 Markdown，不要解释，格式：
        {"changed":true,"narrative":"更新后的完整关系理解"}
        当前叙事：
        $current
        近期对话：
        $transcript
    """.trimIndent()
}
