/* Spotify Web API, Kotlin Wrapper; MIT License, 2017-2023; Original author: Adam Ratzman */
package com.adamratzman.spotify.http

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal actual class SpotifyCacheLock {
    private val lock = ReentrantLock()

    actual fun <T> withLock(block: () -> T): T = lock.withLock(block)
}
