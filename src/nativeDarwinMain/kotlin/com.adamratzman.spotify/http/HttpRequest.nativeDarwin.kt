package com.adamratzman.spotify.http

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.darwin.Darwin

internal actual val customHttpClient: Any?
    get() = HttpClient(
        engine = Darwin.create(),
        userConfig = HttpClientConfig<HttpClientEngineConfig>().apply {
            expectSuccess = false
        }
    )