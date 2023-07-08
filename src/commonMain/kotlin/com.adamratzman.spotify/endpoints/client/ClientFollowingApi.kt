/* Spotify Web API, Kotlin Wrapper; MIT License, 2017-2022; Original author: Adam Ratzman */
package com.adamratzman.spotify.endpoints.client

import com.adamratzman.spotify.GenericSpotifyApi
import com.adamratzman.spotify.SpotifyClientApi
import com.adamratzman.spotify.SpotifyException.BadRequestException
import com.adamratzman.spotify.SpotifyScope
import com.adamratzman.spotify.endpoints.pub.FollowingApi
import com.adamratzman.spotify.models.Artist
import com.adamratzman.spotify.models.ArtistUri
import com.adamratzman.spotify.models.CursorBasedPagingObject
import com.adamratzman.spotify.models.PlaylistUri
import com.adamratzman.spotify.models.UserUri
import com.adamratzman.spotify.models.serialization.toCursorBasedPagingObject
import com.adamratzman.spotify.models.serialization.toList
import com.adamratzman.spotify.utils.encodeUrl
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

/**
 * These endpoints allow you manage the artists, users and playlists that a Spotify user follows.
 *
 * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/follow/)**
 */
public class ClientFollowingApi(api: GenericSpotifyApi) : FollowingApi(api) {
    /**
     * Check to see if the current user is following another Spotify user.
     *
     * **Requires** the [SpotifyScope.UserFollowRead] scope
     *
     * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/follow/check-current-user-follows/)**
     *
     * @param user The user id or uri to check.
     *
     * @throws BadRequestException if [user] is a non-existing id
     * @return Whether the current user is following [user]
     */
    public suspend fun isFollowingUser(user: String): Boolean {
        requireScopes(SpotifyScope.UserFollowRead)
        return isFollowingUsers(user)[0]
    }

    /**
     * Check to see if the current Spotify user is following the specified playlist.
     *
     * Checking if the user is privately following a playlist is only possible for the current user when
     * that user has granted access to the [SpotifyScope.PlaylistReadPrivate] scope.
     *
     * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/follow/check-user-following-playlist/)**
     *
     * @param playlistId playlist id or uri
     *
     * @return Boolean representing whether the user follows the playlist
     *
     * @throws [BadRequestException] if the playlist is not found
     * @return Whether the current user is following [playlistId]
     */
    public suspend fun isFollowingPlaylist(playlistId: String): Boolean {
        return isFollowingPlaylist(
            playlistId,
            (api as SpotifyClientApi).getUserId()
        )
    }

    /**
     * Check to see if the current user is following one or more other Spotify users.
     *
     * **Requires** the [SpotifyScope.UserFollowRead] scope
     *
     * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/follow/check-current-user-follows/)**
     *
     * @param users List of the user Spotify IDs to check. Max 50
     *
     * @throws BadRequestException if [users] contains a non-existing id
     * @return A list of booleans corresponding to [users] of whether the current user is following that user
     */
    public suspend fun isFollowingUsers(vararg users: String): List<Boolean> {
        requireScopes(SpotifyScope.UserFollowRead)
        checkBulkRequesting(50, users.size)
        return bulkStatelessRequest(50, users.toList()) { chunk ->
            get(
                endpointBuilder("/me/following/contains").with("type", "user")
                    .with("ids", chunk.joinToString(",") { UserUri(it).id.encodeUrl() }).toString()
            ).toList(ListSerializer(Boolean.serializer()), api, json)
        }.flatten()
    }

    /**
     * Check to see if the current user is following a Spotify artist.
     *
     * **Requires** the [SpotifyScope.UserFollowRead] scope
     *
     * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/follow/check-current-user-follows/)**
     *
     * @param artist The artist id to check.
     *
     * @throws BadRequestException if [artist] is a non-existing id
     * @return Whether the current user is following [artist]
     */
    public suspend fun isFollowingArtist(artist: String): Boolean = isFollowingArtists(artist)[0]

