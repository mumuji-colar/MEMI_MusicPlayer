package com.mmc.merimusicplayer.data.sync.mapping

import android.content.Context
import com.mmc.merimusicplayer.data.local.media.LocalSongSupport
import com.mmc.merimusicplayer.data.model.sync.SyncRecentPlay
import com.mmc.merimusicplayer.data.model.sync.SyncSong
import com.mmc.merimusicplayer.data.history.toSongItem
import com.mmc.merimusicplayer.data.model.history.PlayedEntry

internal fun buildRecentPlaySyncSnapshots(history: List<PlayedEntry>, readDeviceId: () -> String, localizedContext: Context, optimizeLegacyLyrics: Boolean = false): List<SyncRecentPlay> {
    val syncRecentPlays = history
        .filterNot {
            !it.localFilePath.isNullOrBlank() ||
                LocalSongSupport.isLocalSong(it.album, it.mediaUri, it.albumId, localizedContext)
        }
        .map { playedEntry ->
            SyncRecentPlay(
                songId = playedEntry.id,
                song = SyncSong.fromSongItem(playedEntry.toSongItem(), localizedContext, optimizeLegacyLyrics),
                playedAt = playedEntry.playedAt,
                deviceId = readDeviceId(),
                resumePositionMs = playedEntry.resumePositionMs
            )
        }
    return syncRecentPlays
}
