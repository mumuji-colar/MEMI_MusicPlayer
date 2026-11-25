package com.mmc.merimusicplayer.data.sync.sanitize

import com.mmc.merimusicplayer.data.model.sync.SyncSystemPlaylist

interface SyncSanitizationHost {
    val localFilesPlaylistId: Long
    fun systemPlaylist(id: Long, name: String): SyncSystemPlaylist?
    fun isLocalSong(album: String?, mediaUri: String?, albumId: Long): Boolean
    fun sanitizeMediaUri(mediaUri: String?): String?
}
