package com.companion.cc.domain.vision

import com.companion.cc.data.local.SettingsManager
import com.companion.cc.data.remote.vision.GeminiVisionProvider
import com.companion.cc.data.remote.vision.SelfHostedVisionProvider
import com.companion.cc.domain.model.VisionAnalysis
import com.companion.cc.domain.model.VisionServiceMode
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** 根据用户明确选择的服务路由图片理解请求，不做隐式云端降级。 */
@Singleton
class ConfigurableVisionProvider @Inject constructor(
    private val settingsManager: SettingsManager,
    private val geminiProvider: GeminiVisionProvider,
    private val selfHostedProvider: SelfHostedVisionProvider
) : VisionProvider {
    override val providerName: String = "用户选择的图片理解服务"

    override val maxImageSize: Long = 20 * 1024 * 1024

    override suspend fun isAvailable(): Boolean = selectedProvider().isAvailable()

    override suspend fun cacheIdentity(): String = selectedProvider().providerName

    override suspend fun analyzeImage(
        imageUri: android.net.Uri,
        userText: String,
        conversationContext: List<String>
    ): VisionAnalysis = selectedProvider().analyzeImage(imageUri, userText, conversationContext)

    private suspend fun selectedProvider(): VisionProvider = when (
        VisionServiceMode.fromStoredValue(settingsManager.visionServiceModeFlow.first())
    ) {
        VisionServiceMode.GEMINI -> geminiProvider
        VisionServiceMode.SELF_HOSTED -> selfHostedProvider
    }
}
