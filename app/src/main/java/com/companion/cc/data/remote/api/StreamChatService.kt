package com.companion.cc.data.remote.api

import com.companion.cc.data.remote.model.ChatRequest
import com.companion.cc.data.remote.model.ChatStreamChunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 流式聊天服务
 * 使用SSE (Server-Sent Events) 实现流式响应
 */
@Singleton
class StreamChatService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    private val gson = com.google.gson.Gson()

    /**
     * 流式聊天
     * @return Flow<String> 每次emit一小段文本
     */
    fun streamChat(
        baseUrl: String,
        apiKey: String,
        request: ChatRequest
    ): Flow<String> = flow {
        // 规范化baseUrl（去掉尾部斜杠）
        val normalizedBaseUrl = baseUrl.trimEnd('/')

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
        val response = okHttpClient.newCall(httpRequest).execute()
        android.util.Log.d("StreamChatService", "响应状态码: ${response.code}")

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "未知错误"
            android.util.Log.e("StreamChatService", "API调用失败: ${response.code} - $errorBody")
            throw Exception("API调用失败: ${response.code} - $errorBody")
        }

        // 3. 逐行读取SSE流
        android.util.Log.d("StreamChatService", "开始读取SSE流...")
        var lineCount = 0
        var chunkCount = 0

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
                        break
                    }

                    try {
                        // 解析JSON（使用Gson）
                        val chunk = gson.fromJson(data, ChatStreamChunk::class.java)

                        // 提取content并emit（跳过空块，避免污染消息内容）
                        val content = chunk.choices.firstOrNull()?.delta?.content
                        if (!content.isNullOrBlank()) {
                            chunkCount++
                            emit(content)
                        }
                    } catch (e: Exception) {
                        // 记录解析错误
                        android.util.Log.e("StreamChatService", "解析失败 (行$lineCount): $data", e)
                    }
                }
            }
        }

        android.util.Log.d("StreamChatService", "流式响应完成，总行数: $lineCount, 有效块数: $chunkCount")
    }.flowOn(Dispatchers.IO)  // 在IO线程执行网络请求
}
