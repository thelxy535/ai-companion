package com.companion.cc.domain.character

/** Prevents a character from narrating the same physical beat over and over. */
object ActionContinuityPolicy {
    fun accept(candidate: String?, previous: String?): String? {
        val next = candidate?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
        if (next.isBlank()) return null
        val old = previous?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
        if (old.isBlank()) return next
        if (next == old) return null
        val nextCore = next.trim('。', '，', ',', '.', '！', '!', '？', '?', '。')
        val oldCore = old.trim('。', '，', ',', '.', '！', '!', '？', '?', '。')
        if (nextCore.length >= 4 && oldCore.length >= 4 &&
            (nextCore.contains(oldCore) || oldCore.contains(nextCore))) return null
        return next
    }
}
