package com.companion.cc.data.local.database

import androidx.room.Database
import androidx.room.Entity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryMigrationTest {

    @Test
    fun memory20TablesAreRegisteredWithStableDefaults() {
        val database = AppDatabase::class.java.getAnnotation(Database::class.java)
        requireNotNull(database)

        assertEquals(11, database.version)
        val tables = database.entities.mapNotNull {
            it.java.getAnnotation(Entity::class.java)?.tableName
        }.toSet()
        assertTrue("memory_sources" in tables)
        assertTrue("memory_reviews" in tables)
        assertTrue("memory_nodes" in tables)
        assertTrue("memory_evidence" in tables)
        assertTrue("memory_versions" in tables)
    }

    @Test
    fun migration10To11IsRegistered() {
        assertEquals(10, APP_MIGRATION_10_11.startVersion)
        assertEquals(11, APP_MIGRATION_10_11.endVersion)
    }
}
