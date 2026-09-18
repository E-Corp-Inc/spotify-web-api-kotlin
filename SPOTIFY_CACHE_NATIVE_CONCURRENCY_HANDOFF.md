# Spotify cache concurrency investigation

## Context

`MediaApp` has intermittently crashed on iOS while several Spotify-backed screens were loading.
The clearest retained and symbolicated crash was executing the request-cache cleanup path during a
`getSavedShows` request. Other retained reports terminated in an application monitoring
unhandled-exception hook, so they did not preserve enough of the original Kotlin failure to prove a
single root cause.

This document records a strong hypothesis and a safe investigation plan. It does not claim that the
cache is conclusively responsible until a regression test or a newly captured crash reproduces the
failure.

The consuming application is located at:

```text
/Users/mridjali-prest/MyDocuments/Projects/CMP/apps/MediaApp
```

## Evidence in this library

The relevant implementation is in:

- `src/commonMain/kotlin/com.adamratzman.spotify/http/Endpoints.kt`
- `src/commonMain/kotlin/com.adamratzman.spotify/utils/ConcurrentHashMap.kt`
- `src/nativeDarwinMain/kotlin/com/adamratzman/spotify/utils/PlatformUtils.kt`

`SpotifyCache` stores request state in a public `ConcurrentHashMap`. On Darwin, however,
`ConcurrentHashMap` is a typealias for a regular Kotlin `HashMap`:

```kotlin
public actual typealias ConcurrentHashMap<K, V> = HashMap<K, V>
```

Cache access is composed from multiple independent operations. `checkCache` iterates and mutates
the entry set, reads its size, sorts another entry view, and removes selected entries. In
particular, expiration currently uses:

```kotlin
cachedRequests.entries.removeAll { !it.value.isStillValid() }
```

`SpotifyEndpoint.bulkStatelessRequest` deliberately runs request chunks concurrently, and ordinary
application callers can also invoke endpoint requests concurrently. On Darwin, one request may
therefore read or write the `HashMap` while another request is iterating or pruning it.

Disabling provider request caching in MediaApp stopped the observed crash during one test period,
but that was only an A/B correlation. Caching was restored because requests consume a limited
Spotify quota and not every feature has an application-owned replacement cache.

## Required outcome

Keep request caching enabled and preserve parallel network execution. Synchronize only the short
in-memory cache transactions.

The corrected abstraction should guarantee that each of these is safe relative to every other
cache operation:

1. Prune expired entries and read one request.
2. Store one response, prune expired entries, and enforce the cache limit.
3. Remove one request and perform any required cleanup.
4. Clear the cache.
5. Produce the snapshot returned by `GenericSpotifyApi.getCache()`.

Pruning, size calculation, eviction selection, and eviction must form one protected transaction.
Making only `get`, `put`, or `remove` individually thread-safe is insufficient.

## Design constraints

- Do not serialize HTTP requests or hold a lock during network I/O, parsing, callbacks, delays, or
  token refresh.
- Do not disable caching on Kotlin/Native.
- Do not add an application-wide request mutex in MediaApp as the primary fix.
- Avoid exposing a mutable entry view that callers can mutate without the same lock.
- Preserve the existing public API where practical. If `cachedRequests` cannot safely remain a
  public mutable map, document the compatibility decision and provide a read-only snapshot API.
- Preserve coroutine cancellation. Never catch or translate `CancellationException` as a cache
  failure.
- Support the library's declared platforms. Do not solve Darwin while silently breaking JVM,
  Android, JS, or other enabled native targets.
- Prefer a small synchronization primitive over a broad redesign. A multiplatform lock from an
  established dependency, or a narrowly scoped internal `expect`/`actual` lock, should be evaluated
  against the existing target matrix. A coroutine `Mutex` is appropriate only if the API can be
  made suspend-safe without blocking or awkward `runBlocking` bridges.

## Suggested implementation sequence

1. Add deterministic `SpotifyCache` unit tests for expiration, limit eviction, clearing, removal,
   and snapshot behavior before changing its representation.
2. Add a stress test that launches concurrent readers, writers, removals, clears, and snapshot
   requests against the same cache. The test must fail or expose unsafe behavior on the old Darwin
   implementation often enough to serve as a regression test.
3. Encapsulate the mutable map inside `SpotifyCache` and introduce one synchronization boundary for
   complete cache transactions.
4. Replace `GenericSpotifyApi.getCache()` access to `cachedRequests.asList()` with the protected
   snapshot operation.
5. Run JVM tests and an `iosSimulatorArm64` test binary. Do not add or restore an iOS x64 target for
   MediaApp validation.
6. Publish the fixed library to Maven Local or use an included/local dependency with a distinct
   snapshot version.
7. Point MediaApp at that snapshot, keep Spotify request caching enabled, and exercise Library,
   Home, ListeningStats, and PlaybackHistory concurrently on the iOS simulator and a device when
   available.
