/* Spotify Web API, Kotlin Wrapper; MIT License, 2017-2022; Original author: Adam Ratzman */
package com.adamratzman.spotify.utils

import io.ktor.http.encodeURLQueryComponent
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

internal actual fun String.encodeUrl() = encodeURLQueryComponent()

/**
 * Actual platform that this program is run on.
 */
public actual val currentApiPlatform: Platform = Platform.Native

public actual typealias ConcurrentHashMap<K, V> = HashMap<K, V>

public actual fun <K, V> ConcurrentHashMap<K, V>.asList(): List<Pair<K, V>> = toList()

/**
 * The current time in milliseconds since UNIX epoch.
 */
@OptIn(ExperimentalTime::class)
public actual fun getCurrentTimeMs(): Long = Clock.System.now().toEpochMilliseconds()

/**
 * Format date to ISO 8601 format
 */
@OptIn(ExperimentalTime::class)
internal actual fun formatDate(date: Long): String {
    return Instant.fromEpochMilliseconds(date).toString()
}
