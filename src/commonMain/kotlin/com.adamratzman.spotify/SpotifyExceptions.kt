/* Spotify Web API - Kotlin Wrapper; MIT License, 2019; Original author: Adam Ratzman */
package com.adamratzman.spotify

import com.adamratzman.spotify.models.AuthenticationError
import com.adamratzman.spotify.models.ErrorObject
import io.ktor.client.features.ResponseException

sealed class SpotifyException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    abstract class UnNullableException(message: String) : SpotifyException(message)

    /**
     * Thrown when a request fails
     */
    open class BadRequestException(message: String, val statusCode: Int? = null, cause: Throwable? = null) :
        SpotifyException(message, cause) {
        constructor(message: String, cause: Throwable? = null) : this(message, null, cause)
        constructor(error: ErrorObject, cause: Throwable? = null) : this(
            "Received Status Code ${error.status}. Error cause: ${error.message}",
            error.status,
            cause
        )

        constructor(authenticationError: AuthenticationError) :
                this(
                    "Authentication error: ${authenticationError.error}. Description: ${authenticationError.description}",
                    401
                )
        constructor(responseException: ResponseException) :
                this(
                    responseException.message ?: "Bad Request",
                    responseException.response.status.value,
                    responseException
                )
    }

    class SpotifyParseException(message: String, cause: Throwable? = null) : SpotifyException(message, cause)
}
