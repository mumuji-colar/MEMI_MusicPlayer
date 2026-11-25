package com.mmc.merimusicplayer.core.download.index

import com.mmc.merimusicplayer.data.identity.identity
import com.mmc.merimusicplayer.data.identity.stableKey

import com.mmc.merimusicplayer.data.model.download.DownloadedAudioEmbeddingState
import com.mmc.merimusicplayer.core.download.ManagedDownloadStorage
import com.mmc.merimusicplayer.core.download.storage.METADATA_SUFFIX
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.model.stableKey

internal object ManagedLibraryFastIndexEntryFactory {
    fun fromCompletedDownload(
        libraryId: String,
        song: SongItem,
        audio: ManagedDownloadStorage.StoredEntry,
        state: String,
        coverPath: String? = null,
        metadataEmbeddingState: DownloadedAudioEmbeddingState? = null,
        updatedAtMs: Long = System.currentTimeMillis()
    ): ManagedLibraryIndexEntry {
        val identity = song.identity()
        val stableKey = identity.stableKey()
        return ManagedLibraryIndexEntry(
            stableKey = stableKey,
            artifactId = "managed:$libraryId:$stableKey",
            audioName = audio.logicalName,
            audioReference = audio.reference,
            metadataName = "${audio.logicalName}$METADATA_SUFFIX",
            state = state,
            metadataEmbeddingState = metadataEmbeddingState,
            downloadTimeMs = audio.lastModifiedMs.takeIf { it > 0L },
            updatedAtMs = updatedAtMs,
            songId = song.id.takeIf { it > 0L },
            title = song.name,
            artist = song.artist,
            album = song.album,
            mediaUri = identity.mediaUri ?: song.mediaUri,
            channelId = song.channelId,
            audioId = song.audioId,
            subAudioId = song.subAudioId,
            playlistContextId = song.playlistContextId,
            durationMs = song.durationMs.takeIf { it > 0L },
            coverPath = coverPath,
            logicalCreatedAtMs = song.logicalCreatedAtMs
                ?: song.addedAt.takeIf { it > 0L }
                ?: audio.lastModifiedMs.takeIf { it > 0L },
            createdAtSource = song.createdAtSource
                ?: when {
                    song.logicalCreatedAtMs != null -> "MANAGED_COMMIT"
                    song.addedAt > 0L -> "DOWNLOAD_TIME"
                    audio.lastModifiedMs > 0L -> "MTIME"
                    else -> null
                },
            createdAtConfidence = song.createdAtConfidence
                ?: when {
                    song.logicalCreatedAtMs != null -> "EXACT"
                    song.addedAt > 0L || audio.lastModifiedMs > 0L -> "INFERRED"
                    else -> null
                }
        )
    }
}
