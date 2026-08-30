package com.companion.cc.data.local.repository

import com.companion.cc.domain.memory.MemoryCapsuleV2Codec

class MemoryCapsuleV2TransferService(
    private val builder: MemoryCapsuleV2Builder,
    private val importer: MemoryCapsuleV2Importer,
    private val codec: MemoryCapsuleV2Codec
) : MemoryCapsuleV2Transfer {
    override suspend fun exportCapsuleJson(scopeKey: String): Result<String> = runCatching {
        val capsule = builder.build(scopeKey).getOrThrow()
        codec.encode(capsule)
    }

    override suspend fun importCapsuleJson(
        json: String,
        targetScopeKey: String
    ): Result<MemoryCapsuleV2Importer.Report> {
        if (json.toByteArray(Charsets.UTF_8).size > MAX_IMPORT_BYTES) {
            return Result.failure(IllegalArgumentException("Memory capsule exceeds 10 MB limit"))
        }
        return codec.decode(json).fold(
            onSuccess = { capsule -> importer.importCapsule(capsule, targetScopeKey) },
            onFailure = { error -> Result.failure(error) }
        )
    }

    private companion object {
        const val MAX_IMPORT_BYTES = 10 * 1024 * 1024
    }
}