    /**
     * Check to see if the current user is following one or more artists.
     *
     * **Requires** the [SpotifyScope.UserFollowRead] scope
     *
     * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/follow/check-current-user-follows/)**
     *
     * @param artists List of the artist ids or uris to check. Max 50
     *
     * @throws BadRequestException if [artists] contains a non-existing id
     * @return A list of booleans corresponding to [artists] of whether the current user is following that artist
     */
    public suspend fun isFollowingArtists(vararg artists: String): List<Boolean> {
        requireScopes(SpotifyScope.UserFollowRead)
        checkBulkRequesting(50, artists.size)
        return bulkStatelessRequest(50, artists.toList()) { chunk ->
            get(
                endpointBuilder("/me/following/contains").with("type", "artist")
                    .with("ids", chunk.joinToString(",") { ArtistUri(it).id.encodeUrl() }).toString()
            ).toList(ListSerializer(Boolean.serializer()), api, json)
        }.flatten()
    }

    /**
     * Get the current user’s followed artists.
     *
     * **Requires** the [SpotifyScope.UserFollowRead] scope
     *
     * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/follow/get-followed/)**
     *
     * @param limit The maximum number of items to return. Default: 50 (or api limit). Minimum: 1. Maximum: 50.
     * @param after The last artist ID retrieved from the previous request.
     *
     * @return [CursorBasedPagingObject] ([Information about them](https://github.com/adamint/com.adamratzman.spotify-web-api-kotlin/blob/master/README.md#the-benefits-of-linkedresults-pagingobjects-and-cursor-based-paging-objects)
     * with full [Artist] objects
     */
    public suspend fun getFollowedArtists(
        limit: Int? = api.spotifyApiOptions.defaultLimit,
        after: String? = null
    ): CursorBasedPagingObject<Artist> {
        requireScopes(SpotifyScope.UserFollowRead)
        return get(
            endpointBuilder("/me/following").with("type", "artist").with("limit", limit).with(
                "after",
                after
            ).toString()
        ).toCursorBasedPagingObject(Artist::class, Artist.serializer(), "artists", api, json)
    }

    /**
     * Add the current user as a follower of another user
     *
     * **Requires** the [SpotifyScope.UserFollowModify] scope
     *
     * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/follow/follow-artists-users/)**
     *
     * @throws BadRequestException if an invalid id is provided
     */
    public suspend fun followUser(user: String): Unit = followUsers(user)

    /**
     * Add the current user as a follower of other users
     *
     * **Requires** the [SpotifyScope.UserFollowModify] scope
     *
     * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/follow/follow-artists-users/)**
     *
     * @param users User ids or uris. Maximum **50**.
     *
     * @throws BadRequestException if an invalid id is provided
     */
    public suspend fun followUsers(vararg users: String) {
        requireScopes(SpotifyScope.UserFollowModify)
        checkBulkRequesting(50, users.size)
        bulkStatelessRequest(50, users.toList()) { chunk ->
            put(
                endpointBuilder("/me/following").with("type", "user")
                    .with("ids", chunk.joinToString(",") { UserUri(it).id.encodeUrl() }).toString()
            )
        }
    }

    /**
     * Add the current user as a follower of an artist
     *
     * **Requires** the [SpotifyScope.UserFollowModify] scope
     *
     * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/follow/follow-artists-users/)**
     *
     * @throws BadRequestException if an invalid id is provided
     */
    public suspend fun followArtist(artistId: String): Unit = followArtists(artistId)

    /**
     * Add the current user as a follower of other artists
     *
     * **Requires** the [SpotifyScope.UserFollowModify] scope
     *
     * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/follow/follow-artists-users/)**
     *
     * @param artists User ids or uris. Maximum **50**.
     *
     * @throws BadRequestException if an invalid id is provided
     */
    public suspend fun followArtists(vararg artists: String) {
        requireScopes(SpotifyScope.UserFollowModify)
        checkBulkRequesting(50, artists.size)
        bulkStatelessRequest(50, artists.toList()) { chunk ->
            put(
                endpointBuilder("/me/following").with("type", "artist")
                    .with("ids", chunk.joinToString(",") { ArtistUri(it).id.encodeUrl() }).toString()
            )
        }
    }

