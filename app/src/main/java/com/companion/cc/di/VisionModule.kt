package com.companion.cc.di

import com.companion.cc.data.remote.vision.GeminiVisionProvider
import com.companion.cc.data.remote.vision.OpenAIVisionProvider
import com.companion.cc.domain.vision.VisionProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 视觉模块 - 依赖注入配置
 *
 * 切换不同的视觉 Provider：
 * - 修改 @Binds 注解绑定的实现类即可
 * - GeminiVisionProvider: Google Gemini 1.5 Flash（免费额度 1500次/天）
 * - OpenAIVisionProvider: OpenAI GPT-4 Vision（付费）
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class VisionModule {

    @Binds
    @Singleton
    abstract fun bindVisionProvider(
        geminiVisionProvider: GeminiVisionProvider  // 使用免费的 Gemini
    ): VisionProvider

    // 如果想切换回 OpenAI，改成：
    // abstract fun bindVisionProvider(
    //     openAIVisionProvider: OpenAIVisionProvider
    // ): VisionProvider
}
