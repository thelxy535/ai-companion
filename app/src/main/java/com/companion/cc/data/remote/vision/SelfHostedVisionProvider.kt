package com.companion.cc.data.remote.vision

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.companion.cc.BuildConfig
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
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * 自建 Qwen2.5-VL 视觉服务 Provider。
 *
 * 网关负责鉴权、限流和 Ollama 协议转换；App 不直接访问 11434，也不把
 * Ollama 管理接口暴露给用户设备。
 */
class SelfHostedVisionProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager,
    private val gson: Gson
) : VisionProvider {
    override val providerName: String = "我的视觉服务器（Qwen2.5-VL 3B）"
    override val maxImageSize: Long = 4 * 1024 * 1024

    private val gatewayBaseUrl = BuildConfig.SELF_HOSTED_VISION_BASE_URL
        .trim()
        .trimEnd('/')
        .also { baseUrl ->
            require(baseUrl.startsWith("https://")) { "自建视觉服务必须使用 HTTPS" }
        }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(280, TimeUnit.SECONDS)
        .build()

    override suspend fun isAvailable(): Boolean =
        !settingsManager.selfHostedDeviceTokenFlow.first().isNullOrBlank()

    /** 使用服务器终端生成的一次性配对码完成设备绑定。 */
    suspend fun pair(pairingCode: String): Result<Unit> = withContext(Dispatchers.IO) {
        val code = pairingCode.trim()
        if (code.isBlank()) return@withContext Result.failure(IllegalArgumentException("配对码不能为空"))

        try {
            val requestBody = gson.toJson(
                PairRequest(
                    pairingCode = code,
                    installationId = settingsManager.getOrCreateInstallationId()
                )
            ).toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url("$gatewayBaseUrl/v1/pair")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        VisionError.ApiError(pairErrorMessage(response.code), response.code.toString())
                    )
                }

                val token = gson.fromJson(body, PairResponse::class.java)?.deviceToken
                if (token.isNullOrBlank()) {
                    return@withContext Result.failure(VisionError.ApiError("服务器未返回设备令牌"))
                }

                settingsManager.saveSelfHostedDeviceToken(token)
                settingsManager.saveVisionServiceMode("SELF_HOSTED")
                Result.success(Unit)
            }
        } catch (error: VisionError) {
            Result.failure(error)
        } catch (error: SecurityException) {
            Logger.e(TAG, "自建视觉服务令牌保存失败", error)
            Result.failure(VisionError.ApiError("无法安全保存设备令牌，请检查设备安全设置后重试"))
        } catch (error: Exception) {
            Logger.e(TAG, "自建视觉服务配对失败", error)
            Result.failure(VisionError.NetworkError("无法连接自建视觉服务"))
        }
    }

    suspend fun clearPairing() {
        settingsManager.clearSelfHostedDeviceToken()
    }

    override suspend fun analyzeImage(
        imageUri: Uri,
        userText: String,
        conversationContext: List<String>
    ): VisionAnalysis = withContext(Dispatchers.IO) {
        val token = settingsManager.selfHostedDeviceTokenFlow.first()
            ?: throw VisionError.ApiError("请先在设置中配对我的视觉服务器")
        if (token.isBlank()) throw VisionError.ApiError("请先在设置中配对我的视觉服务器")

        val imageBytes = readImageBytes(imageUri)
        val mimeType = detectMimeType(imageBytes)
        val requestBody = gson.toJson(
            AnalyzeRequest(
                imageBase64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP),
                mimeType = mimeType,
                userText = userText.take(MAX_USER_TEXT_LENGTH),
                conversationContext = conversationContext.takeLast(3)
                    .map { it.take(MAX_CONTEXT_ITEM_LENGTH) }
            )
        ).toRequestBody(JSON_MEDIA_TYPE)

        try {
            val request = Request.Builder()
                .url("$gatewayBaseUrl/v1/vision/analyze")
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw mapHttpError(response.code)
                }

                val result = gson.fromJson(body, AnalyzeResponse::class.java)
                    ?: throw VisionError.ApiError("自建视觉服务响应为空")
                val rawAnalysis = result.analysis?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: throw VisionError.ApiError("自建视觉服务未返回分析结果")

                return@withContext VisionResponseParser.parse(rawAnalysis, 0.78f)
            }
        } catch (error: VisionError) {
            throw error
        } catch (error: java.net.SocketTimeoutException) {
            throw VisionError.Timeout("自建视觉服务处理超时，请稍后重试")
        } catch (error: Exception) {
            Logger.e(TAG, "自建视觉服务分析失败", error)
            throw VisionError.NetworkError("自建视觉服务暂时不可用")
        }
    }

    private fun mapHttpError(code: Int): VisionError = when (code) {
        401, 403 -> VisionError.ApiError("自建视觉服务授权已失效，请重新配对", code.toString())
        413 -> VisionError.ImageTooLarge(maxImageSize + 1, maxImageSize)
        429 -> VisionError.ApiError("自建视觉服务当前繁忙，请稍后重试", code.toString())
        504 -> VisionError.Timeout("自建视觉服务处理超时，请稍后重试")
        else -> VisionError.ApiError("自建视觉服务请求失败（$code）", code.toString())
    }

    private fun pairErrorMessage(code: Int): String = when (code) {
        400, 401, 403 -> "配对码无效、已使用或已过期"
        429 -> "配对请求过于频繁，请稍后重试"
        else -> "自建视觉服务配对失败（$code）"
    }

    private fun readImageBytes(imageUri: Uri): ByteArray {
        val stream = context.contentResolver.openInputStream(imageUri)
            ?: throw VisionError.Unknown("无法读取图片文件")
        return stream.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count <= 0) break
                output.write(buffer, 0, count)
                if (output.size().toLong() > maxImageSize) {
                    throw VisionError.ImageTooLarge(output.size().toLong(), maxImageSize)
                }
            }
            output.toByteArray()
        }
    }

    private fun detectMimeType(imageBytes: ByteArray): String = when {
        imageBytes.size >= 3 &&
            imageBytes[0].unsignedByte() == 0xFF &&
            imageBytes[1].unsignedByte() == 0xD8 &&
            imageBytes[2].unsignedByte() == 0xFF -> "image/jpeg"
        imageBytes.size >= 8 &&
            imageBytes.copyOfRange(0, 8).contentEquals(PNG_SIGNATURE) -> "image/png"
        imageBytes.size >= 12 &&
            imageBytes.copyOfRange(0, 4).contentEquals(RIFF_SIGNATURE) &&
            imageBytes.copyOfRange(8, 12).contentEquals(WEBP_SIGNATURE) -> "image/webp"
        else -> throw VisionError.UnsupportedFormat("仅支持 JPEG、PNG 或 WebP 图片")
    }

    private fun Byte.unsignedByte(): Int = toInt() and 0xFF

    private data class PairRequest(
        @SerializedName("pairing_code") val pairingCode: String,
        @SerializedName("installation_id") val installationId: String
    )

    private data class PairResponse(
        @SerializedName("device_token") val deviceToken: String?
    )

    private data class AnalyzeRequest(
        @SerializedName("image_base64") val imageBase64: String,
        @SerializedName("mime_type") val mimeType: String,
        @SerializedName("user_text") val userText: String,
        @SerializedName("conversation_context") val conversationContext: List<String>
    )

    private data class AnalyzeResponse(
        val analysis: String?
    )

    private companion object {
        const val TAG = "SelfHostedVisionProvider"
        const val MAX_USER_TEXT_LENGTH = 2000
        const val MAX_CONTEXT_ITEM_LENGTH = 1000
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        val RIFF_SIGNATURE = byteArrayOf(0x52, 0x49, 0x46, 0x46)
        val WEBP_SIGNATURE = byteArrayOf(0x57, 0x45, 0x42, 0x50)
    }
}
