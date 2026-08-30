package com.companion.cc.data.repository

import com.companion.cc.data.local.entity.CharacterCleanupTaskEntity
import com.companion.cc.domain.character.CharacterExternalCleanup
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Deletes only private app files explicitly captured by the cleanup task. */
@Singleton
class LocalCharacterExternalCleanup @Inject constructor() : CharacterExternalCleanup {
    override suspend fun cleanup(task: CharacterCleanupTaskEntity) {
        val reference = task.avatarReference ?: return
        val file = when {
            reference.startsWith("file://") -> File(java.net.URI(reference))
            else -> return
        }
        if (file.exists() && !file.delete()) {
            throw IllegalStateException("Unable to delete character avatar")
        }
    }
}
