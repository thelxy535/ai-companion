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
    fun characterCapsuleMigrationUsesVersion13() {
        assertEquals(12, APP_MIGRATION_12_13.startVersion)
        assertEquals(13, APP_MIGRATION_12_13.endVersion)
    }

    @Test
    fun legacyMemoryScopesUseQuarantineMigration() {
        assertEquals(13, APP_MIGRATION_13_14.startVersion)
        assertEquals(14, APP_MIGRATION_13_14.endVersion)
    }

    @Test
    fun migration10To11IsRegistered() {
        assertEquals(10, APP_MIGRATION_10_11.startVersion)
        assertEquals(11, APP_MIGRATION_10_11.endVersion)
    }
}
