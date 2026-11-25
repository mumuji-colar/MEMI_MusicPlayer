package com.mmc.merimusicplayer.data.sync.host

import android.content.Context
import com.mmc.merimusicplayer.data.local.media.LocalSongSupport
import com.mmc.merimusicplayer.data.local.playlist.system.LocalFilesPlaylist
import com.mmc.merimusicplayer.data.local.playlist.system.SystemLocalPlaylists
import com.mmc.merimusicplayer.data.model.sync.SyncSystemPlaylist
import com.mmc.merimusicplayer.data.sync.sanitize.SyncSanitizationHost
import com.mmc.merimusicplayer.common.locale.LanguageManager

internal class AndroidSyncSanitizationHost(
    private val context: Context,
    private val readLocalAlbumNames: () -> Set<String> = { LocalFilesPlaylist.candidateNames(context) }
) : SyncSanitizationHost {
    override val localFilesPlaylistId = LocalFilesPlaylist.SYSTEM_ID
    private val localAlbumNames by lazy(readLocalAlbumNames)

    override fun systemPlaylist(id: Long, name: String): SyncSystemPlaylist? =
        SystemLocalPlaylists.resolve(id, name, LanguageManager.applyLanguage(context))?.let {
            SyncSystemPlaylist(it.id, it.currentName)
        }

    override fun isLocalSong(album: String?, mediaUri: String?, albumId: Long): Boolean =
        LocalSongSupport.isLocalSong(album, mediaUri, albumId, localAlbumNames)

    override fun sanitizeMediaUri(mediaUri: String?): String? =
        LocalSongSupport.sanitizeMediaUriForSync(mediaUri)
}
