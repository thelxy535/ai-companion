package com.companion.cc.data.local.database

import androidx.room.Database
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemorySchemaContractTest {

    @Test
    fun currentDatabaseVersionAndLegacyTablesAreStable() {
        val annotation = AppDatabase::class.java.getAnnotation(Database::class.java)
        requireNotNull(annotation)

        assertEquals(10, annotation.version)

        val tableNames = annotation.entities
            .map { entity ->
                entity.java.getAnnotation(androidx.room.Entity::class.java)?.tableName
                    ?.takeIf { it.isNotBlank() }
                    ?: entity.java.simpleName
            }
            .toSet()

        assertTrue("messages" in tableNames)
        assertTrue("memories" in tableNames)
        assertTrue("vector_memories" in tableNames)
    }
}
