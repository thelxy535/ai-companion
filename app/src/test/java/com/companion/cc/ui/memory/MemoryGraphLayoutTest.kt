package com.companion.cc.ui.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryGraphLayoutTest {

    @Test
    fun phoneWidthUsesTwoColumnsForSeveralNodes() {
        assertEquals(2, MemoryGraphLayout.columnsFor(nodeCount = 6, availableWidthDp = 360f))
    }

    @Test
    fun positionsAreDistinctAndStayInsideCalculatedGrid() {
        val columns = MemoryGraphLayout.columnsFor(nodeCount = 7, availableWidthDp = 360f)
        val positions = (0 until 7).map { index ->
            MemoryGraphLayout.positionFor(index = index, nodeCount = 7, columns = columns)
        }

        assertEquals(7, positions.distinct().size)
        assertTrue(positions.all { it.column in 0 until columns })
        assertTrue(positions.all { it.row in 0 until MemoryGraphLayout.rowCount(7, columns) })
    }

    @Test
    fun singleNodeIsPlacedAtTheFirstGridSlot() {
        assertEquals(
            MemoryGraphGridPosition(column = 0, row = 0),
            MemoryGraphLayout.positionFor(index = 0, nodeCount = 1, columns = 1)
        )
    }
}
