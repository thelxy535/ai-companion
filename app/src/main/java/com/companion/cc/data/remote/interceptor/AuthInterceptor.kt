package com.companion.cc.data.remote.interceptor

import com.companion.cc.data.local.SettingsManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val settingsManager: SettingsManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val apiKey = runBlocking {
            settingsManager.apiKeyFlow.first()
        }

        val request = if (apiKey != null) {
            // 清理 API Key 中的空白字符（包括换行符、回车符等）
            val cleanApiKey = apiKey.trim().replace(Regex("\\s+"), "")

            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $cleanApiKey")
                .build()
        } else {
            chain.request()
        }

        return chain.proceed(request)
    }
}
