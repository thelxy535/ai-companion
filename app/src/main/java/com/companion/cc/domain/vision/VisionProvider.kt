package com.companion.cc.domain.vision

import android.net.Uri
import com.companion.cc.domain.model.VisionAnalysis
import com.companion.cc.domain.model.VisionError

/**
 * 视觉 Provider 抽象接口
 *
 * 设计目标：
 * - 可插拔：通过依赖注入切换不同云端视觉 API
 * - 统一接口：隔离具体 API 实现细节
 * - 易扩展：新增视觉模型只需实现此接口
 */
interface VisionProvider {
    /**
     * 分析图片内容
     *
     * @param imageUri 图片 URI (file:// 或 content://)
     * @param userText 用户附带的文字（用于语境理解）
     * @param conversationContext 最近几轮对话（可选，帮助理解用户意图）
     * @return 结构化视觉理解结果
     * @throws VisionError 分析失败时抛出具体错误类型
     */
    suspend fun analyzeImage(
        imageUri: Uri,
        userText: String,
        conversationContext: List<String> = emptyList()
    ): VisionAnalysis

    /**
     * 检查 Provider 是否可用
     *
     * @return true = 配置正确且可调用，false = 需要用户配置 API Key 等
     */
    suspend fun isAvailable(): Boolean

    /**
     * Provider 名称（用于日志和配置）
     */
    val providerName: String

    /**
     * 当前结果缓存所属的服务标识。路由 Provider 应覆盖此值，避免切换服务后复用另一个服务的结果。
     */
    suspend fun cacheIdentity(): String = providerName

    /**
     * 支持的最大图片大小（字节）
     */
    val maxImageSize: Long
        get() = 10 * 1024 * 1024  // 默认 10MB

    /**
     * 支持的图片格式
     */
    val supportedFormats: Set<String>
        get() = setOf("image/jpeg", "image/png", "image/webp")
}
