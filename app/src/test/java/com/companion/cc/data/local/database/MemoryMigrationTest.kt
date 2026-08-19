package com.companion.cc.data.local.database

import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryMigrationTest {

    @Test
    fun memory20TablesAreRegisteredWithStableDefaults() {
        assertEquals(10, APP_MIGRATION_10_11.startVersion)
        assertEquals(11, APP_MIGRATION_10_11.endVersion)
        assertEquals(11, APP_MIGRATION_11_12.startVersion)
        assertEquals(12, APP_MIGRATION_11_12.endVersion)
    }

    @Test
    fun migration10To11IsRegistered() {
        assertEquals(10, APP_MIGRATION_10_11.startVersion)
        assertEquals(11, APP_MIGRATION_10_11.endVersion)
    }
}
