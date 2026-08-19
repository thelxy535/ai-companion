package com.companion.cc.ui.memory

import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryReviewViewModelTest {
    @Test
    fun defaultScopeIsCompanionScoped() {
        assertEquals("companion:xiaocan", MemoryReviewViewModel.scopeFor("xiaocan"))
    }
}
