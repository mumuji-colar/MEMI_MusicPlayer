@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.mmc.merimusicplayer.core.player.lifecycle

import coil.Coil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.mmc.merimusicplayer.common.logging.NPLogger
import com.mmc.merimusicplayer.core.player.PlayerManager
import com.mmc.merimusicplayer.core.player.cache.clearRetainingImageCache
import com.mmc.merimusicplayer.data.identity.stableKey
import com.mmc.merimusicplayer.data.model.storage.ExtraCacheClearResult
import com.mmc.merimusicplayer.data.model.storage.PlayerCacheClearOutcome
import com.mmc.merimusicplayer.data.model.storage.StorageCacheClearOptions

private val storageCacheClearMutex = Mutex()

internal suspend fun PlayerManager.clearStorageCachesImpl(options: StorageCacheClearOptions): PlayerCacheClearOutcome =
    storageCacheClearMutex.withLock {
        withContext(Dispatchers.IO) {
            val empty = ExtraCacheClearResult(true, 0, 0, 0)
            if (!options.needsPlayerCacheClear) return@withContext PlayerCacheClearOutcome(empty)
            var completed = empty
            var retentionUnavailable = options.retainRecentSongs > 0
            try {
                val recent = loadRetainedStorageCacheSongs(options.retainRecentSongs)
                    ?: return@withContext PlayerCacheClearOutcome(empty.copy(success = false), retentionUnavailable = true)
                retentionUnavailable = false
                val plan = snapshotStorageCacheRetention(recent, options.retainRecentSongs)
                val audio = if (options.audioCache) cache?.let {
                    clearAudioStorageCache(it, plan.retainedSongs, plan.protectedKeys, plan.preserveUnmapped) { remove ->
                        synchronized(storageCachePlaybackLock) {
                            if (playbackRequestToken != plan.requestToken || currentSongFlow.value?.stableKey() != plan.currentSongKey) {
                                false
                            } else {
                                remove()
                                true
                            }
                        }
                    }
                } ?: empty else empty
                completed = audio
                val image = if (options.imageCache) {
                    clearRetainingImageCache(Coil.imageLoader(application), plan.retainedCoverUrls, plan.preserveUnmapped)
                } else empty
                PlayerCacheClearOutcome(
                    ExtraCacheClearResult(audio.success && image.success, audio.freedBytes + image.freedBytes, 0,
                        audio.deletedFiles + image.deletedFiles)
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                NPLogger.w("NERI-Player", "Storage cache clear failed", error)
                PlayerCacheClearOutcome(completed.copy(success = false), retentionUnavailable)
            }
        }
    }
