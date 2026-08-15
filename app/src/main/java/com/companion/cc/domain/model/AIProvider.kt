package com.companion.cc.domain.model

/**
 * AI供应商枚举 - 2026年主流模型版本
 */
enum class AIProvider(
    val displayName: String,
    val baseUrl: String,
    val keyPrefix: String,  // API密钥前缀，用于自动识别
    val models: List<String>
) {
    SILICONFLOW(
        displayName = "SiliconFlow",
        baseUrl = "https://api.siliconflow.cn/v1",
        keyPrefix = "sk-",
        models = listOf(
            // DeepSeek 系列
            "deepseek-ai/DeepSeek-V3",
            "deepseek-ai/DeepSeek-V2.5",

            // Qwen 系列
            "Qwen/Qwen2.5-72B-Instruct",
            "Qwen/Qwen2.5-32B-Instruct",
            "Qwen/Qwen2.5-14B-Instruct",
            "Qwen/Qwen2.5-7B-Instruct",
            "Qwen/QwQ-32B-Preview",

            // Meta Llama 系列
            "meta-llama/Meta-Llama-3.1-405B-Instruct",
            "meta-llama/Meta-Llama-3.1-70B-Instruct",
            "meta-llama/Meta-Llama-3.1-8B-Instruct",
            "meta-llama/Llama-3.2-3B-Instruct",

            // Yi 系列
            "01-ai/Yi-1.5-34B-Chat-16K",
            "01-ai/Yi-1.5-9B-Chat-16K",

            // Mistral 系列
            "mistralai/Mistral-7B-Instruct-v0.3",
            "mistralai/Mixtral-8x7B-Instruct-v0.1"
        )
    ),

    OPENAI(
        displayName = "OpenAI",
        baseUrl = "https://api.openai.com/v1",
        keyPrefix = "sk-",
        models = listOf(
            // GPT-4 系列
            "gpt-4o",
            "gpt-4o-mini",
            "gpt-4-turbo",
            "gpt-4-turbo-preview",
            "gpt-4",
            "gpt-4-32k",

            // GPT-3.5 系列
            "gpt-3.5-turbo",
            "gpt-3.5-turbo-16k",

            // o1 系列（推理模型）
            "o1-preview",
            "o1-mini"
        )
    ),

    ANTHROPIC(
        displayName = "Anthropic (Claude)",
        baseUrl = "https://api.anthropic.com/v1",
        keyPrefix = "sk-ant-",
        models = listOf(
            // Claude 3.5 系列
            "claude-3-5-sonnet-20241022",
            "claude-3-5-haiku-20241022",

            // Claude 3 系列
            "claude-3-opus-20240229",
            "claude-3-sonnet-20240229",
            "claude-3-haiku-20240307"
        )
    ),

    ZHIPU(
        displayName = "智谱AI (GLM)",
        baseUrl = "https://open.bigmodel.cn/api/paas/v4",
        keyPrefix = "",
        models = listOf(
            // GLM-4 系列
            "glm-4-plus",
            "glm-4-0520",
            "glm-4",
            "glm-4-air",
            "glm-4-airx",
            "glm-4-flash",

            // GLM-3 系列
            "glm-3-turbo"
        )
    ),

    DEEPSEEK(
        displayName = "DeepSeek",
        baseUrl = "https://api.deepseek.com/v1",
        keyPrefix = "sk-",
        models = listOf(
            // DeepSeek V3
            "deepseek-chat",
            "deepseek-reasoner",  // 推理模型

            // 代码专用
            "deepseek-coder"
        )
    ),

    MOONSHOT(
        displayName = "月之暗面 (Kimi)",
        baseUrl = "https://api.moonshot.cn/v1",
        keyPrefix = "sk-",
        models = listOf(
            "moonshot-v1-128k",
            "moonshot-v1-32k",
            "moonshot-v1-8k"
        )
    ),

    ALIBABA(
        displayName = "阿里云百炼 (Qwen)",
        baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
        keyPrefix = "sk-",
        models = listOf(
            // Qwen 系列
            "qwen-max",
            "qwen-max-longcontext",
            "qwen-plus",
            "qwen-turbo",
            "qwen-long",

            // QwQ 推理模型
            "qwq-32b-preview",

            // Qwen2.5 系列
            "qwen2.5-72b-instruct",
            "qwen2.5-32b-instruct",
            "qwen2.5-14b-instruct",
            "qwen2.5-7b-instruct"
        )
    ),

    GOOGLE(
        displayName = "Google (Gemini)",
        baseUrl = "https://generativelanguage.googleapis.com/v1beta",
        keyPrefix = "",
        models = listOf(
            // Gemini 2.0 系列
            "gemini-2.0-flash-exp",

            // Gemini 1.5 系列
            "gemini-1.5-pro",
            "gemini-1.5-flash",
            "gemini-1.5-flash-8b",

            // Gemini 1.0 系列
            "gemini-pro"
        )
    ),

    MISTRAL(
        displayName = "Mistral AI",
        baseUrl = "https://api.mistral.ai/v1",
        keyPrefix = "",
        models = listOf(
            // Large 系列
            "mistral-large-latest",
            "mistral-large-2411",

            // Medium 系列
            "mistral-medium-latest",

            // Small 系列
            "mistral-small-latest",

            // Mixtral 系列
            "open-mixtral-8x7b",
            "open-mixtral-8x22b",

            // Codestral（代码）
            "codestral-latest"
        )
    ),

    BAIDU(
        displayName = "百度 (文心一言)",
        baseUrl = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1",
        keyPrefix = "",
        models = listOf(
            // 文心4.0系列
            "ernie-4.0-8k",
            "ernie-4.0-turbo-8k",

            // 文心3.5系列
            "ernie-3.5-8k",
            "ernie-3.5-128k",

            // 文心Lite
            "ernie-lite-8k",
            "ernie-speed-128k"
        )
    ),

    CUSTOM(
        displayName = "自定义",
        baseUrl = "",
        keyPrefix = "",
        models = emptyList()
    );

    companion object {
        /**
         * 根据API密钥和baseUrl自动识别供应商
         */
        fun detectProvider(apiKey: String, baseUrl: String?): AIProvider {
            // 1. 先根据baseUrl精确匹配（最高优先级）
            if (!baseUrl.isNullOrBlank()) {
                // 精确匹配各供应商的 baseUrl
                values().forEach { provider ->
                    if (provider != CUSTOM && provider.baseUrl.isNotBlank()) {
                        // 移除协议和尾部斜杠后比较域名
                        val normalizedProviderUrl = provider.baseUrl
                            .removePrefix("https://")
                            .removePrefix("http://")
                            .removeSuffix("/")

                        val normalizedInputUrl = baseUrl
                            .removePrefix("https://")
                            .removePrefix("http://")
                            .removeSuffix("/")

                        // 如果输入的URL包含供应商的域名，直接返回
                        if (normalizedInputUrl.contains(normalizedProviderUrl, ignoreCase = true) ||
                            normalizedProviderUrl.contains(normalizedInputUrl, ignoreCase = true)) {
                            return provider
                        }
                    }
                }

                // URL关键词匹配（作为后备）
                return when {
                    baseUrl.contains("siliconflow", ignoreCase = true) -> SILICONFLOW
                    baseUrl.contains("openai", ignoreCase = true) -> OPENAI
                    baseUrl.contains("anthropic", ignoreCase = true) -> ANTHROPIC
                    baseUrl.contains("zhipu", ignoreCase = true) ||
                        baseUrl.contains("bigmodel", ignoreCase = true) -> ZHIPU
                    baseUrl.contains("deepseek", ignoreCase = true) -> DEEPSEEK
                    baseUrl.contains("moonshot", ignoreCase = true) -> MOONSHOT
                    baseUrl.contains("dashscope", ignoreCase = true) ||
                        baseUrl.contains("aliyun", ignoreCase = true) -> ALIBABA
                    baseUrl.contains("google", ignoreCase = true) ||
                        baseUrl.contains("gemini", ignoreCase = true) -> GOOGLE
                    baseUrl.contains("mistral", ignoreCase = true) -> MISTRAL
                    baseUrl.contains("baidu", ignoreCase = true) ||
                        baseUrl.contains("wenxin", ignoreCase = true) -> BAIDU
                    else -> CUSTOM
                }
            }

            // 2. 如果没有URL，根据API密钥特征推断（优先级较低，容易误判）
            return when {
                // Anthropic Claude (唯一特征明确的)
                apiKey.startsWith("sk-ant-") -> ANTHROPIC

                // 智谱AI (32位字母数字，特征明确)
                apiKey.length == 32 && apiKey.all { it.isLetterOrDigit() } -> ZHIPU

                // 其他 sk- 开头的无法准确区分，返回 CUSTOM 让用户手动选择
                // 因为 OpenAI、SiliconFlow、DeepSeek、Moonshot、Alibaba 都用 sk-
                apiKey.startsWith("sk-") -> CUSTOM

                // 无法识别
                else -> CUSTOM
            }
        }

        /**
         * 验证API密钥格式
         */
        fun validateApiKey(apiKey: String, provider: AIProvider): Boolean {
            if (apiKey.isBlank()) return false

            return when (provider) {
                SILICONFLOW, OPENAI, DEEPSEEK, MOONSHOT, ALIBABA -> {
                    apiKey.startsWith("sk-") && apiKey.length > 20
                }
                ANTHROPIC -> {
                    apiKey.startsWith("sk-ant-") && apiKey.length > 30
                }
                ZHIPU -> {
                    apiKey.length >= 20 && apiKey.all { it.isLetterOrDigit() || it == '.' }
                }
                GOOGLE -> {
                    apiKey.length >= 30
                }
                MISTRAL -> {
                    apiKey.length >= 20
                }
                BAIDU -> {
                    apiKey.length >= 20
                }
                CUSTOM -> apiKey.length >= 10
            }
        }

        /**
         * 根据模型名称推断供应商
         */
        fun detectProviderFromModel(modelName: String): AIProvider? {
            return when {
                modelName.startsWith("gpt-") -> OPENAI
                modelName.startsWith("claude-") -> ANTHROPIC
                modelName.startsWith("gemini-") -> GOOGLE
                modelName.startsWith("glm-") -> ZHIPU
                modelName.startsWith("deepseek-") -> DEEPSEEK
                modelName.startsWith("moonshot-") -> MOONSHOT
                modelName.startsWith("qwen") -> ALIBABA
                modelName.startsWith("ernie-") -> BAIDU
                modelName.contains("mistral", ignoreCase = true) -> MISTRAL
                else -> null
            }
        }
    }
}

/**
 * 供应商配置
 */
data class ProviderConfig(
    val provider: AIProvider,
    val apiKey: String,
    val baseUrl: String,
    val selectedModel: String,
    val availableModels: List<String>
)
