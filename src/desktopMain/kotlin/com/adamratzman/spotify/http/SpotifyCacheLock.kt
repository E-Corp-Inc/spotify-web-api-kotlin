/* Spotify Web API, Kotlin Wrapper; MIT License, 2017-2023; Original author: Adam Ratzman */
package com.adamratzman.spotify.http

import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
internal actual class SpotifyCacheLock {
    private val state = AtomicInt(0)

    actual fun <T> withLock(block: () -> T): T {
        while (!state.compareAndSet(0, 1)) {
            // Cache transactions are deliberately short and never include suspending work.
        }
        return try {
            block()
        } finally {
            state.store(0)
        }
    }
}