    /**
     * Add the current user as a follower of a playlist.
     *
     * Following a playlist publicly requires authorization of the [SpotifyScope.PlaylistModifyPublic] scope;
     * following it privately requires the [SpotifyScope.PlaylistModifyPrivate] scope.
     *
     * Note that the scopes you provide determine only whether the current user can themselves follow the playlist
     * publicly or privately (i.e. show others what they are following), not whether the playlist itself is
     * public or private.
     *
     * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/follow/follow-playlist/)**
     *
     * @param playlist the id or uri of the playlist. Any playlist can be followed, regardless of its
     * public/private status, as long as you know its playlist ID.
     * @param followPublicly Defaults to true. If true the playlist will be included in user’s public playlists,
     * if false it will remain private. To be able to follow playlists privately, the user must have granted the playlist-modify-private scope.
     *
     * @throws BadRequestException if the playlist is not found
     */
    public suspend fun followPlaylist(playlist: String, followPublicly: Boolean = true): String {
        requireScopes(SpotifyScope.PlaylistModifyPublic, SpotifyScope.PlaylistModifyPrivate, anyOf = true)

        return put(
            endpointBuilder("/playlists/${PlaylistUri(playlist).id}/followers").toString(),
            "{\"public\": $followPublicly}"
        )
    }

    /**
     * Remove the current user as a follower of another user
     *
     * **Requires** the [SpotifyScope.UserFollowModify] scope
     *
     * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/follow/unfollow-artists-users/)**
     *
     * @param user The user to be unfollowed from
     *
     * @throws BadRequestException if [user] is not found
     */
    public suspend fun unfollowUser(user: String): Unit = unfollowUsers(user)

    /**
     * Remove the current user as a follower of other users
     *
     * **Requires** the [SpotifyScope.UserFollowModify] scope
     *
     * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/follow/unfollow-artists-users/)**
     *
     * @param users The users to be unfollowed from. Maximum **50**.
     *
     * @throws BadRequestException if an invalid id is provided
     */
    public suspend fun unfollowUsers(vararg users: String) {
        requireScopes(SpotifyScope.UserFollowModify)
        checkBulkRequesting(50, users.size)
        bulkStatelessRequest(50, users.toList()) { list ->
            delete(
                endpointBuilder("/me/following").with("type", "user")
                    .with("ids", list.joinToString(",") { UserUri(it).id.encodeUrl() }).toString()
            )
        }
    }

    /**
     * Remove the current user as a follower of an artist
     *
     * **Requires** the [SpotifyScope.UserFollowModify] scope
     *
     * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/follow/unfollow-artists-users/)**
     *
     * @param artist The artist to be unfollowed from
     *
     * @throws BadRequestException if an invalid id is provided
     */
    public suspend fun unfollowArtist(artist: String): Unit = unfollowArtists(artist)

    /**
     * Remove the current user as a follower of artists
     *
     * **Requires** the [SpotifyScope.UserFollowModify] scope
     *
     * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/follow/unfollow-artists-users/)**
     *
     * @param artists The artists to be unfollowed from. Maximum **50**.
     *
     *
     * @throws BadRequestException if an invalid id is provided
     */
    public suspend fun unfollowArtists(vararg artists: String) {
        requireScopes(SpotifyScope.UserFollowModify)
        checkBulkRequesting(50, artists.size)
        bulkStatelessRequest(50, artists.toList()) { list ->
            delete(
                endpointBuilder("/me/following").with("type", "artist")
                    .with("ids", list.joinToString(",") { ArtistUri(it).id.encodeUrl() }).toString()
            )
        }
    }

    /**
     * Remove the current user as a follower of a playlist.
     *
     * Unfollowing a publicly followed playlist for a user requires authorization of the [SpotifyScope.PlaylistModifyPublic] scope;
     * unfollowing a privately followed playlist requires the [SpotifyScope.PlaylistModifyPrivate] scope.
     *
     * Note that the scopes you provide relate only to whether the current user is following the playlist publicly or
     * privately (i.e. showing others what they are following), not whether the playlist itself is public or private.
     *
     * **[Api Reference](https://developer.spotify.com/documentation/web-api/reference/follow/unfollow-playlist/)**
     *
     * @param playlist The id or uri of the playlist that is to be no longer followed.
     *
     * @throws BadRequestException if the playlist is not found
     */
    public suspend fun unfollowPlaylist(playlist: String): String {
        requireScopes(SpotifyScope.PlaylistModifyPublic, SpotifyScope.PlaylistModifyPrivate, anyOf = true)

        return delete(endpointBuilder("/playlists/${PlaylistUri(playlist).id}/followers").toString())
    }
}
