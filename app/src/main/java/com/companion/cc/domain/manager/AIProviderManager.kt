package com.companion.cc.domain.manager

import com.companion.cc.data.local.SettingsManager
import com.companion.cc.data.remote.model.ModelsResponse
import com.companion.cc.domain.model.AIProvider
import com.companion.cc.domain.model.ProviderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI供应商管理器
 * 负责自动识别供应商、验证密钥、加载模型列表
 */
@Singleton
class AIProviderManager @Inject constructor(
    private val settingsManager: SettingsManager,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    /**
     * 从API实时加载模型列表
     * 支持所有兼容 OpenAI API 格式的供应商
     */
    suspend fun loadModelsFromAPI(baseUrl: String, apiKey: String): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val normalizedBaseUrl = normalizeApiBaseUrl(baseUrl)
                android.util.Log.d("AIProviderManager", "开始加载模型列表")
                android.util.Log.d("AIProviderManager", "Base URL: $normalizedBaseUrl")

                // 检测供应商类型
                val provider = AIProvider.detectProvider(apiKey, normalizedBaseUrl)

                // 对于某些供应商，直接返回预定义的模型列表
                if (shouldUsePredefinedModels(provider)) {
                    android.util.Log.d("AIProviderManager", "使用预定义模型列表: ${provider.displayName}")
                    return@withContext Result.success(provider.models)
                }

                // 构建完整的 models 端点 URL
                val modelsUrl = buildModelsUrl(normalizedBaseUrl, provider)

                android.util.Log.d("AIProviderManager", "Models URL: $modelsUrl")

                // 构建请求
                val request = buildModelsRequest(modelsUrl, apiKey, provider)

                android.util.Log.d("AIProviderManager", "发送 HTTP 请求...")

                // 执行请求 - 在 IO 线程中执行同步调用
                val response = okHttpClient.newCall(request).execute()

                android.util.Log.d("AIProviderManager", "HTTP 状态码: ${response.code}")

                if (response.isSuccessful) {
                    val body = response.body?.string()
                    android.util.Log.d("AIProviderManager", "响应体长度: ${body?.length}")

                    if (body != null) {
                        val models = parseModelsResponse(body, provider)
                        android.util.Log.d("AIProviderManager", "成功加载 ${models.size} 个模型")
                        android.util.Log.d("AIProviderManager", "模型列表: ${models.take(5).joinToString()}")
                        Result.success(models)
                    } else {
                        android.util.Log.e("AIProviderManager", "响应体为空")
                        // 返回预定义模型作为后备
                        Result.success(provider.models)
                    }
                } else {
                    val errorBody = response.body?.string()
                    android.util.Log.e("AIProviderManager", "API 调用失败: ${response.code} ${response.message}")
                    android.util.Log.e("AIProviderManager", "错误详情: $errorBody")

                    // API调用失败，返回预定义模型作为后备
                    android.util.Log.d("AIProviderManager", "使用后备预定义模型列表")
                    Result.success(provider.models)
                }
            } catch (e: Exception) {
                android.util.Log.e("AIProviderManager", "加载模型失败", e)

                // 尝试返回预定义模型作为后备
                val provider = AIProvider.detectProvider(apiKey, normalizeApiBaseUrl(baseUrl))
                if (provider.models.isNotEmpty()) {
                    android.util.Log.d("AIProviderManager", "异常情况下使用预定义模型")
                    Result.success(provider.models)
                } else {
                    Result.failure(e)
                }
            }
        }
    }

    /**
     * 判断是否应该使用预定义模型列表
     */
    private fun shouldUsePredefinedModels(provider: AIProvider): Boolean {
        return when (provider) {
            // 这些供应商的 /models 接口可能不标准或需要特殊认证
            AIProvider.ANTHROPIC,   // Claude 没有标准的 models 接口
            AIProvider.GOOGLE,      // Gemini API 结构不同
            AIProvider.BAIDU,       // 百度需要特殊认证流程
            AIProvider.ZHIPU        // 智谱 API 可能需要特殊处理
            -> true
            else -> false
        }
    }

    /**
     * 构建模型列表 URL
     */
    private fun buildModelsUrl(baseUrl: String, provider: AIProvider): String {
        return when (provider) {
            AIProvider.GOOGLE -> {
                // Google Gemini API 使用不同的端点
                if (baseUrl.endsWith("/")) {
                    "${baseUrl}models"
                } else {
                    "$baseUrl/models"
                }
            }
            else -> {
                // 标准 OpenAI 风格
                if (baseUrl.endsWith("/")) {
                    "${baseUrl}models"
                } else {
                    "$baseUrl/models"
                }
            }
        }
    }

    /**
     * 构建模型请求
     */
    private fun buildModelsRequest(url: String, apiKey: String, provider: AIProvider): Request {
        val builder = Request.Builder()
            .url(url)
            .get()

        // 根据供应商设置认证头
        when (provider) {
            AIProvider.ANTHROPIC -> {
                builder.header("x-api-key", apiKey)
                builder.header("anthropic-version", "2023-06-01")
            }
            AIProvider.GOOGLE -> {
                // Google 使用查询参数
                builder.url("$url?key=$apiKey")
            }
            else -> {
                // 标准 Bearer 认证
                builder.header("Authorization", "Bearer $apiKey")
            }
        }

        builder.header("Content-Type", "application/json")
        return builder.build()
    }

    /**
     * 解析模型响应
     */
    private fun parseModelsResponse(body: String, provider: AIProvider): List<String> {
        return try {
            when (provider) {
                AIProvider.GOOGLE -> {
                    // Google 的响应格式可能不同
                    val modelsResponse = gson.fromJson(body, ModelsResponse::class.java)
                    modelsResponse.data.map { it.id }
                }
                else -> {
                    // 标准 OpenAI 格式
                    val modelsResponse = gson.fromJson(body, ModelsResponse::class.java)
                    modelsResponse.data.map { it.id }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AIProviderManager", "解析模型响应失败", e)
            // 返回空列表，调用方会使用预定义模型
            emptyList()
        }
    }

    /**
     * 检测并配置供应商
     * @param apiKey API密钥
     * @param baseUrl 可选的baseUrl（如果用户手动指定）
     * @return 供应商配置
     */
    suspend fun detectAndConfigureProvider(
        apiKey: String,
        baseUrl: String? = null
    ): Result<ProviderConfig> {
        // 1. 验证API密钥不为空
        if (apiKey.isBlank()) {
            return Result.failure(IllegalArgumentException("API密钥不能为空"))
        }

        // 2. 获取有效的 Base URL（用户输入 > 已保存的 > 默认的硅基流动）
        val savedBaseUrl = settingsManager.baseUrlFlow.first()
        val effectiveBaseUrl = normalizeApiBaseUrl(baseUrl?.takeIf { it.isNotBlank() }
            ?: savedBaseUrl.takeIf { it.isNotBlank() }
            ?: "https://api.siliconflow.cn/v1")

        android.util.Log.d("AIProviderManager", "使用 Base URL: $effectiveBaseUrl")

        // 3. 根据 Base URL 识别供应商
        val provider = AIProvider.detectProvider(apiKey, effectiveBaseUrl)

        android.util.Log.d("AIProviderManager", "识别为: ${provider.displayName}")

        // 4. 确定最终 Base URL
        val finalBaseUrl = when {
            provider != AIProvider.CUSTOM -> provider.baseUrl  // 识别成功，用供应商的
            effectiveBaseUrl.isNotBlank() -> effectiveBaseUrl  // 识别为 CUSTOM，用传入的
            else -> return Result.failure(IllegalArgumentException("无法识别供应商，请手动选择"))
        }

        // 5. 尝试从API加载模型列表
        val availableModels = loadModelsFromAPI(finalBaseUrl, apiKey)
            .getOrElse {
                android.util.Log.w("AIProviderManager", "API加载失败，使用预定义模型")
                provider.models
            }

        if (availableModels.isEmpty()) {
            return Result.failure(IllegalArgumentException("无法加载模型列表"))
        }

        // 6. 选择默认模型
        val currentModel = settingsManager.modelFlow.first()
        val selectedModel = if (availableModels.contains(currentModel)) {
            currentModel
        } else {
            availableModels.firstOrNull() ?: ""
        }

        // 7. 保存配置
        settingsManager.saveApiKey(apiKey)
        settingsManager.saveBaseUrl(finalBaseUrl)
        if (selectedModel.isNotEmpty()) {
            settingsManager.saveModel(selectedModel)
        }

        // 8. 返回配置
        val config = ProviderConfig(
            provider = provider,
            apiKey = apiKey,
            baseUrl = finalBaseUrl,
            selectedModel = selectedModel,
            availableModels = availableModels
        )

        return Result.success(config)
    }

    /**
     * 获取当前供应商配置
     */
    suspend fun getCurrentConfig(): ProviderConfig? {
        val apiKey = settingsManager.apiKeyFlow.first() ?: return null
        val baseUrl = normalizeApiBaseUrl(settingsManager.baseUrlFlow.first())
        val model = settingsManager.modelFlow.first()

        val provider = AIProvider.detectProvider(apiKey, baseUrl)

        // 尝试从 API 加载模型列表，失败则使用预定义列表
        val availableModels = loadModelsFromAPI(baseUrl, apiKey)
            .getOrNull() ?: provider.models

        return ProviderConfig(
            provider = provider,
            apiKey = apiKey,
            baseUrl = baseUrl,
            selectedModel = model,
            availableModels = availableModels
        )
    }

    /**
     * 切换模型
     */
    suspend fun switchModel(modelName: String): Result<Unit> {
        val config = getCurrentConfig()
            ?: return Result.failure(IllegalStateException("未配置API密钥"))

        if (!config.availableModels.contains(modelName)) {
            return Result.failure(IllegalArgumentException(
                "模型 $modelName 不在 ${config.provider.displayName} 的可用列表中"
            ))
        }

        settingsManager.saveModel(modelName)
        return Result.success(Unit)
    }

    /**
     * 验证API密钥是否有效（可选：实际调用API测试）
     */
    suspend fun validateApiKey(apiKey: String, baseUrl: String? = null): Result<Boolean> {
        val provider = AIProvider.detectProvider(apiKey, baseUrl)

        val isFormatValid = AIProvider.validateApiKey(apiKey, provider)
        if (!isFormatValid) {
            return Result.failure(IllegalArgumentException(
                "密钥格式不正确：${getKeyFormatHint(provider)}"
            ))
        }

        // TODO: 可以在这里添加实际的API调用测试
        // 例如：调用models接口查看是否返回200

        return Result.success(true)
    }

    /**
     * 获取密钥格式提示
     */
    private fun getKeyFormatHint(provider: AIProvider): String {
        return when (provider) {
            AIProvider.SILICONFLOW -> "以 sk- 开头，长度40字符以上"
            AIProvider.OPENAI -> "以 sk- 开头，长度40-60字符"
            AIProvider.ANTHROPIC -> "以 sk-ant- 开头，长度30字符以上"
            AIProvider.ZHIPU -> "32位字母数字混合"
            AIProvider.DEEPSEEK -> "以 sk- 开头"
            AIProvider.MOONSHOT -> "以 sk- 开头"
            AIProvider.ALIBABA -> "以 sk- 开头"
            AIProvider.GOOGLE -> "长度30字符以上"
            AIProvider.MISTRAL -> "长度20字符以上"
            AIProvider.BAIDU -> "长度20字符以上"
            AIProvider.CUSTOM -> "根据你的API服务商要求"
        }
    }

    /**
     * 获取所有支持的供应商列表
     */
    fun getAllProviders(): List<AIProvider> {
        return AIProvider.values().filter { it != AIProvider.CUSTOM }
    }
}
