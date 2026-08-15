package com.companion.cc.domain.manager

import android.content.Context
import com.companion.cc.domain.usecase.MuseMoodState
import com.companion.cc.domain.usecase.XiaoCanMoodState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * System Prompt管理器
 * 负责加载和构建角色的System Prompt
 */
@Singleton
class SystemPromptManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val commonRules: String by lazy {
        loadPromptFromAssets("prompts/common_rules.txt")
    }

    private val musePromptTemplate: String by lazy {
        loadPromptFromAssets("prompts/muse_system_prompt.txt")
    }

    private val xiaocanPromptTemplate: String by lazy {
        loadPromptFromAssets("prompts/xiaocan_system_prompt.txt")
    }

    /**
     * 构建完整的System Prompt
     */
    fun buildSystemPrompt(
        companionId: String,
        moodState: Any,           // MuseMoodState or XiaoCanMoodState
        memoryContext: String,
        specialMode: String?
    ): String {
        val template = when (companionId) {
            "muse" -> musePromptTemplate
            "xiaocan" -> xiaocanPromptTemplate
            else -> throw IllegalArgumentException("Unknown companion: $companionId")
        }

        return when (companionId) {
            "muse" -> {
                val state = moodState as MuseMoodState
                buildMusePrompt(template, state, memoryContext, specialMode)
            }
            "xiaocan" -> {
                val state = moodState as XiaoCanMoodState
                buildXiaoCanPrompt(template, state, memoryContext, specialMode)
            }
            else -> template
        }
    }

    private fun buildMusePrompt(
        template: String,
        state: MuseMoodState,
        memoryContext: String,
        specialMode: String?
    ): String {
        val modeInstruction = when (specialMode) {
            "verbose" -> MUSE_MODE_VERBOSE
            "cold" -> MUSE_MODE_COLD
            else -> MUSE_MODE_NORMAL
        }

        // 组合公共规则 + 角色 Prompt
        val fullPrompt = """
$commonRules

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

$template
        """.trimIndent()

        return fullPrompt
            .replace("{mood_state}", state.getMoodState())
            .replace("{relationship_level}", state.relationshipLevel.toString())
            .replace("{memory_context}", memoryContext)
            .replace("{mode_instruction}", modeInstruction)
    }

    private fun buildXiaoCanPrompt(
        template: String,
        state: XiaoCanMoodState,
        memoryContext: String,
        specialMode: String?
    ): String {
        val modeInstruction = when (specialMode) {
            "care" -> XIAOCAN_MODE_CARE
            else -> XIAOCAN_MODE_NORMAL
        }

        // 组合公共规则 + 角色 Prompt
        val fullPrompt = """
$commonRules

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

$template
        """.trimIndent()

        return fullPrompt
            .replace("{care_level}", state.getCareLevel().toString())
            .replace("{emotion_state}", state.getEmotionState())
            .replace("{memory_context}", memoryContext)
            .replace("{mode_instruction}", modeInstruction)
    }

    private fun loadPromptFromAssets(filename: String): String {
        return context.assets.open(filename).bufferedReader().use { it.readText() }
    }

    companion object {
        private const val MUSE_MODE_VERBOSE = "\n\n【话痨模式】用户在询问你的观点或讨论深度话题。解除字数限制，详细展开，不要客气。"
        private const val MUSE_MODE_COLD = "\n\n【冷淡模式】用户在敷衍你。只需1-2字回复，表达你的不满。"
        private const val MUSE_MODE_NORMAL = ""

        private const val XIAOCAN_MODE_CARE = "\n\n【关心模式】用户情绪不佳或自我否定。用温暖但认真的语气表达关心，可以说长一点。"
        private const val XIAOCAN_MODE_NORMAL = ""
    }
}
