package com.mmc.merimusicplayer.ui.screen.playlist.reorder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.model.playlist.LocalPlaylist

@Composable
internal fun rememberLocalPlaylistReorderCoverPlaylist(
    playlist: LocalPlaylist,
    displayedSongs: List<SongItem>,
    tabKey: Any,
    freezeOrder: Boolean
): LocalPlaylist {
    // 仅忽略持拖和等待保存期间的纯排序，成员或歌曲信息变化仍需更新封面
    val songContents = if (freezeOrder) {
        remember(displayedSongs) { displayedSongs.groupingBy { it }.eachCount() }
    } else {
        null
    }
    val displayOrder = displayedSongs.takeUnless { freezeOrder }
    return remember(playlist, tabKey, freezeOrder, songContents, displayOrder) {
        playlist.copy(songs = displayedSongs.toMutableList())
    }
}
