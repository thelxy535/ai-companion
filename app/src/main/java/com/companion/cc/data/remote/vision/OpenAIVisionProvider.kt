package com.companion.cc.data.remote.vision

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.companion.cc.data.local.SettingsManager
import com.companion.cc.domain.model.VisionAnalysis
import com.companion.cc.domain.model.VisionError
import com.companion.cc.domain.vision.VisionProvider
import com.companion.cc.domain.vision.VisionResponseParser
import com.companion.cc.util.Logger
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * OpenAI GPT-4 Vision Provider
 *
 * 使用 GPT-4V / GPT-4 Turbo with Vision 进行图片理解
 *
 * API 文档: https://platform.openai.com/docs/guides/vision
 */
class OpenAIVisionProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) : VisionProvider {

    override val providerName: String = "OpenAI GPT-4 Vision"

    override val maxImageSize: Long = 20 * 1024 * 1024  // 20MB

    override suspend fun isAvailable(): Boolean {
        val apiKey = settingsManager.apiKeyFlow.first()
        val baseUrl = settingsManager.baseUrlFlow.first()
        return !apiKey.isNullOrBlank() && baseUrl.isNotBlank()
    }

    override suspend fun analyzeImage(
        imageUri: Uri,
        userText: String,
        conversationContext: List<String>
    ): VisionAnalysis = withContext(Dispatchers.IO) {
        val apiKey = settingsManager.apiKeyFlow.first()
            ?: throw VisionError.ApiError("API Key 未配置")
        val baseUrl = settingsManager.baseUrlFlow.first().trimEnd('/')

        if (apiKey.isBlank()) {
            throw VisionError.ApiError("API Key 未配置")
        }

        try {
            // 1. 将图片转换为 Base64
            val base64Image = encodeImageToBase64(imageUri)

            // 2. 构建视觉理解 Prompt（结构化提取）
            val visionPrompt = buildVisionPrompt(userText, conversationContext)

            // 3. 构建请求
            val request = VisionRequest(
                model = "gpt-4-vision-preview",  // 或 "gpt-4-turbo" (2024年后支持视觉)
                messages = listOf(
                    VisionMessage(
                        role = "user",
                        content = listOf(
                            VisionContent.Text(text = visionPrompt),
                            VisionContent.Image(
                                imageUrl = VisionImageUrl(
                                    url = "data:image/jpeg;base64,$base64Image"
                                )
                            )
                        )
                    )
                ),
                maxTokens = 500,  // 限制输出长度，避免冗长描述
                temperature = 0.3  // 降低随机性，保证结构化输出
            )

            val requestBody = gson.toJson(request).toRequestBody("application/json".toMediaType())

            val httpRequest = Request.Builder()
                .url("$baseUrl/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            // 4. 执行请求
            val response = okHttpClient.newBuilder()
                .readTimeout(60, TimeUnit.SECONDS)  // 视觉模型响应较慢
                .build()
                .newCall(httpRequest)
                .execute()

            if (!response.isSuccessful) {
                Logger.e("OpenAIVisionProvider", "API 请求失败: ${response.code}")
                throw VisionError.ApiError("视觉模型调用失败: ${response.code}", response.code.toString())
            }

            // 5. 解析响应
            val responseBody = response.body?.string()
                ?: throw VisionError.ApiError("响应为空")

            val visionResponse = gson.fromJson(responseBody, VisionResponse::class.java)
            val rawAnalysis = visionResponse.choices.firstOrNull()?.message?.content
                ?: throw VisionError.ApiError("视觉理解结果为空")

            // 6. 解析结构化结果
            return@withContext VisionResponseParser.parse(rawAnalysis, confidence = 0.8f)

        } catch (e: VisionError) {
            throw e
        } catch (e: Exception) {
            Logger.e("OpenAIVisionProvider", "视觉理解失败", e)
            throw VisionError.Unknown("图片理解失败: ${e.message}", e)
        }
    }

    /**
     * 构建视觉理解 Prompt（引导模型输出结构化信息）
     */
    private fun buildVisionPrompt(userText: String, context: List<String>): String {
        val contextPart = if (context.isNotEmpty()) {
            "最近对话:\n${context.takeLast(3).joinToString("\n")}\n\n"
        } else {
            ""
        }

        return """
你是一个图片理解助手。用户发送了一张图片，并附带了一段话："$userText"。

${contextPart}请分析这张图片，并以结构化的方式提取以下信息（只提取存在的内容，不要编造）：

1. 主要对象：图片中的人物、物品、动物等（用逗号分隔）
2. 环境：室内/户外、地点、场景类型
3. 动作：正在发生的动作或行为
4. 文字：图片中的文字内容（OCR）
5. 氛围：图片的整体情绪或氛围（欢乐、严肃、温馨等）
6. 语境理解：结合用户的话，推断用户为什么发送这张图片，以及图片与用户话语的关系

输出格式（严格遵守）：
主要对象: [列表]
环境: [描述]
动作: [列表]
文字: [内容]
氛围: [描述]
语境理解: [分析]

如果某项不存在，输出"无"。保持简洁，每项不超过30字。
        """.trimIndent()
    }

    /**
     * 将图片编码为 Base64
     */
    private fun encodeImageToBase64(imageUri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(imageUri)
            ?: throw VisionError.Unknown("无法读取图片文件")

        val bytes = inputStream.readBytes()
        inputStream.close()

        // 检查大小
        if (bytes.size > maxImageSize) {
            throw VisionError.ImageTooLarge(bytes.size.toLong(), maxImageSize)
        }

        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}

// ==================== API 数据模型 ====================

data class VisionRequest(
    val model: String,
    val messages: List<VisionMessage>,
    @SerializedName("max_tokens") val maxTokens: Int,
    val temperature: Double
)

data class VisionMessage(
    val role: String,
    val content: List<VisionContent>
)

sealed class VisionContent {
    data class Text(val type: String = "text", val text: String) : VisionContent()
    data class Image(
        val type: String = "image_url",
        @SerializedName("image_url") val imageUrl: VisionImageUrl
    ) : VisionContent()
}

data class VisionImageUrl(
    val url: String,
    val detail: String = "auto"  // "auto", "low", "high"
)

data class VisionResponse(
    val choices: List<VisionChoice>
)

data class VisionChoice(
    val message: VisionMessageResponse
)

data class VisionMessageResponse(
    val content: String
)
