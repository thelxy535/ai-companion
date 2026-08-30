package com.companion.cc.data.local.repository

import com.companion.cc.data.local.repository.MemoryCapsuleV2Importer.Report

interface MemoryCapsuleV2Transfer {
    suspend fun exportCapsuleJson(scopeKey: String): Result<String>

    suspend fun importCapsuleJson(
        json: String,
        targetScopeKey: String
    ): Result<Report>
}
