package com.companion.cc.ui.memory

internal data class MemoryGraphGridPosition(
    val column: Int,
    val row: Int,
)

/** Stable, non-overlapping placement for the memory graph's composable nodes and edges. */
internal object MemoryGraphLayout {
    private const val minimumCardWidthDp = 132f
    private const val horizontalGapDp = 12f
    private const val maxColumns = 3

    fun columnsFor(nodeCount: Int, availableWidthDp: Float): Int {
        require(nodeCount > 0) { "nodeCount must be positive" }
        val fittingColumns = ((availableWidthDp + horizontalGapDp) /
            (minimumCardWidthDp + horizontalGapDp)).toInt().coerceAtLeast(1)
        return minOf(nodeCount, fittingColumns, maxColumns)
    }

    fun rowCount(nodeCount: Int, columns: Int): Int {
        require(nodeCount > 0) { "nodeCount must be positive" }
        require(columns > 0) { "columns must be positive" }
        return (nodeCount + columns - 1) / columns
    }

    fun positionFor(index: Int, nodeCount: Int, columns: Int): MemoryGraphGridPosition {
        require(index in 0 until nodeCount) { "index must identify a graph node" }
        require(columns in 1..nodeCount) { "columns must be within the node count" }
        return MemoryGraphGridPosition(column = index % columns, row = index / columns)
    }
}
