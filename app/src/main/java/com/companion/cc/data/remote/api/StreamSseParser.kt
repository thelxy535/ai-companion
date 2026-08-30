package com.companion.cc.data.remote.api

import com.google.gson.Gson
import com.google.gson.JsonObject

data class SseParseResult(
    val fragments: List<String>,
    val completed: Boolean,
    val malformedChunks: Int,
    val errorMessage: String? = null
)

object StreamSseParser {
    fun parse(lines: Iterable<String>): SseParseResult {
        val fragments = mutableListOf<String>()
        var completed = false
        var malformed = 0
        var errorMessage: String? = null
        lines.forEach { line ->
            if (!line.startsWith("data:")) return@forEach
            val data = line.removePrefix("data:").trim()
            if (data == "[DONE]") {
                completed = true
                return@forEach
            }
            runCatching {
                Gson().fromJson(data, JsonObject::class.java)
                    ?.let { json ->
                        json.getAsJsonObject("error")
                            ?.get("message")
                            ?.takeIf { !it.isJsonNull }
                            ?.asString
                            ?.let { errorMessage = it }

                    val content = json.getAsJsonArray("choices")
                        ?.firstOrNull()
                        ?.asJsonObject
                        ?.getAsJsonObject("delta")
                        ?.get("content")
                        ?.takeIf { !it.isJsonNull }
                        ?.asString

                    if (content != null) {
                        fragments += content
                    } else if (errorMessage == null) {
                        malformed++
                    }
                    }
            }.onFailure { malformed++ }
        }
        return SseParseResult(fragments, completed, malformed, errorMessage)
    }
}
