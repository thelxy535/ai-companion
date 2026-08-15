package com.companion.cc.data.local

import android.content.Context
import com.companion.cc.domain.model.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 伴侣配置加载器
 *
 * 从 assets/companions.json 加载人格配置
 */
@Singleton
class CompanionConfigLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private var cachedConfig: CompanionsConfig? = null

    /**
     * 加载所有伴侣配置
     *
     * @return 配置对象
     */
    fun loadConfig(): CompanionsConfig {
        // 使用缓存
        cachedConfig?.let { return it }

        try {
            val jsonString = context.assets.open("companions.json")
                .bufferedReader()
                .use { it.readText() }

            android.util.Log.d("CompanionConfigLoader", "=== 开始加载配置 ===")
            android.util.Log.d("CompanionConfigLoader", "JSON 长度: ${jsonString.length} 字符")

            val config = gson.fromJson(jsonString, CompanionsConfig::class.java)

            android.util.Log.d("CompanionConfigLoader", "解析成功: ${config.companions.size} 个伴侣")
            config.companions.forEach { companion ->
                android.util.Log.d("CompanionConfigLoader", "--- 伴侣: ${companion.name} (${companion.id}) ---")
                android.util.Log.d("CompanionConfigLoader", "  核心特质: ${companion.personality.coreTraits.take(3)}")
                android.util.Log.d("CompanionConfigLoader", "  说话风格: ${companion.personality.speakingStyle.tone}")
                android.util.Log.d("CompanionConfigLoader", "  使用emoji: ${companion.personality.speakingStyle.useEmoji}")
                android.util.Log.d("CompanionConfigLoader", "  System Prompt 长度: ${companion.prompts.system.length}")
                android.util.Log.d("CompanionConfigLoader", "  System Prompt 前50字: ${companion.prompts.system.take(50)}")
            }

            cachedConfig = config
            return config
        } catch (e: Exception) {
            android.util.Log.e("CompanionConfigLoader", "加载配置失败", e)
            return getDefaultConfig()
        }
    }

    /**
     * 根据ID获取伴侣配置
     *
     * @param companionId 伴侣ID
     * @return 伴侣配置，如果不存在返回null
     */
    fun getCompanionConfig(companionId: String): CompanionConfig? {
        return loadConfig().companions.find { it.id == companionId }
    }

    /**
     * 获取所有启用的伴侣
     *
     * @return 启用的伴侣列表
     */
    fun getEnabledCompanions(): List<CompanionConfig> {
        return loadConfig().companions.filter { it.enabled }
    }

    /**
     * 重新加载配置（清除缓存）
     */
    fun reload() {
        cachedConfig = null
    }

    /**
     * 获取默认配置（当加载失败时使用）
     */
    private fun getDefaultConfig(): CompanionsConfig {
        return CompanionsConfig(
            version = "1.0",
            companions = listOf(
                CompanionConfig(
                    id = "muse",
                    name = "缪斯",
                    emoji = "🎭",
                    avatar = "",
                    enabled = true,
                    personality = PersonalityConfig(
                        coreTraits = listOf("理性", "冷静", "智慧"),
                        background = "缪斯是希腊神话中的智慧女神",
                        speakingStyle = SpeakingStyle(
                            tone = "冷静",
                            vocabularyLevel = "学术",
                            sentenceLength = "中等",
                            useEmoji = false,
                            useExclamation = false,
                            formality = "正式"
                        ),
                        interests = listOf("哲学", "科学", "艺术"),
                        values = listOf("真理", "逻辑", "理性"),
                        relationship = Relationship(
                            role = "引导者",
                            distance = "适度",
                            interactionStyle = "引导式",
                            addressUser = "你"
                        ),
                        behaviorPatterns = listOf("提出问题", "逻辑分析")
                    ),
                    prompts = Prompts(
                        system = "你是缪斯，智慧女神",
                        greeting = listOf("你好"),
                        farewell = listOf("再见"),
                        fallback = listOf("这是个有趣的问题")
                    ),
                    emotionalModel = EmotionalModelConfig(
                        defaultMood = Mood.CALM,
                        moodStability = 0.8f,
                        energyRecoveryRate = 0.05f,
                        stressThreshold = 0.7f,
                        affectionGrowthRate = 0.03f,
                        moodTransitions = emptyMap()
                    ),
                    memoryPreferences = MemoryPreferences(
                        importanceThreshold = 0.7f,
                        summaryFrequency = "daily",
                        rememberTopics = listOf("哲学", "思考"),
                        forgetTopics = listOf("闲聊")
                    )
                )
            )
        )
    }
}

/**
 * Gson 模块配置
 */
object GsonConfig {
    fun create(): Gson {
        return GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Mood::class.java, MoodTypeAdapter())
            .create()
    }
}

/**
 * Mood 枚举的 Gson 适配器
 */
class MoodTypeAdapter : com.google.gson.TypeAdapter<Mood>() {
    override fun write(out: com.google.gson.stream.JsonWriter, value: Mood?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.name)
        }
    }

    override fun read(reader: com.google.gson.stream.JsonReader): Mood? {
        if (reader.peek() == com.google.gson.stream.JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        val value = reader.nextString()
        return try {
            Mood.valueOf(value)
        } catch (e: IllegalArgumentException) {
            Mood.CALM  // 默认值
        }
    }
}
