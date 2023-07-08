/* Spotify Web API, Kotlin Wrapper; MIT License, 2017-2022; Original author: Adam Ratzman */
package com.adamratzman.spotify.endpoints.client

import com.adamratzman.spotify.GenericSpotifyApi
import com.adamratzman.spotify.SpotifyException.BadRequestException
import com.adamratzman.spotify.SpotifyScope
import com.adamratzman.spotify.http.SpotifyEndpoint
import com.adamratzman.spotify.models.AlbumUri
import com.adamratzman.spotify.models.EpisodeUri
import com.adamratzman.spotify.models.PagingObject
import com.adamratzman.spotify.models.PlayableUri
import com.adamratzman.spotify.models.SavedAlbum
import com.adamratzman.spotify.models.SavedEpisode
import com.adamratzman.spotify.models.SavedShow
import com.adamratzman.spotify.models.SavedTrack
import com.adamratzman.spotify.models.ShowUri
import com.adamratzman.spotify.models.serialization.toList
import com.adamratzman.spotify.models.serialization.toNonNullablePagingObject
import com.adamratzman.spotify.utils.Market
import com.adamratzman.spotify.utils.encodeUrl
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

/**
 * Endpoints for retrieving information about, and managing, tracks and albums that the current user has saved in their “Your Music” library.
 *
 * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/library/)**
 */
public class ClientLibraryApi(api: GenericSpotifyApi) : SpotifyEndpoint(api) {
    /**
     * Get a list of the songs saved in the current Spotify user’s ‘Your Music’ library.
     *
     * **Requires** the [SpotifyScope.UserLibraryRead] scope
     *
     * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/library/get-users-saved-tracks/)**
     *
     * @param limit The number of objects to return. Default: 50 (or api limit). Minimum: 1. Maximum: 50.
     * @param offset The index of the first item to return. Default: 0. Use with limit to get the next set of items
     * @param market Provide this parameter if you want the list of returned items to be relevant to a particular country.
     * If omitted, the returned items will be relevant to all countries.
     *
     * @return [PagingObject] of [SavedTrack] ordered by position in library
     */
    public suspend fun getSavedTracks(
        limit: Int? = api.spotifyApiOptions.defaultLimit,
        offset: Int? = null,
        market: Market? = null
    ): PagingObject<SavedTrack> {
        requireScopes(SpotifyScope.UserLibraryRead)

        return get(
            endpointBuilder("/me/tracks").with("limit", limit).with("offset", offset).with("market", market?.name)
                .toString()
        ).toNonNullablePagingObject(SavedTrack.serializer(), api = api, json = json)
    }

    /**
     * Get a list of the albums saved in the current Spotify user’s ‘Your Music’ library.
     *
     * **Requires** the [SpotifyScope.UserLibraryRead] scope
     *
     * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/library/get-users-saved-albums/)**
     *
     * @param limit The number of objects to return. Default: 50 (or api limit). Minimum: 1. Maximum: 50.
     * @param offset The index of the first item to return. Default: 0. Use with limit to get the next set of items
     * @param market Provide this parameter if you want the list of returned items to be relevant to a particular country.
     * If omitted, the returned items will be relevant to all countries.
     *
     * @return Paging Object of [SavedAlbum] ordered by position in library
     */
    public suspend fun getSavedAlbums(
        limit: Int? = api.spotifyApiOptions.defaultLimit,
        offset: Int? = null,
        market: Market? = null
    ): PagingObject<SavedAlbum> {
        requireScopes(SpotifyScope.UserLibraryRead)

        return get(
            endpointBuilder("/me/albums").with("limit", limit).with("offset", offset).with("market", market?.name)
                .toString()
        ).toNonNullablePagingObject(SavedAlbum.serializer(), api = api, json = json)
    }

    /**
     * Get a list of shows saved in the current Spotify user’s library.
     * Optional parameters can be used to limit the number of shows returned.
     *
     * **Requires** the [SpotifyScope.UserLibraryRead] scope
     *
     * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/library/get-users-saved-albums/)**
     *
     * @param limit The number of objects to return. Default: 50 (or api limit). Minimum: 1. Maximum: 50.
     * @param offset The index of the first item to return. Default: 0. Use with limit to get the next set of items
     *
     * @return Paging Object of [SavedShow] ordered by position in library
     */
    public suspend fun getSavedShows(
        limit: Int? = api.spotifyApiOptions.defaultLimit,
        offset: Int? = null
    ): PagingObject<SavedShow> {
        requireScopes(SpotifyScope.UserLibraryRead)

        return get(
            endpointBuilder("/me/shows").with("limit", limit).with("offset", offset).toString()
        ).toNonNullablePagingObject(SavedShow.serializer(), api = api, json = json)
    }

    /**
     * Get a list of the episodes saved in the current Spotify user’s library.
     * This API endpoint is in beta and could change without warning.
     *
     * **Requires** the [SpotifyScope.UserLibraryRead] scope
     *
     * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/library/get-users-saved-albums/)**
     *
     * @param limit The number of objects to return. Default: 50 (or api limit). Minimum: 1. Maximum: 50.
     * @param offset The index of the first item to return. Default: 0. Use with limit to get the next set of items
     * @param market Provide this parameter if you want the list of returned items to be relevant to a particular country.
     * If omitted, the returned items will be relevant to all countries.
     *
     * @return Paging Object of [SavedEpisode] ordered by position in library
     */
    public suspend fun getSavedEpisodes(
        limit: Int? = api.spotifyApiOptions.defaultLimit,
        offset: Int? = null,
        market: Market? = null
    ): PagingObject<SavedEpisode> {
        requireScopes(SpotifyScope.UserLibraryRead)

        return get(
            endpointBuilder("/me/episodes").with("limit", limit).with("offset", offset).with("market", market)
                .toString()
        ).toNonNullablePagingObject(SavedEpisode.serializer(), api = api, json = json)
    }

