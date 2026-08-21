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
 * Google Gemini 1.5 Flash Vision Provider
 *
 * 使用 Gemini 1.5 Flash 进行图片理解（免费额度：1500次/天）
 *
 * API 文档: https://ai.google.dev/gemini-api/docs/vision
 * 获取免费 API Key: https://aistudio.google.com/app/apikey
 */
class GeminiVisionProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager,
    private val gson: Gson
) : VisionProvider {

    override val providerName: String = "Google Gemini 1.5 Flash"

    override val maxImageSize: Long = 20 * 1024 * 1024  // 20MB

    // 使用独立的 OkHttpClient，避免与主对话模型的认证冲突
    private val geminiHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun isAvailable(): Boolean {
        val apiKey = settingsManager.visionApiKeyFlow.first()
        return !apiKey.isNullOrBlank()
    }

    override suspend fun analyzeImage(
        imageUri: Uri,
        userText: String,
        conversationContext: List<String>
    ): VisionAnalysis = withContext(Dispatchers.IO) {
        val apiKey = settingsManager.visionApiKeyFlow.first()
            ?: throw VisionError.ApiError("Gemini API Key 未配置")

        if (apiKey.isBlank()) {
            throw VisionError.ApiError("Gemini API Key 未配置")
        }

        try {
            // 1. 将图片转换为 Base64
            val base64Image = encodeImageToBase64(imageUri)
            val mimeType = getMimeType(imageUri)

            // 2. 构建视觉理解 Prompt
            val visionPrompt = buildVisionPrompt(userText, conversationContext)

            // 3. 构建 Gemini 请求
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart.Text(text = visionPrompt),
                            GeminiPart.InlineData(
                                inlineData = GeminiInlineData(
                                    mimeType = mimeType,
                                    data = base64Image
                                )
                            )
                        )
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.3,
                    maxOutputTokens = 500
                )
            )

            val requestBody = gson.toJson(request).toRequestBody("application/json".toMediaType())

            // 4. Gemini API 端点（使用 gemini-2.5-flash 多模态模型）
            val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            val httpRequest = Request.Builder()
                .url(apiUrl)
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            // 5. 执行请求（使用独立的 HTTP 客户端）
            val response = geminiHttpClient.newCall(httpRequest).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "未知错误"
                Logger.e("GeminiVisionProvider", "API 请求失败: ${response.code}")

                // 解析 Gemini 错误信息
                val errorMessage = try {
                    val errorJson = gson.fromJson(errorBody, GeminiErrorResponse::class.java)
                    errorJson.error?.message ?: "视觉模型调用失败"
                } catch (e: Exception) {
                    "视觉模型调用失败: ${response.code}"
                }

                throw VisionError.ApiError(errorMessage, response.code.toString())
            }

            // 6. 解析响应（直接从 JSON 提取 text，避免 sealed class 反序列化问题）
            val responseBody = response.body?.string()
                ?: throw VisionError.ApiError("响应为空")

            // 手动解析 JSON，提取第一个 candidate 的 text
            val rawAnalysis = try {
                val jsonObject = gson.fromJson(responseBody, com.google.gson.JsonObject::class.java)
                jsonObject.getAsJsonArray("candidates")
                    ?.get(0)?.asJsonObject
                    ?.getAsJsonObject("content")
                    ?.getAsJsonArray("parts")
                    ?.get(0)?.asJsonObject
                    ?.get("text")?.asString
                    ?: throw VisionError.ApiError("响应中没有 text 字段")
            } catch (e: Exception) {
                Logger.e("GeminiVisionProvider", "JSON 解析失败", e)
                throw VisionError.ApiError("解析响应失败: ${e.message}")
            }

            // 7. 解析结构化结果
            return@withContext VisionResponseParser.parse(rawAnalysis, confidence = 0.85f)

        } catch (e: VisionError) {
            throw e
        } catch (e: Exception) {
            Logger.e("GeminiVisionProvider", "视觉理解失败", e)
            throw VisionError.Unknown("图片理解失败: ${e.message}", e)
        }
    }

    /**
     * 构建视觉理解 Prompt
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

    /**
     * 获取图片 MIME 类型
     */
    private fun getMimeType(imageUri: Uri): String {
        return context.contentResolver.getType(imageUri) ?: "image/jpeg"
    }
}

// ==================== Gemini API 数据模型 ====================

data class GeminiRequest(
    val contents: List<GeminiContent>,
    @SerializedName("generationConfig") val generationConfig: GeminiGenerationConfig
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

sealed class GeminiPart {
    data class Text(val text: String) : GeminiPart()
    data class InlineData(
        @SerializedName("inline_data") val inlineData: GeminiInlineData
    ) : GeminiPart()
}

data class GeminiInlineData(
    @SerializedName("mime_type") val mimeType: String,
    val data: String  // Base64 编码的图片
)

data class GeminiGenerationConfig(
    val temperature: Double,
    @SerializedName("maxOutputTokens") val maxOutputTokens: Int
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent?
)

data class GeminiErrorResponse(
    val error: GeminiError?
)

data class GeminiError(
    val message: String?
)
