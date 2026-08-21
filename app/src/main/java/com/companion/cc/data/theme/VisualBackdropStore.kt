package com.companion.cc.data.theme

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Owns private backdrop files created by the visual customization system.
 *
 * Imports are bounded and decoded before publication. All deletion is
 * canonical-path checked so a malformed preference can never remove an
 * arbitrary file outside the owned directory.
 */
@Singleton
class VisualBackdropStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val managedDirectory: File
        get() = File(context.filesDir, DIRECTORY_NAME)

    suspend fun stage(source: Uri): String = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(source)
            ?: throw IllegalArgumentException("无法读取背景图片")
        stream.use(::importImage)
    }

    internal fun importImage(input: InputStream): String {
        ensureManagedDirectory()
        val pending = File.createTempFile(".visual-", ".pending", managedDirectory)
        try {
            pending.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var totalBytes = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count <= 0) break
                    totalBytes += count
                    if (totalBytes > MAX_FILE_BYTES) {
                        throw IllegalArgumentException("背景图片不能超过 12 MiB")
                    }
                    output.write(buffer, 0, count)
                }
                if (totalBytes == 0L) {
                    throw IllegalArgumentException("背景图片为空")
                }
            }

            val extension = extensionFor(pending)
                ?: throw IllegalArgumentException("仅支持 JPEG、PNG 或 WebP 背景图片")
            validateDimensions(pending)

            val published = File(managedDirectory, "${UUID.randomUUID()}.$extension")
            if (!pending.renameTo(published)) {
                throw IllegalStateException("无法保存背景图片")
            }
            return published.toURI().toString()
        } finally {
            if (pending.exists()) {
                pending.delete()
            }
        }
    }

    fun discardManaged(reference: String?) {
        val file = reference.toManagedFileOrNull() ?: return
        runCatching {
            if (file.isFile) {
                file.delete()
            }
        }
    }

    private fun validateDimensions(file: File) {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.path, options)
        val width = options.outWidth
        val height = options.outHeight
        if (width <= 0 || height <= 0) {
            throw IllegalArgumentException("无法解码背景图片")
        }
        if (width > MAX_IMAGE_EDGE || height > MAX_IMAGE_EDGE) {
            throw IllegalArgumentException("背景图片尺寸过大")
        }
        if (width.toLong() * height.toLong() > MAX_IMAGE_PIXELS) {
            throw IllegalArgumentException("背景图片像素过多")
        }
    }

    private fun extensionFor(file: File): String? {
        val header = ByteArray(12)
        val count = file.inputStream().use { it.read(header) }
        return when {
            count >= 3 &&
                header[0].unsigned() == 0xFF &&
                header[1].unsigned() == 0xD8 &&
                header[2].unsigned() == 0xFF -> "jpg"
            count >= PNG_SIGNATURE.size &&
                header.copyOfRange(0, PNG_SIGNATURE.size).contentEquals(PNG_SIGNATURE) -> "png"
            count >= 12 &&
                header.copyOfRange(0, 4).contentEquals(RIFF_SIGNATURE) &&
                header.copyOfRange(8, 12).contentEquals(WEBP_SIGNATURE) -> "webp"
            else -> null
        }
    }

    private fun ensureManagedDirectory() {
        if (!managedDirectory.exists() && !managedDirectory.mkdirs()) {
            throw IllegalStateException("无法创建背景图片目录")
        }
        if (!managedDirectory.isDirectory) {
            throw IllegalStateException("背景图片目录不可用")
        }
    }

    private fun String?.toManagedFileOrNull(): File? {
        val uri = runCatching { Uri.parse(this) }.getOrNull() ?: return null
        if (uri.scheme != "file") return null
        val path = uri.path?.takeIf { it.isNotBlank() } ?: return null

        val root = runCatching { managedDirectory.canonicalFile }.getOrNull() ?: return null
        val candidate = runCatching { File(path).canonicalFile }.getOrNull() ?: return null
        val rootPrefix = root.path + File.separator
        return candidate.takeIf { it.path.startsWith(rootPrefix) }
    }

    private fun Byte.unsigned(): Int = toInt() and 0xFF

    private companion object {
        const val DIRECTORY_NAME = "visual-backdrops"
        const val MAX_FILE_BYTES = 12L * 1024L * 1024L
        const val MAX_IMAGE_EDGE = 8192
        const val MAX_IMAGE_PIXELS = 24_000_000L
        val PNG_SIGNATURE = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        )
        val RIFF_SIGNATURE = byteArrayOf(0x52, 0x49, 0x46, 0x46)
        val WEBP_SIGNATURE = byteArrayOf(0x57, 0x45, 0x42, 0x50)
    }
}
