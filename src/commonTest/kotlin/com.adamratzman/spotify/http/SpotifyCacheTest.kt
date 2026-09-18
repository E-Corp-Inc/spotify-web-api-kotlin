/* Spotify Web API, Kotlin Wrapper; MIT License, 2017-2023; Original author: Adam Ratzman */
package com.adamratzman.spotify.http

import com.adamratzman.spotify.GenericSpotifyApi
import com.adamratzman.spotify.models.Token
import com.adamratzman.spotify.runTestOnDefaultDispatcher
import com.adamratzman.spotify.spotifyAppApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpotifyCacheTest {
    @Test
    fun cacheOperationsAndSnapshotsAreConsistent(): TestResult = runTestOnDefaultDispatcher {
        val api = testApi(cacheLimit = 2)
        val cache = SpotifyCache()
        val first = request(api, 1)
        val second = request(api, 2)
        val third = request(api, 3)

        cache[first] = state(1, expireBy = 0)
        assertNull(cache[first])

        cache[first] = state(1, expireBy = Long.MAX_VALUE - 2)
        cache[second] = state(2, expireBy = Long.MAX_VALUE - 1)
        val snapshot = cache.snapshot()
        @Suppress("DEPRECATION")
        val compatibilitySnapshot = cache.cachedRequests
        cache[third] = state(3)

        assertEquals(setOf(first, second), snapshot.keys)
        assertEquals(setOf(first, second), compatibilitySnapshot.keys)
        assertEquals(setOf(second, third), cache.snapshot().keys)

        cache -= second
        assertEquals(setOf(third), cache.snapshot().keys)

        cache.clear()
        assertTrue(cache.snapshot().isEmpty())
    }

    @Test
    fun concurrentMutationsAndSnapshotsAreSafe(): TestResult = runTestOnDefaultDispatcher {
        val api = testApi(cacheLimit = 64)
        val cache = SpotifyCache()

        withContext(Dispatchers.Default) {
            List(12) { worker ->
                async {
                    repeat(1_000) { iteration ->
                        val request = request(api, (worker * 1_000) + iteration)
                        cache[request] = state(iteration)
                        cache[request]
                        if (iteration % 3 == 0) cache -= request
                        if (iteration % 17 == 0) cache.snapshot()
                        if (iteration % 251 == 0) cache.clear()
                    }
                }
            }.awaitAll()
        }

        assertTrue(cache.snapshot().size <= 64)
    }

    private suspend fun testApi(cacheLimit: Int): GenericSpotifyApi = spotifyAppApi(
        clientId = null,
        clientSecret = null,
        token = Token("test", "Bearer", Int.MAX_VALUE)
    ) {
        this.cacheLimit = cacheLimit
    }.build()

    private fun request(api: GenericSpotifyApi, id: Int): SpotifyRequest =
        SpotifyRequest("https://example.test/$id", HttpRequestMethod.GET, null, api)

    private fun state(id: Int, expireBy: Long = Long.MAX_VALUE): CacheState =
        CacheState("response-$id", null, expireBy)
}
