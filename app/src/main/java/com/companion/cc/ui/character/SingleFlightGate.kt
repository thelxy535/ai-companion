package com.companion.cc.ui.character

import java.util.concurrent.atomic.AtomicBoolean

/** Prevents duplicate concurrent saves or deletes from rapid repeated activation. */
class SingleFlightGate {
    private val active = AtomicBoolean(false)

    fun tryAcquire(): Boolean = active.compareAndSet(false, true)

    fun release() {
        active.set(false)
    }
}
