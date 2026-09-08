package com.companion.cc.domain.manager

/** Normalizes provider roots without changing custom proxy paths. */
internal fun normalizeApiBaseUrl(value: String): String {
    val trimmed = value.trim().trimEnd('/')
    if (trimmed.isBlank()) return trimmed

    return runCatching {
        val uri = java.net.URI(trimmed)
        val host = uri.host?.lowercase()
        val path = uri.path.orEmpty().trimEnd('/')
        if (host in setOf("api.siliconflow.cn", "api.openai.com", "api.deepseek.com") && path.isBlank()) {
            "$trimmed/v1"
        } else {
            trimmed
        }
    }.getOrDefault(trimmed)
}
