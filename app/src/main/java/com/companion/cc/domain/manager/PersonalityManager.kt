package com.companion.cc.domain.manager

import com.companion.cc.data.local.CompanionConfigLoader
import com.companion.cc.domain.model.ApiParametersConfig
import com.companion.cc.domain.model.CompanionConfig
import com.companion.cc.domain.model.CompanionPersonality
import com.companion.cc.domain.model.EmotionalModelConfig
import com.companion.cc.domain.model.MemoryLayered
import com.companion.cc.domain.model.MemoryPreferences
import com.companion.cc.domain.model.Mood
import com.companion.cc.domain.model.PersonalityCategory
import com.companion.cc.domain.model.PersonalityConfig
import com.companion.cc.domain.model.Prompts
import com.companion.cc.domain.model.Relationship
import com.companion.cc.domain.model.SpeakingStyle
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 伴侣人格管理器
 *
 * 管理伴侣的人格配置和动态行为：
 * - 加载人格配置
 * - 生成 System Prompt
 * - 应用说话风格
 * - 调整行为模式
 * - 支持动态角色注册（自定义角色）
 *
 * 结合 Moeru-AI 的人格配置和 Phase 1 的记忆系统
 */
@Singleton
class PersonalityManager @Inject constructor(
    private val configLoader: CompanionConfigLoader,
    private val timeContextManager: TimeContextManager,
    private val eventTracker: EventTracker
) {
    // Resolver 缓存的配置（由 ChatViewModel 在 setCharacter 时填充）
    private val resolvedConfigs = ConcurrentHashMap<String, CompanionConfig>()

    /**
     * 缓存 resolver 解析的配置
     * 由 ChatViewModel.setCharacter() 调用
     */
    fun cacheResolvedConfig(config: CompanionConfig) {
        resolvedConfigs[config.id] = config
    }

    /**
     * 移除缓存的配置
     */
    fun removeResolvedConfig(companionId: String) {
        resolvedConfigs.remove(companionId)
    }

    /**
     * 获取伴侣配置
     * 优先返回 resolver 缓存的配置，fallback 到配置文件加载
     *
     * @param companionId 伴侣ID
     * @return 配置对象，如果未找到返回 null
     */
    fun getCompanionConfig(companionId: String): CompanionConfig? {
        // 1. 优先使用 resolver 缓存（由 ChatViewModel.setCharacter 填充）
        resolvedConfigs[companionId]?.let { return it }

        // 2. Fallback: 从配置文件加载内置角色
        return configLoader.getCompanionConfig(companionId)
    }

    /**
     * 生成 System Prompt
     *
     * 整合人格配置和永久记忆
     *
     * @param companionId 伴侣ID
     * @param permanentMemories 永久记忆
     * @return System Prompt 文本
     */
    fun generateSystemPrompt(
        companionId: String,
        permanentMemories: List<MemoryLayered.Permanent> = emptyList()
    ): String {
        val config = getCompanionConfig(companionId) ?: return getDefaultSystemPrompt()

        val parts = mutableListOf<String>()

        // 1. 基础 System Prompt
        parts.add(config.prompts.system)
        parts.add("")

        // 2. 永久记忆（如果有）
        if (permanentMemories.isNotEmpty()) {
            parts.add("## 核心记忆")
            permanentMemories.groupBy { it.category }.forEach { (category, memories) ->
                parts.add("### ${category.displayName()}")
                memories.forEach { memory ->
                    parts.add("- ${memory.trait}")
                }
            }
            parts.add("")
        }

        // 3. 行为指引
        parts.add("## 当前行为指引")
        parts.add("说话风格：${config.personality.speakingStyle.tone}")
        parts.add("词汇水平：${config.personality.speakingStyle.vocabularyLevel}")
        parts.add("称呼对方：${config.personality.relationship.addressUser}")

        return parts.joinToString("\n")
    }

    /**
     * 生成完整的 System Prompt（使用完整记忆上下文）
     *
     * @param companionId 伴侣ID
     * @param userId 用户ID
     * @param memoryContext 完整的记忆上下文（4层）
     * @return System Prompt 文本
     */
    suspend fun generateSystemPrompt(
        companionId: String,
        userId: String,
        memoryContext: com.companion.cc.domain.model.CompleteMemoryContext
    ): String {
        val config = getCompanionConfig(companionId)
        if (config == null) {
            android.util.Log.e("PersonalityManager", "配置为 null！companionId=$companionId")
            return getDefaultSystemPrompt()
        }

        val parts = mutableListOf<String>()

        // 1. 基础 System Prompt
        parts.add(config.prompts.system)
        parts.add("")

        // 2. 当前日期时间
        parts.add(getCurrentDateTime())
        parts.add("")

        // 3. 真实感知上下文（时间、事件、用户状态）
        try {
            val timeContext = timeContextManager.getTimeContext(userId, companionId)
            val eventContext = eventTracker.getEventContext(userId, companionId)

            parts.add("## 时间上下文")
            parts.add("- 当前时间段：${timeContext.timePeriod.displayName}")
            if (timeContext.lastInteractionTime != null) {
                parts.add("- ${timeContext.toHumanReadable()}")
            }
            parts.add("")

            parts.add("## 用户状态")
            parts.add("- ${eventContext.messageRhythm.toHumanReadable()}")
            parts.add("- 急迫程度：${eventContext.userState.urgencyLevel.displayName}")
            parts.add("- 专注程度：${eventContext.userState.attentionLevel.displayName}")
            parts.add("")
        } catch (e: Exception) {
            android.util.Log.e("PersonalityManager", "获取感知上下文失败", e)
        }

        // 4. 永久记忆（核心特质）
        if (memoryContext.permanent.isNotEmpty()) {
            parts.add("## 核心记忆")
            memoryContext.permanent.groupBy { it.category }.forEach { (category, memories) ->
                parts.add("### ${category.displayName()}")
                memories.forEach { memory ->
                    parts.add("- ${memory.trait}")
                }
            }
            parts.add("")
        }

        // 4. 长期记忆（重要事件）
        if (memoryContext.longTerm.isNotEmpty()) {
            parts.add("## 重要记忆")
            memoryContext.longTerm.forEach { memory ->
                parts.add("- ${memory.content}")
            }
            parts.add("")
        }

        // 5. 中期记忆（近期摘要）
        if (memoryContext.midTerm.isNotEmpty()) {
            parts.add("## 近期对话")
            memoryContext.midTerm.forEach { summary ->
                parts.add("- ${summary.summary}")
            }
            parts.add("")
        }

        // 5. 短期记忆（最近交流）
        if (memoryContext.shortTerm.isNotEmpty()) {
            parts.add("## 刚才说的")
            memoryContext.shortTerm.forEach { recentMsg ->
                parts.add("- $recentMsg")
            }
            parts.add("")
        }

        // 6. 行为指引
        parts.add("## 当前行为指引")
        parts.add("说话风格：${config.personality.speakingStyle.tone}")
        parts.add("词汇水平：${config.personality.speakingStyle.vocabularyLevel}")
        parts.add("称呼对方：${config.personality.relationship.addressUser}")

        return parts.joinToString("\n")
    }

    /**
     * 根据人格调整回复
     *
     * 应用说话风格：emoji、感叹号、语气词等
     *
     * @param response 原始回复
     * @param companionId 伴侣ID
     * @return 调整后的回复
     */
    fun adjustResponse(response: String, companionId: String): String {
        val config = getCompanionConfig(companionId) ?: return response
        val style = config.personality.speakingStyle

        var adjusted = response

        // 1. 处理emoji
        if (!style.useEmoji) {
            // 移除emoji（如果模型生成了）
            adjusted = adjusted.replace(Regex("[\\p{So}\\p{Cn}]"), "")
        }

        // 2. 处理感叹号
        if (!style.useExclamation) {
            // 替换感叹号为句号
            adjusted = adjusted.replace("！", "。").replace("!", ".")
        }

        // 3. 根据正式程度调整
        if (style.formality == "正式") {
            // 移除语气词
            adjusted = adjusted
                .replace("呀", "")
                .replace("呢", "")
                .replace("哦", "")
                .replace("啦", "")
                .replace("～", "")
        }

        return adjusted.trim()
    }

    /**
     * 获取打招呼语
     *
     * @param companionId 伴侣ID
     * @return 随机选择一个打招呼语
     */
    fun getGreeting(companionId: String): String {
        val config = getCompanionConfig(companionId) ?: return "你好"
        return config.prompts.greeting.randomOrNull() ?: "你好"
    }

    /**
     * 获取告别语
     *
     * @param companionId 伴侣ID
     * @return 随机选择一个告别语
     */
    fun getFarewell(companionId: String): String {
        val config = getCompanionConfig(companionId) ?: return "再见"
        return config.prompts.farewell.randomOrNull() ?: "再见"
    }

    /**
     * 获取兜底回复
     *
     * @param companionId 伴侣ID
     * @return 随机选择一个兜底回复
     */
    fun getFallback(companionId: String): String {
        val config = getCompanionConfig(companionId) ?: return "我在思考..."
        return config.prompts.fallback.randomOrNull() ?: "我在思考..."
    }

    /**
     * 检查主题是否应该记住
     *
     * @param companionId 伴侣ID
     * @param topic 主题
     * @return 是否应该记住
     */
    fun shouldRememberTopic(companionId: String, topic: String): Boolean {
        val config = getCompanionConfig(companionId) ?: return true

        // 检查忘记列表
        if (config.memoryPreferences.forgetTopics.any { topic.contains(it) }) {
            return false
        }

        // 检查记住列表
        if (config.memoryPreferences.rememberTopics.isEmpty()) {
            return true
        }

        return config.memoryPreferences.rememberTopics.any { topic.contains(it) }
    }

    /**
     * 获取重要性阈值
     *
     * @param companionId 伴侣ID
     * @return 重要性阈值
     */
    fun getImportanceThreshold(companionId: String): Float {
        val config = getCompanionConfig(companionId) ?: return 0.5f
        return config.memoryPreferences.importanceThreshold
    }

    /**
     * 获取所有启用的伴侣
     *
     * @return 伴侣配置列表
     */
    fun getAllCompanions(): List<CompanionConfig> {
        return configLoader.getEnabledCompanions()
    }

    /**
     * 默认 System Prompt
     */
    private fun getDefaultSystemPrompt(): String {
        return "你是一个AI助手。"
    }

    /**
     * 获取当前日期时间信息
     * @return 格式化的日期时间字符串
     */
    private fun getCurrentDateTime(): String {
        val now = LocalDateTime.now()

        // 日期格式：2024年8月14日 星期三
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 EEEE", Locale.CHINA)
        val dateStr = now.format(dateFormatter)

        // 时间格式：22:30:45
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        val timeStr = now.format(timeFormatter)

        // 时段判断
        val period = when (now.hour) {
            in 0..5 -> "凌晨"
            in 6..8 -> "早上"
            in 9..11 -> "上午"
            in 12..13 -> "中午"
            in 14..17 -> "下午"
            in 18..19 -> "傍晚"
            in 20..23 -> "晚上"
            else -> "夜间"
        }

        return "## 当前时间\n现在是：$dateStr $period $timeStr"
    }
}

/**
 * 人格类别显示名称扩展
 */
private fun PersonalityCategory.displayName(): String {
    return when (this) {
        PersonalityCategory.CORE_VALUES -> "核心价值观"
        PersonalityCategory.INTERESTS -> "兴趣爱好"
        PersonalityCategory.SPEAKING_STYLE -> "说话风格"
        PersonalityCategory.BEHAVIOR_PATTERN -> "行为模式"
        PersonalityCategory.RELATIONSHIP -> "关系定位"
        PersonalityCategory.BACKGROUND -> "背景故事"
    }
}