8. Only after the regression test and consuming-app validation pass should the fix be prepared for
   an upstream pull request or release.

## Validation expectations

At minimum, record the exact commands and results for:

- the focused cache tests on JVM;
- the focused cache tests on `iosSimulatorArm64`;
- the library's normal JVM and iOS compilation checks;
- MediaApp iOS compilation, full simulator build, install, and launch;
- repeated concurrent navigation/fetch scenarios with caching enabled;
- confirmation that no new cache-related crash report was produced.

If the failure cannot be reproduced, retain the concurrency hardening only when the implementation
is demonstrably correct and the tests exercise concurrent mutation. Do not present absence of a
crash during a short manual run as proof of the root cause.

## Implemented solution

The cache has been changed so that its mutable state is private and every complete cache operation
is protected by the same lock. This is important because making individual map calls thread-safe
would not be enough: expiration and limit enforcement each require several reads and writes to act
as one transaction.

The protected transactions are now:

- prune expired entries and read one cached response;
- store one response, prune expired entries, calculate the cache size, and evict old entries;
- prune entries and remove one request;
- clear the cache; and
- copy the cache into a snapshot.

Only these short, in-memory operations hold the lock. Network requests, response parsing, token
refreshes, callbacks, coroutine delays, and other suspending work remain outside it. Concurrent HTTP
requests therefore continue to run in parallel.

### Platform synchronization

A small internal `SpotifyCacheLock` `expect`/`actual` abstraction was added instead of introducing a
new dependency or converting the cache API to suspending functions:

- JVM and Android use `ReentrantLock`;
- Darwin targets use `NSLock`;
- other Kotlin/Native desktop targets use Kotlin's atomic API for a short spin lock; and
- JavaScript executes the block directly because its supported runtime is single-threaded.

The backing map is an ordinary private mutable map because all access is now controlled by this
lock. Correctness no longer depends on whether a platform's `ConcurrentHashMap` actual type is a
real concurrent collection or a `HashMap` typealias.

### Avoiding live map-view mutation

The original expiration path called `cachedRequests.entries.removeAll { ... }`. On Kotlin/Native,
the regression test reproduced a `ConcurrentModificationException` in this path even after access
was serialized, because it mutated a `HashMap` through its live entry view while iterating it.

Expiration and eviction now first select keys from a snapshot and then remove those keys while the
cache lock is held. This avoids both cross-thread races and unsafe mutation through a live entry
view.

### Public API compatibility

`SpotifyCache.cachedRequests` keeps its existing public `ConcurrentHashMap` return type for source
and binary compatibility, but now returns a detached snapshot. Mutating that returned map cannot
modify the real cache or bypass synchronization. The property is deprecated to make this changed
mutation behavior visible to callers.

The new `SpotifyCache.snapshot()` function is the supported read-only inspection API.
`GenericSpotifyApi.getCache()` now builds its result from protected snapshots instead of reading
the mutable backing maps directly.

### Coroutine cancellation

Cache locking is synchronous, contains no suspension points, and does not catch exceptions. The HTTP
execution path was also narrowed to translate only `TimeoutCancellationException` into the
library's `TimeoutException`. An unrelated parent or caller cancellation continues to propagate as
`CancellationException` rather than being reported as a request timeout.

### Regression coverage

`SpotifyCacheTest` covers expiration, cache-limit eviction, explicit removal, clearing, and detached
snapshot behavior. Its concurrency test runs 12 workers against the same cache, performing 12,000
combined writes, reads, removals, snapshots, and clears, then verifies that limit enforcement still
holds.

The same tests run from `commonTest`, so they exercise the JVM implementation and the real Darwin
implementation in an iOS Simulator binary.

### Validation performed

The following checks passed:

```text
./gradlew jvmTest --tests com.adamratzman.spotify.http.SpotifyCacheTest \
  iosSimulatorArm64Test --tests com.adamratzman.spotify.http.SpotifyCacheTest --console=plain

./gradlew compileKotlinJs compileKotlinLinuxX64 compileKotlinMacosX64 \
  compileDebugKotlinAndroid --console=plain

git diff --check
```

The repository-wide `spotlessCheck` currently fails on a pre-existing wildcard import in
`ArtistApi.kt`; the failure is unrelated to the cache changes. No commit has been created pending
review.

## Prompt for a dedicated Codex session

Start Codex from this repository and use:

> Investigate and fix the concurrency safety of `SpotifyCache` on Kotlin/Native. Darwin currently
> typealiases `ConcurrentHashMap` to `HashMap`, while `SpotifyCache.checkCache` iterates and mutates
> `entries` during concurrent requests. Keep request caching enabled, synchronize only cache state
> operations, preserve cancellation and public compatibility where reasonable, and add JVM plus
> iOS Simulator concurrency regression tests. Read
> `SPOTIFY_CACHE_NATIVE_CONCURRENCY_HANDOFF.md` before editing. Do not commit until I review the
> changes.
