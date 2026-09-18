/* Spotify Web API, Kotlin Wrapper; MIT License, 2017-2023; Original author: Adam Ratzman */
package com.adamratzman.spotify.http

import platform.Foundation.NSLock

internal actual class SpotifyCacheLock {
    private val lock = NSLock()

    actual fun <T> withLock(block: () -> T): T {
        lock.lock()
        return try {
            block()
        } finally {
            lock.unlock()
        }
    }
}
