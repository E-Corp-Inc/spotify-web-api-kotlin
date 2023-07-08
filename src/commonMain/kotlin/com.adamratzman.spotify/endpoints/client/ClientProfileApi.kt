/* Spotify Web API, Kotlin Wrapper; MIT License, 2017-2022; Original author: Adam Ratzman */
package com.adamratzman.spotify.endpoints.client

import com.adamratzman.spotify.GenericSpotifyApi
import com.adamratzman.spotify.SpotifyScope
import com.adamratzman.spotify.endpoints.pub.UserApi
import com.adamratzman.spotify.models.SpotifyUserInformation
import com.adamratzman.spotify.models.serialization.toObject

/**
 * Endpoints for retrieving information about a user’s profile.
 *
 * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/users-profile/)**
 */
public class ClientProfileApi(api: GenericSpotifyApi) : UserApi(api) {
    /**
     * Get detailed profile information about the current user (including the current user’s username).
     *
     * The access token must have been issued on behalf of the current user.
     * Reading the user’s email address requires the [SpotifyScope.UserReadEmail] scope;
     * reading country and product subscription level requires the [SpotifyScope.UserReadPrivate] scope.
     *
     * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/users-profile/get-current-users-profile/)**
     *
     * @return Never-null [SpotifyUserInformation] object with possibly-null country, email, subscription and birthday fields
     */
    public suspend fun getClientProfile(): SpotifyUserInformation =
        get(endpointBuilder("/me").toString()).toObject(SpotifyUserInformation.serializer(), api, json)
}