    /**
     * Check if the [LibraryType] with id [id] is already saved in the current Spotify user’s ‘Your Music’ library.
     *
     * **Requires** the [SpotifyScope.UserLibraryRead] scope
     *
     * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/library/)**
     *
     * @param type The type of object (album or track)
     * @param id The id or uri of the object
     *
     * @throws BadRequestException if [id] is not found
     */
    public suspend fun contains(type: LibraryType, id: String): Boolean = contains(type, ids = arrayOf(id))[0]

    /**
     * Check if one or more of [LibraryType] is already saved in the current Spotify user’s ‘Your Music’ library.
     *
     * **Requires** the [SpotifyScope.UserLibraryRead] scope
     *
     * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/library/)**
     *
     * @param type The type of objects (album or track)
     * @param ids The ids or uris of the objects. Maximum **50** ids.
     *
     * @throws BadRequestException if any of the provided ids is invalid
     */
    public suspend fun contains(type: LibraryType, vararg ids: String): List<Boolean> {
        requireScopes(SpotifyScope.UserLibraryRead)

        if (ids.size > 50 && !api.spotifyApiOptions.allowBulkRequests) {
            throw BadRequestException(
                "Too many ids (${ids.size}) provided, only 50 allowed",
                IllegalArgumentException("Bulk requests are not turned on, and too many ids were provided")
            )
        }
        return ids.toList().chunked(50).map { list ->
            get(
                endpointBuilder("/me/$type/contains").with("ids", list.joinToString(",") { type.id(it).encodeUrl() })
                    .toString()
            ).toList(ListSerializer(Boolean.serializer()), api, json)
        }.flatten()
    }

    /**
     * Save one of [LibraryType] to the current user’s ‘Your Music’ library.
     *
     * **Requires** the [SpotifyScope.UserLibraryModify] scope
     *
     * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/library/)**
     *
     * @param type The type of object (album or track)
     * @param id The id or uri of the object
     *
     * @throws BadRequestException if the id is invalid
     */
    public suspend fun add(type: LibraryType, id: String): Unit = add(type, ids = arrayOf(id))

    /**
     * Save one or more of [LibraryType] to the current user’s ‘Your Music’ library.
     *
     * **Requires** the [SpotifyScope.UserLibraryModify] scope
     *
     * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/library/)**
     *
     * @param type The type of objects to check against (album or track)
     * @param ids The ids or uris of the objects. Maximum **50** ids.
     *
     * @throws BadRequestException if any of the provided ids is invalid
     */
    public suspend fun add(type: LibraryType, vararg ids: String) {
        requireScopes(SpotifyScope.UserLibraryModify)

        if (ids.size > 50 && !api.spotifyApiOptions.allowBulkRequests) {
            throw BadRequestException(
                "Too many ids (${ids.size}) provided, only 50 allowed",
                IllegalArgumentException("Bulk requests are not turned on, and too many ids were provided")
            )
        }
        ids.toList().chunked(50).forEach { list ->
            put(endpointBuilder("/me/$type").with("ids", list.joinToString(",") { type.id(it).encodeUrl() }).toString())
        }
    }

    /**
     * Remove one of [LibraryType] (track or album) from the current user’s ‘Your Music’ library.
     *
     * Changes to a user’s saved items may not be visible in other Spotify applications immediately.
     *
     * **Requires** the [SpotifyScope.UserLibraryModify] scope
     *
     * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/library/)**
     *
     * @param type The type of object to check against (album or track)
     * @param id The id or uri of the object
     *
     * @throws BadRequestException if any of the provided ids is invalid
     */
    public suspend fun remove(type: LibraryType, id: String): Unit = remove(type, ids = arrayOf(id))

    /**
     * Remove one or more of the [LibraryType] (tracks or albums) from the current user’s ‘Your Music’ library.
     *
     * Changes to a user’s saved items may not be visible in other Spotify applications immediately.

     * **Requires** the [SpotifyScope.UserLibraryModify] scope
     *
     * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/library/)**
     *
     * @param type The type of objects to check against (album or track)
     * @param ids The ids or uris of the objects. Maximum **50** ids.
     *
     * @throws BadRequestException if any of the provided ids is invalid
     */
    public suspend fun remove(type: LibraryType, vararg ids: String) {
        requireScopes(SpotifyScope.UserLibraryModify)

        if (ids.size > 50 && !api.spotifyApiOptions.allowBulkRequests) {
            throw BadRequestException(
                "Too many ids (${ids.size}) provided, only 50 allowed",
                IllegalArgumentException("Bulk requests are not turned on, and too many ids were provided")
            )
        }
        ids.toList().chunked(50).forEach { list ->
            delete(
                endpointBuilder("/me/$type").with(
                    "ids",
                    list.joinToString(",") { type.id(it).encodeUrl() }
                ).toString()
            )
        }
    }
}

/**
 * Type of object in a user's Spotify library
 *
 * @param value Spotify id for the type
 * @param id How to transform an id (or uri) input into its Spotify id
 */
public enum class LibraryType(private val value: String, internal val id: (String) -> String) {
    Track("tracks", { PlayableUri(it).id }),
    Album("albums", { AlbumUri(it).id }),
    Episode("episodes", { EpisodeUri(it).id }),
    Show("shows", { ShowUri(it).id });

    override fun toString(): String = value
}
