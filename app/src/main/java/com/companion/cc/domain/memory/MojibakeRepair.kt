package com.companion.cc.domain.memory

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.charset.Charset

/** Repairs the legacy UTF-8-as-GBK corruption found in early memory titles. */
object MojibakeRepair {
    private val legacyCharsets = listOf(
        Charset.forName("GB18030"),
        StandardCharsets.ISO_8859_1,
        Charset.forName("windows-1252"),
    )

    fun repair(value: String): String {
        if (value.isBlank()) return value
        if (value.any { it == '\uFFFD' }) return value

        // Try only encodings that can produce a valid UTF-8 byte sequence and
        // accept a candidate when it restores CJK text without erasing useful
        // non-CJK content around it.
        return legacyCharsets.asSequence()
            .mapNotNull { charset -> decodeUtf8(value, charset) }
            .filter { candidate -> candidate != value && candidate.any(::isCjk) }
            .maxByOrNull(::cjkScore)
            ?: value
    }

    private fun decodeUtf8(value: String, source: Charset): String? = runCatching {
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(value.toByteArray(source)))
            .toString()
    }.getOrNull()

    private fun isCjk(char: Char): Boolean = char in '\u3400'..'\u9fff'

    private fun cjkScore(value: String): Int = value.count(::isCjk)
}
