package com.mmc.merimusicplayer.ui.screen.playlist.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mmc.merimusicplayer.data.local.media.displayArtist
import com.mmc.merimusicplayer.data.local.media.displayName
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.mv.MvLauncher
import com.mmc.merimusicplayer.ui.component.common.MvBadge
import com.mmc.merimusicplayer.ui.component.download.SongDownloadSubtitle
import com.mmc.merimusicplayer.ui.haptic.performHapticFeedback
import com.mmc.merimusicplayer.ui.screen.playlist.LocalPlaylistSongArtwork
import com.mmc.merimusicplayer.ui.screen.playlist.PlayingIndicator
import com.mmc.merimusicplayer.ui.screen.playlist.PlaylistModernListItemSurface
import com.mmc.merimusicplayer.ui.screen.playlist.playlistModernListPrimaryContentColor
import com.mmc.merimusicplayer.ui.screen.playlist.playlistModernListSecondaryContentColor
import com.mmc.merimusicplayer.ui.screen.playlist.playlistModernListTertiaryContentColor
import com.mmc.merimusicplayer.ui.screen.playlist.shouldResolveLocalPlaylistRowArtworkFallback
import com.mmc.merimusicplayer.util.format.formatDuration

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun PlaylistModernSongRow(
    song: SongItem,
    index: Int,
    coverUrl: String?,
    offlineMode: Boolean,
    selectionMode: Boolean,
    selected: Boolean,
    current: Boolean,
    playing: Boolean,
    downloaded: Boolean,
    downloadPresenceVersion: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelection: () -> Unit,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
    compact: Boolean = false,
    trailing: @Composable RowScope.() -> Unit
) {
    val context = LocalContext.current
    // B 站直接判定；网易云要查一次（仓库内部合并成批量请求）。只做标识，不拦截行点击
    var hasMv by remember(song.id) { mutableStateOf(MvLauncher.canOpenMv(song)) }
    LaunchedEffect(song.id, song.channelId) {
        if (!hasMv) hasMv = MvLauncher.hasMv(song)
    }
    val rowContainerColor = if (selectionMode && selected) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
    } else Color.Transparent
    PlaylistModernListItemSurface(coverUrl, offlineMode, modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().background(rowContainerColor)
                .combinedClickable(
                    onClick = { context.performHapticFeedback(); onClick() },
                    onLongClick = onLongClick
                ).padding(horizontal = if (compact) 0.dp else 16.dp, vertical = if (compact) 10.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(if (compact && !selectionMode) 28.dp else 48.dp), contentAlignment = Alignment.Center) {
                    if (selectionMode) {
                        Checkbox(checked = selected, onCheckedChange = { onToggleSelection() })
                    } else {
                        Text(
                            (index + 1).toString(),
                            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall,
                            color = playlistModernListTertiaryContentColor(),
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
                val resolveArtworkFallback = shouldResolveLocalPlaylistRowArtworkFallback()
                LocalPlaylistSongArtwork(
                    song = song,
                    offlineMode = offlineMode,
                    resolveLocalFallback = resolveArtworkFallback,
                    downloadPresenceVersion = downloadPresenceVersion,
                    allowEmbeddedCoverFallback = resolveArtworkFallback
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            song.displayName(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                            color = playlistModernListPrimaryContentColor(),
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (hasMv) {
                            Spacer(Modifier.width(6.dp))
                            MvBadge()
                        }
                    }
                    if (compact) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SongDownloadSubtitle(
                                text = song.displayArtist(), downloaded = downloaded,
                                modifier = Modifier.weight(1f), color = playlistModernListSecondaryContentColor()
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(formatDuration(song.durationMs), style = MaterialTheme.typography.bodySmall,
                                color = playlistModernListSecondaryContentColor())
                        }
                    } else {
                        SongDownloadSubtitle(
                            text = song.displayArtist(), downloaded = downloaded,
                            color = playlistModernListSecondaryContentColor()
                        )
                    }
                }
            }
            if (selectionMode) {
                trailing()
            } else {
                AnimatedVisibility(
                    visible = !isDragging,
                    enter = fadeIn(tween(120)),
                    exit = fadeOut(tween(100))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (current) {
                            PlayingIndicator(color = MaterialTheme.colorScheme.primary, animate = playing)
                        } else if (!compact) {
                            Text(
                                formatDuration(song.durationMs),
                                color = playlistModernListSecondaryContentColor(),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        trailing()
                    }
                }
            }
        }
    }
}
