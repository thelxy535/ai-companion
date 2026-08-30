package com.companion.cc.domain.character

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** Opaque, single-use candidate token handling for memory capsules. */
object CharacterMemoryCapsuleToken {
    private const val TOKEN_BYTES = 32
    private val random = SecureRandom()

    fun generate(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun hash(token: String): String {
        require(token.isNotBlank()) { "Capsule token must not be blank" }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    fun matches(token: String, expectedHash: String): Boolean {
        if (token.isBlank() || expectedHash.isBlank()) return false
        return MessageDigest.isEqual(
            hash(token).toByteArray(Charsets.US_ASCII),
            expectedHash.lowercase().toByteArray(Charsets.US_ASCII)
        )
    }
}
