package com.companion.cc.data.local.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryHistoryAndConflictTest {
    @Test
    fun conflictStatesAreExplicitlyRecognized() {
        assertEquals(true, MemoryRepository.isConflictStatus("conflict"))
        assertEquals(false, MemoryRepository.isConflictStatus("active"))
    }
}
