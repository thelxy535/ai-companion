package com.companion.cc.domain.memory

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.JsonParser

class MemoryCapsuleV2Codec(
    private val gson: Gson = Gson()
) {
    fun encode(capsule: MemoryCapsuleV2): String {
        MemoryCapsuleV2Validator.validate(capsule).getOrThrow()
        return gson.toJson(capsule)
    }

    fun decode(json: String): Result<MemoryCapsuleV2> = runCatching {
        val root = JsonParser().parse(json)
        require(root.isJsonObject) {
            "Memory capsule must be a JSON object"
        }
        val objectRoot = root.asJsonObject
        require(objectRoot.has("format")) {
            "Memory capsule format is required"
        }
        require(objectRoot.has("schemaVersion")) {
            "Memory capsule schema version is required"
        }
        require(!objectRoot.has("apiKey") && !objectRoot.has("settings") && !objectRoot.has("deviceToken")) {
            "Memory capsule must not contain credentials or settings"
        }
        val capsule = gson.fromJson(objectRoot, MemoryCapsuleV2::class.java)
            ?: throw JsonParseException("Memory capsule is empty")
        MemoryCapsuleV2Validator.validate(capsule).getOrThrow()
        capsule
    }
}
