package com.companion.cc.ui.memory

import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryLibraryFilterPolicyTest {
    @Test
    fun selectedFilterUsesStorageStatusValue() {
        assertEquals("active", memoryFilterValue("active"))
    }
}
