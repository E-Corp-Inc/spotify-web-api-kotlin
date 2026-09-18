/* Spotify Web API, Kotlin Wrapper; MIT License, 2017-2023; Original author: Adam Ratzman */
package com.adamratzman.spotify.http

internal actual class SpotifyCacheLock {
    actual fun <T> withLock(block: () -> T): T = block()
}
