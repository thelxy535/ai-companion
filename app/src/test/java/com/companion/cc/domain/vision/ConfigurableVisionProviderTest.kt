package com.companion.cc.domain.vision

import android.net.Uri
import com.companion.cc.data.local.SettingsManager
import com.companion.cc.data.remote.vision.GeminiVisionProvider
import com.companion.cc.data.remote.vision.SelfHostedVisionProvider
import com.companion.cc.domain.model.VisionAnalysis
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class ConfigurableVisionProviderTest {
    @Test
    fun `self hosted mode delegates only to self hosted provider`() = runTest {
        val settingsManager = mock<SettingsManager>()
        val geminiProvider = mock<GeminiVisionProvider>()
        val selfHostedProvider = mock<SelfHostedVisionProvider>()
        val imageUri = mock<Uri>()
        val expected = VisionAnalysis(environment = "测试场景")
        val context = listOf("上一条对话")

        whenever(settingsManager.visionServiceModeFlow).thenReturn(flowOf("SELF_HOSTED"))
        whenever(selfHostedProvider.analyzeImage(imageUri, "看看这张图", context))
            .thenReturn(expected)

        val provider = ConfigurableVisionProvider(
            settingsManager = settingsManager,
            geminiProvider = geminiProvider,
            selfHostedProvider = selfHostedProvider
        )

        val actual = provider.analyzeImage(imageUri, "看看这张图", context)

        assertEquals(expected, actual)
        verify(selfHostedProvider).analyzeImage(imageUri, "看看这张图", context)
        verifyNoInteractions(geminiProvider)
    }
}
