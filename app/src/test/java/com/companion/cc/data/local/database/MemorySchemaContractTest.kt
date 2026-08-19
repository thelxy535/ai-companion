package com.companion.cc.data.local.database

import org.junit.Assert.assertEquals
import org.junit.Test

class MemorySchemaContractTest {

    @Test
    fun currentDatabaseVersionAndLegacyTablesAreStable() {
        assertEquals(10, APP_MIGRATION_9_10.endVersion)
        assertEquals(11, APP_MIGRATION_10_11.endVersion)
        assertEquals(12, APP_MIGRATION_11_12.endVersion)
    }
}
