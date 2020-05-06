/* Spotify Web API, Kotlin Wrapper; MIT License, 2017-2020; Original author: Adam Ratzman */
package com.adamratzman.spotify.models

import com.adamratzman.spotify.SpotifyApi
import com.adamratzman.spotify.SpotifyRestAction
import com.adamratzman.spotify.annotations.SpotifyExperimentalHttpApi
import com.adamratzman.spotify.http.SpotifyEndpoint
import com.adamratzman.spotify.models.PagingTraversalType.FORWARDS
import com.adamratzman.spotify.models.serialization.toCursorBasedPagingObject
import com.adamratzman.spotify.models.serialization.toPagingObject
import com.adamratzman.spotify.utils.runBlocking
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/*
    Types used in PagingObjects and CursorBasedPagingObjects:

    CursorBasedPagingObject:
       PlayHistory
       Artist

    PagingObject:
       SimpleTrack
       SimpleAlbum
       SpotifyCategory
       SimplePlaylist
       SavedTrack
       SavedAlbum
       Artist
       Track
       PlaylistTrack

 */

enum class PagingTraversalType {
    BACKWARDS,
    FORWARDS;
}

/**
 * The offset-based paging object is a container for a set of objects. It contains a key called items
 * (whose value is an array of the requested objects) along with other keys like previous, next and
 * limit that can be useful in future calls.
 *
 * @property href A link to the Web API endpoint returning the full result of the request.
 * @property items The requested data.
 * @property limit The maximum number of items in the response (as set in the query or by default).
 * @property next URL to the next page of items. ( null if none)
 * @property previous URL to the previous page of items. ( null if none)
 * @property total The maximum number of items available to return.
 * @property offset The offset of the items returned (as set in the query or by default).
 */
@Serializable
class PagingObject<T : Any>(
    override val href: String,
    override val items: List<T>,
    override val limit: Int,
    override val next: String? = null,
    override val offset: Int,
    override val previous: String? = null,
    override val total: Int = 0
) : AbstractPagingObject<T>(href, items, limit, next, offset, previous, total) {
    @Suppress("UNCHECKED_CAST")
    override suspend fun getImpl(type: PagingTraversalType): AbstractPagingObject<T>? {
        val endpointFinal = endpoint!!
        return (if (type == FORWARDS) next else previous)?.let { endpoint!!.get(it) }?.let { json ->
            when (itemClazz) {
                SimpleTrack::class -> json.toPagingObject(SimpleTrack.serializer(), null, endpointFinal, endpointFinal.api.json, true)
                SpotifyCategory::class -> json.toPagingObject(SpotifyCategory.serializer(), "categories", endpointFinal, endpointFinal.api.json, true)
                SimpleAlbum::class -> json.toPagingObject(SimpleAlbum.serializer(), "albums", endpointFinal, endpointFinal.api.json, true)
                SimplePlaylist::class -> json.toPagingObject(SimplePlaylist.serializer(), "playlists", endpointFinal, endpointFinal.api.json, true)
                SavedTrack::class -> json.toPagingObject(SavedTrack.serializer(), null, endpointFinal, endpointFinal.api.json, true)
                SavedAlbum::class -> json.toPagingObject(SavedAlbum.serializer(), null, endpointFinal, endpointFinal.api.json, true)
                Artist::class -> json.toPagingObject(Artist.serializer(), null, endpointFinal, endpointFinal.api.json, true)
                Track::class -> json.toPagingObject(Track.serializer(), null, endpointFinal, endpointFinal.api.json, true)
                PlaylistTrack::class -> json.toPagingObject(PlaylistTrack.serializer(), null, endpointFinal, endpointFinal.api.json, true)
                else -> throw IllegalArgumentException("Unknown type in $href response")
            } as? PagingObject<T>
        }
    }

    @SpotifyExperimentalHttpApi
    override suspend fun getWithNextImpl(total: Int): Sequence<AbstractPagingObject<T>> {
        val pagingObjects = mutableListOf<AbstractPagingObject<T>>(this)

        var nxt = next?.let { getNext() }
        while (pagingObjects.size < total && nxt != null) {
            pagingObjects.add(nxt)
            nxt = nxt.next?.let { nxt?.getNext() }
        }

        return pagingObjects.distinctBy { it.href }.asSequence()
    }

    override suspend fun getAllImpl(): Sequence<AbstractPagingObject<T>> {
        val pagingObjects = mutableListOf<AbstractPagingObject<T>>()
        var prev = previous?.let { getPrevious() }
        while (prev != null) {
            pagingObjects.add(prev)
            prev = prev.previous?.let { prev?.getPrevious() }
        }
        pagingObjects.reverse() // closer we are to current, the further we are from the start

        pagingObjects.add(this)

        var nxt = next?.let { getNext() }
        while (nxt != null) {
            pagingObjects.add(nxt)
            nxt = nxt.next?.let { nxt?.getNext() }
        }

        // we don't need to reverse here, as it's in order
        return pagingObjects.asSequence()
    }

    /**
     * Get all PagingObjects associated with the request
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun getAll() = endpoint!!.toAction { (getAllImpl() as Sequence<PagingObject<T>>).toList() }

    /**
     * Synchronously retrieve the next [total] paging objects associated with this [PagingObject], including this [PagingObject].
     *
     * @param total The total amount of [PagingObject] to request, which includes this [PagingObject].
     * @since 3.0.0
     */
    @SpotifyExperimentalHttpApi
    @Suppress("UNCHECKED_CAST")
    suspend fun getWithNext(total: Int) = endpoint!!.toAction { getWithNextImpl(total) }

    /**
     * Get all items of type [T] associated with the request
     */
    override suspend fun getAllItems(context: CoroutineContext) =
            endpoint!!.toAction { getAll().suspendComplete(context).map { it.items }.flatten().asSequence() }
}

