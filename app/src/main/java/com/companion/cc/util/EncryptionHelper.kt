package com.companion.cc.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.companion.cc.util.Logger
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 加密工具类
 *
 * 使用 Android Keystore 加密敏感数据（如 API Key）
 *
 * 功能：
 * 1. 使用 AES-GCM 256 位加密
 * 2. 密钥存储在 Android Keystore（硬件保护）
 * 3. 每次加密使用随机 IV
 */
@Singleton
class EncryptionHelper @Inject constructor() {

    companion object {
        private const val KEYSTORE_ALIAS = "CC_SWITCH_KEY"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    init {
        // 确保密钥存在
        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            generateKey()
        }
    }

    /**
     * 生成加密密钥
     */
    private fun generateKey() {
        try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)  // 确保每次加密使用随机 IV
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()

            Logger.d("EncryptionHelper", "【加密】密钥生成成功")
        } catch (e: Exception) {
            Logger.e("EncryptionHelper", "密钥生成失败", e)
            throw e
        }
    }

    /**
     * 获取密钥
     */
    private fun getSecretKey(): SecretKey {
        return keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
    }

    /**
     * 加密文本
     *
     * @param plainText 明文
     * @return Base64 编码的密文（包含 IV）
     */
    fun encrypt(plainText: String): String? {
        if (plainText.isBlank()) return null

        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())

            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // 组合 IV 和密文: [IV_LENGTH(1 byte)][IV][ENCRYPTED_DATA]
            val combined = ByteArray(1 + iv.size + encryptedBytes.size)
            combined[0] = iv.size.toByte()
            System.arraycopy(iv, 0, combined, 1, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, 1 + iv.size, encryptedBytes.size)

            val result = Base64.encodeToString(combined, Base64.NO_WRAP)
            Logger.d("EncryptionHelper", "【加密】加密成功，长度: ${result.length}")
            result
        } catch (e: Exception) {
            Logger.e("EncryptionHelper", "加密失败", e)
            null
        }
    }

    /**
     * 解密文本
     *
     * @param encryptedText Base64 编码的密文（包含 IV）
     * @return 明文
     */
    fun decrypt(encryptedText: String?): String? {
        if (encryptedText.isNullOrBlank()) return null

        return try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)

            // 提取 IV 和密文
            val ivLength = combined[0].toInt()
            val iv = ByteArray(ivLength)
            System.arraycopy(combined, 1, iv, 0, ivLength)

            val encryptedBytes = ByteArray(combined.size - 1 - ivLength)
            System.arraycopy(combined, 1 + ivLength, encryptedBytes, 0, encryptedBytes.size)

            // 解密
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            val result = String(decryptedBytes, Charsets.UTF_8)

            Logger.d("EncryptionHelper", "【解密】解密成功")
            result
        } catch (e: Exception) {
            Logger.e("EncryptionHelper", "解密失败", e)
            null
        }
    }

    /**
     * 检查加密是否可用
     */
    fun isEncryptionAvailable(): Boolean {
        return try {
            keyStore.containsAlias(KEYSTORE_ALIAS) || run {
                generateKey()
                true
            }
        } catch (e: Exception) {
            Logger.e("EncryptionHelper", "加密不可用", e)
            false
        }
    }
}
