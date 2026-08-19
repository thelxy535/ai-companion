package com.companion.cc.ui.memory

import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryLibraryViewModelTest {
    @Test
    fun emptyFilterUsesAllMemoryKinds() {
        assertEquals("", MemoryLibraryViewModel.normalizeKind("全部"))
    }
}
