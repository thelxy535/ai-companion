package com.companion.cc.data.remote.api

import com.companion.cc.data.remote.model.ChatRequest
import com.companion.cc.domain.model.StreamHttpException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton
import com.companion.cc.domain.manager.normalizeApiBaseUrl

/**
 * 流式聊天服务
 * 使用SSE (Server-Sent Events) 实现流式响应
 */
@Singleton
class StreamChatService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    @Volatile
    private var activeCall: okhttp3.Call? = null

    fun cancelActiveCall() {
        activeCall?.cancel()
    }

    /**
     * 流式聊天
     * @return Flow<String> 每次emit一小段文本
     */
    fun streamChat(
        baseUrl: String,
        request: ChatRequest
    ): Flow<String> = flow {
        // 规范化baseUrl（去掉尾部斜杠）
        val normalizedBaseUrl = normalizeApiBaseUrl(baseUrl)

        android.util.Log.d("StreamChatService", "=== 开始流式请求 ===")
        android.util.Log.d("StreamChatService", "URL: $normalizedBaseUrl/chat/completions")
        android.util.Log.d("StreamChatService", "模型: ${request.model}")

        // 1. 构建请求（开启stream）
        val streamRequest = request.copy(stream = true)

        // 使用Gson序列化（与现有代码一致）
        val gson = com.google.gson.Gson()
        val jsonString = gson.toJson(streamRequest)
        val requestBody = jsonString.toRequestBody("application/json".toMediaType())

        android.util.Log.d("StreamChatService", "请求体长度: ${jsonString.length} 字符")

        val httpRequest = Request.Builder()
            .url("$normalizedBaseUrl/chat/completions")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        // 注意：Authorization header由AuthInterceptor自动添加，不要手动添加以避免重复

        // 2. 执行请求
        val call = okHttpClient.newCall(httpRequest)
        activeCall = call
        val cancellationHandle = currentCoroutineContext()[Job]?.invokeOnCompletion {
            call.cancel()
        }
        try {
            val response = call.execute()
            response.use {
        android.util.Log.d("StreamChatService", "响应状态码: ${response.code}")

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "未知错误"
            android.util.Log.e("StreamChatService", "API调用失败: ${response.code} - $errorBody")
            throw StreamHttpException(
                code = response.code,
                message = "API调用失败: ${response.code} - $errorBody",
                retryAfterSeconds = response.header("Retry-After")?.toLongOrNull()
            )
        }

        // 3. 逐行读取SSE流
        android.util.Log.d("StreamChatService", "开始读取SSE流...")
        var lineCount = 0
        var chunkCount = 0
        var done = false

        response.body?.source()?.use { source ->
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                lineCount++

                // SSE格式：data: {...}
                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()

                    // [DONE] 表示结束
                    if (data == "[DONE]") {
                        android.util.Log.d("StreamChatService", "收到[DONE]标记，流结束")
                        done = true
                        break
                    }

                    val parsed = StreamSseParser.parse(listOf(line))
                    parsed.errorMessage?.let { errorMessage ->
                        throw IllegalStateException("服务器流错误: $errorMessage")
                    }
                    if (parsed.malformedChunks > 0) {
                        throw IllegalStateException("解析流响应失败 (行$lineCount)")
                    }
                    parsed.fragments.forEach { content ->
                        chunkCount++
                        emit(content)
                    }
                }
            }
        }

        require(done) { "流式响应缺少[DONE]结束标记" }
        require(chunkCount > 0) { "流式响应没有有效内容" }
        android.util.Log.d("StreamChatService", "流式响应完成，总行数: $lineCount, 有效块数: $chunkCount")
        }
        } finally {
            cancellationHandle?.dispose()
            if (activeCall === call) activeCall = null
        }
    }.flowOn(Dispatchers.IO)  // 在IO线程执行网络请求
}