/**
 * The cursor-based paging object is a container for a set of objects. It contains a key called
 * items (whose value is an array of the requested objects) along with other keys like next and
 * cursors that can be useful in future calls.
 *
 * @property href A link to the Web API endpoint returning the full result of the request.
 * @property items The requested data.
 * @property limit The maximum number of items in the response (as set in the query or by default).
 * @property next URL to the next page of items. ( null if none)
 * @property total The maximum number of items available to return.
 * @property cursor The cursors used to find the next set of items..
 */
@Serializable
class CursorBasedPagingObject<T : Any>(
    override val href: String,
    override val items: List<T>,
    override val limit: Int,
    override val next: String? = null,
    @SerialName("cursors") val cursor: Cursor,
    override val total: Int = 0
) : AbstractPagingObject<T>(href, items, limit, next, 0, null, total) {
    /**
     * Get all CursorBasedPagingObjects associated with the request
     */
    @Suppress("UNCHECKED_CAST")
    fun getAll() = endpoint!!.toAction {
        getAllImpl() as Sequence<CursorBasedPagingObject<T>>
    }

    /**
     * Synchronously retrieve the next [total] paging objects associated with this [CursorBasedPagingObject], including this [CursorBasedPagingObject].
     *
     * @param total The total amount of [CursorBasedPagingObject] to request, which includes this [CursorBasedPagingObject].
     * @since 3.0.0
     */
    @SpotifyExperimentalHttpApi
    @Suppress("UNCHECKED_CAST")
    fun getWithNext(total: Int) = endpoint!!.toAction {
        getWithNextImpl(total) as Sequence<CursorBasedPagingObject<T>>
    }

    /**
     * Get all items of type [T] associated with the request
     */
    override suspend fun getAllItems(context: CoroutineContext) = endpoint!!.toAction {
        getAll().suspendComplete(context).map { it.items }.flatten().asSequence()
    }

    override suspend fun getImpl(type: PagingTraversalType): AbstractPagingObject<T>? {
        require(type != PagingTraversalType.BACKWARDS) { "CursorBasedPagingObjects only can go forwards" }
        return next?.let { getCursorBasedPagingObject(it) }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun getCursorBasedPagingObject(url: String): CursorBasedPagingObject<T>? {
        val json = endpoint!!.get(url)
        return when (itemClazz) {
            PlayHistory::class -> json.toCursorBasedPagingObject(
                    PlayHistory.serializer(),
                    null,
                    endpoint!!,
                    endpoint!!.api.json
            )
            Artist::class -> json.toCursorBasedPagingObject(
                    Artist.serializer(),
                    null,
                    endpoint!!,
                    endpoint!!.api.json
            )
            else -> throw IllegalArgumentException("Unknown type in $href")
        } as? CursorBasedPagingObject<T>
    }

    override suspend fun getAllImpl(): Sequence<AbstractPagingObject<T>> {
        return generateSequence(this) { runBlocking { it.getImpl(FORWARDS) as? CursorBasedPagingObject<T> } }
    }

    @SpotifyExperimentalHttpApi
    override suspend fun getWithNextImpl(total: Int): Sequence<AbstractPagingObject<T>> {
        val pagingObjects = mutableListOf<AbstractPagingObject<T>>(this)

        var nxt = getNext()
        while (pagingObjects.size < total && nxt != null) {
            pagingObjects.add(nxt)
            nxt = nxt.next?.let { nxt?.getNext() }
        }

        return pagingObjects.distinctBy { it.href }.asSequence()
    }
}

/**
 * The cursor to use as key to find the next (or previous) page of items.
 *
 * @property before The cursor to use as key to find the previous page of items.
 * @property after The cursor to use as key to find the next page of items.
 */
@Serializable
data class Cursor(val before: String? = null, val after: String? = null)

/**
 * @property href A link to the Web API endpoint returning the full result of the request.
 * @property items The requested data.
 * @property limit The maximum number of items in the response (as set in the query or by default).
 * @property next URL to the next page of items. ( null if none)
 * @property previous URL to the previous page of items. ( null if none)
 * @property total The maximum number of items available to return.
 * @property offset The offset of the items returned (as set in the query or by default).
 */
@Serializable
abstract class AbstractPagingObject<T : Any>(
    @Transient open val href: String = TRANSIENT_EMPTY_STRING,
    @Transient open val items: List<T> = listOf(),
    @Transient open val limit: Int = -1,
    @Transient open val next: String? = null,
    @Transient open val offset: Int = 0,
    @Transient open val previous: String? = null,
    @Transient open val total: Int = -1
) : List<T> {
    @Transient
    internal var endpoint: SpotifyEndpoint? = null

    @Transient
    internal var itemClazz: KClass<T>? = null

    internal abstract suspend fun getImpl(type: PagingTraversalType): AbstractPagingObject<T>?
    internal abstract suspend fun getAllImpl(): Sequence<AbstractPagingObject<T>>

    /**
     * Synchronously retrieve the next [total] paging objects associated with this [AbstractPagingObject], including this [AbstractPagingObject].
     *
     * @param total The total amount of [AbstractPagingObject] to request, which includes this [AbstractPagingObject].
     * @since 3.0.0
     */
    @SpotifyExperimentalHttpApi
    internal abstract suspend fun getWithNextImpl(total: Int): Sequence<AbstractPagingObject<T>>

    internal abstract suspend fun getAllItems(context: CoroutineContext = Dispatchers.Default): SpotifyRestAction<Sequence<T>>

    private suspend fun getNextImpl() = getImpl(FORWARDS)
    private suspend fun getPreviousImpl() = getImpl(PagingTraversalType.BACKWARDS)

    suspend fun getNext(): AbstractPagingObject<T>? = getNextImpl()
    suspend fun getPrevious(): AbstractPagingObject<T>? = getPreviousImpl()

    /**
     * Flow from current page backwards.
     * */
    @ExperimentalCoroutinesApi
    fun flowBackward(): Flow<AbstractPagingObject<T>> = flow<AbstractPagingObject<T>> {
        if (previous == null) return@flow
        var next = getPrevious()
        while (next != null) {
            emit(next)
            next = next.getPrevious()
        }
    }.flowOn(Dispatchers.Default)

    /**
     * Flow from current page forwards.
     * */
    @ExperimentalCoroutinesApi
    fun flowForward(): Flow<AbstractPagingObject<T>> = flow<AbstractPagingObject<T>> {
        if (next == null) return@flow
        var next = getNext()
        while (next != null) {
            emit(next)
            next = next.getNext()
        }
    }.flowOn(Dispatchers.Default)

    @ExperimentalCoroutinesApi
    fun flowStartOrdered(): Flow<AbstractPagingObject<T>> =
            flow {
                if (previous == null) return@flow
                flowBackward().toList().reversed().also {
                    emitAll(it.asFlow())
                }
            }.flowOn(Dispatchers.Default)

    @ExperimentalCoroutinesApi
    fun flowEndOrdered(): Flow<AbstractPagingObject<T>> = flowForward()

    // A paging object is also a list, and instantiation by deserialization doesn't properly store the items list internally
    // so we implement list methods

    override val size: Int get() = items.size
    override fun contains(element: T) = items.contains(element)
    override fun containsAll(elements: Collection<T>) = items.containsAll(elements)
    override fun get(index: Int) = items[index]
    override fun indexOf(element: T) = items.indexOf(element)
    override fun isEmpty() = items.isEmpty()
    override fun iterator() = items.iterator()
    override fun lastIndexOf(element: T) = items.lastIndexOf(element)
    override fun listIterator() = items.listIterator()
    override fun listIterator(index: Int) = items.listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int) = items.subList(fromIndex, toIndex)
}

internal fun Any.instantiatePagingObjects(spotifyApi: SpotifyApi<*, *>) = when (this) {
    is FeaturedPlaylists -> this.playlists
    is Show -> this.episodes
    is Album -> this.tracks
    is Playlist -> this.tracks
    else -> null
}.let { it?.endpoint = spotifyApi.tracks; this }
