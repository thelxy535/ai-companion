package com.companion.cc.domain.manager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
import com.companion.cc.domain.model.VisionAnalysis
import com.companion.cc.domain.capability.Capability
import com.companion.cc.domain.capability.CapabilityGate
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.data.local.SettingsManager
import com.companion.cc.domain.model.VisionError
import com.companion.cc.domain.vision.VisionProvider
import com.companion.cc.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 视觉管理器（视觉感知层）
 *
 * 职责：
 * - 图片预处理（压缩、格式转换）
 * - 结果缓存（同图片 + 同问题 → 避免重复调用 API）
 * - 错误处理和降级
 * - 协调具体的 VisionProvider
 */
@Singleton
class VisionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val visionProvider: VisionProvider,
    private val settingsManager: SettingsManager,
    private val currentUserProvider: CurrentUserProvider
) {
    // 视觉理解结果缓存
    // Key: imageHash + userText 的 hash
    // Value: VisionAnalysis
    private val analysisCache = ConcurrentHashMap<String, CacheEntry>()

    data class CacheEntry(
        val analysis: VisionAnalysis,
        val timestamp: Long
    )

    companion object {
        private const val CACHE_TTL_MS = 30 * 60 * 1000L  // 30分钟缓存
        private const val MAX_IMAGE_DIMENSION = 1920      // 压缩后最大边长
        private const val COMPRESSION_QUALITY = 85        // JPEG 压缩质量
    }

    /**
     * 分析图片（带缓存）
     *
     * @param imageUri 原始图片 URI
     * @param userText 用户附带的文字
     * @param conversationContext 最近对话上下文
     * @return 视觉理解结果
     */
    suspend fun analyzeImage(
        imageUri: Uri,
        userText: String,
        conversationContext: List<String> = emptyList(),
        characterId: String? = null
    ): VisionAnalysis = withContext(Dispatchers.IO) {
        Logger.d("VisionManager", "========== 开始图片分析 ==========")

        // Capability is checked before provider availability or image processing.
        val userId = currentUserProvider.requireUserId()
        val scopedCharacterId = requireNotNull(characterId) { "视觉请求缺少角色 scope" }
        val flags = settingsManager.capabilityFlagsFlow(userId, scopedCharacterId).first()
        try {
            CapabilityGate.requireAllowed(Capability.VISION, flags)
        } catch (e: UnsupportedOperationException) {
            throw VisionError.ApiError(e.message ?: "当前角色未启用视觉能力")
        }

        // 1. 检查 Provider 是否可用
        val isAvailable = visionProvider.isAvailable()
        Logger.d("VisionManager", "Provider 可用性: $isAvailable")

        if (!isAvailable) {
            Logger.e("VisionManager", "视觉模型不可用")
            throw VisionError.ApiError("所选图片理解服务未配置或不可用，请在设置中完成配置")
        }

        // 2. 计算缓存 Key
        val cacheKey = computeCacheKey(
            imageUri = imageUri,
            userText = userText,
            providerIdentity = visionProvider.cacheIdentity()
        )

        // 3. 检查缓存
        analysisCache[cacheKey]?.let { cached ->
            val age = System.currentTimeMillis() - cached.timestamp
            if (age < CACHE_TTL_MS) {
                Logger.d("VisionManager", "使用缓存的视觉理解结果 (${age / 1000}秒前)")
                return@withContext cached.analysis
            } else {
                // 缓存过期，移除
                analysisCache.remove(cacheKey)
            }
        }

        // 4. 预处理图片（压缩）
        val processedUri = preprocessImage(imageUri)

        // 5. 调用 Provider 进行分析
        try {
            val analysis = visionProvider.analyzeImage(
                imageUri = processedUri,
                userText = userText,
                conversationContext = conversationContext
            )

            // 6. 缓存结果
            analysisCache[cacheKey] = CacheEntry(
                analysis = analysis,
                timestamp = System.currentTimeMillis()
            )

            Logger.d("VisionManager", "视觉理解完成")
            return@withContext analysis

        } catch (e: VisionError) {
            Logger.e("VisionManager", "视觉理解失败", e)
            throw e
        } catch (e: Exception) {
            Logger.e("VisionManager", "视觉理解未知错误", e)
            throw VisionError.Unknown("图片理解失败: ${e.message}", e)
        }
    }

    /**
     * 预处理图片（压缩、格式转换）
     *
     * @param originalUri 原始图片 URI
     * @return 处理后的 URI（可能是压缩后的临时文件）
     */
    private suspend fun preprocessImage(originalUri: Uri): Uri = withContext(Dispatchers.IO) {
        try {
            // 1. 解码原始图片
            val inputStream = context.contentResolver.openInputStream(originalUri)
                ?: throw VisionError.Unknown("无法打开图片文件")

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight

            // 2. 计算缩放比例
            val scale = calculateScale(originalWidth, originalHeight, MAX_IMAGE_DIMENSION)

            // 如果不需要缩放，直接返回原 URI
            if (scale == 1) {
                Logger.d("VisionManager", "图片无需压缩: ${originalWidth}x${originalHeight}")
                return@withContext originalUri
            }

            // 3. 重新解码并缩放
            val scaledBitmap = context.contentResolver.openInputStream(originalUri)?.use { stream ->
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = scale
                }
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: throw VisionError.Unknown("图片解码失败")

            // 4. 保存到临时文件
            val tempFile = File(context.cacheDir, "vision_temp_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, out)
            }
            scaledBitmap.recycle()

            val newWidth = scaledBitmap.width
            val newHeight = scaledBitmap.height
            Logger.d("VisionManager", "图片已压缩: ${originalWidth}x${originalHeight} → ${newWidth}x${newHeight}")

            return@withContext tempFile.toUri()

        } catch (e: VisionError) {
            throw e
        } catch (e: Exception) {
            Logger.e("VisionManager", "图片预处理失败", e)
            // 预处理失败时，尝试直接使用原图
            return@withContext originalUri
        }
    }

    /**
     * 计算图片缩放比例
     */
    private fun calculateScale(width: Int, height: Int, maxDimension: Int): Int {
        val maxOriginal = maxOf(width, height)
        if (maxOriginal <= maxDimension) return 1

        var scale = 1
        while (maxOriginal / scale > maxDimension) {
            scale *= 2
        }
        return scale
    }

    /**
     * 计算缓存 Key（图片内容 + 用户文字的 hash）
     */
    private suspend fun computeCacheKey(
        imageUri: Uri,
        userText: String,
        providerIdentity: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val digest = MessageDigest.getInstance("MD5")

            // Hash 图片内容（前 64KB，避免大图片卡顿）
            context.contentResolver.openInputStream(imageUri)?.use { stream ->
                val buffer = ByteArray(64 * 1024)
                val bytesRead = stream.read(buffer)
                if (bytesRead > 0) {
                    digest.update(buffer, 0, bytesRead)
                }
            }

            // Hash 用户文字和所选服务，避免服务切换后复用另一服务的结果。
            digest.update(userText.toByteArray())
            digest.update(0.toByte())
            digest.update(providerIdentity.toByteArray())

            // 转换为 Hex 字符串
            digest.digest().joinToString("") { "%02x".format(it) }

        } catch (e: Exception) {
            Logger.e("VisionManager", "计算缓存 Key 失败", e)
            // 降级：使用 URI + 文字的简单 hash
            "${imageUri.hashCode()}_${userText.hashCode()}_${providerIdentity.hashCode()}"
        }
    }

    /**
     * 清理缓存（手动触发或定期清理）
     */
    fun clearCache() {
        analysisCache.clear()
        Logger.d("VisionManager", "视觉理解缓存已清空")
    }

    /**
     * 清理临时文件
     */
    fun cleanTempFiles() {
        context.cacheDir.listFiles { file ->
            file.name.startsWith("vision_temp_")
        }?.forEach { file ->
            if (file.delete()) {
                Logger.d("VisionManager", "删除临时文件: ${file.name}")
            }
        }
    }
}
