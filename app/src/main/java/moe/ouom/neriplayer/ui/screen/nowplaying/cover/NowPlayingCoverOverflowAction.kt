package moe.ouom.neriplayer.ui.screen.nowplaying.cover

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.ui.haptic.HapticIconButton
import moe.ouom.neriplayer.ui.screen.nowplaying.LocalNowPlayingLyricsBoundsTransform
import moe.ouom.neriplayer.ui.screen.nowplaying.rememberAudioDeviceInfo

/**
 * 横屏播放页「更多」里承载的动作。
 *
 * 横屏页为了减少元素数量，把收藏、评论、队列、睡眠定时、音量、歌词页、加入歌单都从界面上撤掉了。
 * 这些功能仍然必须可达，所以全部收进这个菜单，保证「少放几个图标」不等于「少掉几个功能」。
 */
internal data class NowPlayingCoverOverflowActions(
    val isFavorite: Boolean,
    val favoriteEnabled: Boolean,
    val commentAvailable: Boolean,
    val sleepTimerActive: Boolean,
    val lyricsAvailable: Boolean,
    val lyricsShowing: Boolean,
    val onFavorite: () -> Unit,
    val onComment: () -> Unit,
    val onQueue: () -> Unit,
    val onSleepTimer: () -> Unit,
    val onVolume: () -> Unit,
    val onLyrics: () -> Unit,
    val onAddToPlaylist: () -> Unit,
    val onMoreOptions: () -> Unit
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun NowPlayingCoverOverflowAction(
    buttonSize: Dp,
    iconSize: Dp,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    actions: NowPlayingCoverOverflowActions
) {
    var expanded by remember { mutableStateOf(false) }
    val favorite = nowPlayingFavoritePresentation(
        isFavorite = actions.isFavorite,
        defaultTint = LocalContentColor.current
    )
    val volume = rememberAudioDeviceInfo()
    Box {
        with(sharedTransitionScope) {
            HapticIconButton(
                onClick = { expanded = true },
                modifier = Modifier.size(buttonSize)
                    .testTag("nowPlayingOverflowButton")
                    .sharedBounds(
                        rememberSharedContentState(key = "btn_more"),
                        boundsTransform = LocalNowPlayingLyricsBoundsTransform.current,
                        animatedVisibilityScope = animatedVisibilityScope,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None
                    ).zIndex(1f)
            ) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = stringResource(CoreCommonR.string.nowplaying_more_options),
                    modifier = Modifier.size(iconSize)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.testTag("nowPlayingOverflowMenu")
        ) {
            NowPlayingCoverOverflowItem(
                tag = "overflow_favorite",
                icon = favorite.icon,
                label = stringResource(favorite.labelRes),
                tint = favorite.tint,
                enabled = actions.favoriteEnabled,
                onClick = {
                    expanded = false
                    actions.onFavorite()
                }
            )
            if (actions.commentAvailable) {
                NowPlayingCoverOverflowItem(
                    tag = "overflow_comment",
                    icon = Icons.AutoMirrored.Outlined.Comment,
                    label = stringResource(moe.ouom.neriplayer.R.string.comment_entry),
                    onClick = {
                        expanded = false
                        actions.onComment()
                    }
                )
            }
            NowPlayingCoverOverflowItem(
                tag = "overflow_queue",
                icon = Icons.AutoMirrored.Outlined.QueueMusic,
                label = stringResource(CoreCommonR.string.playlist_queue),
                onClick = {
                    expanded = false
                    actions.onQueue()
                }
            )
            NowPlayingCoverOverflowItem(
                tag = "overflow_timer",
                icon = Icons.Outlined.Timer,
                label = stringResource(CoreCommonR.string.sleep_timer_short),
                tint = if (actions.sleepTimerActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    LocalContentColor.current
                },
                onClick = {
                    expanded = false
                    actions.onSleepTimer()
                }
            )
            NowPlayingCoverOverflowItem(
                tag = "overflow_volume",
                icon = volume.second,
                label = volume.first,
                onClick = {
                    expanded = false
                    actions.onVolume()
                }
            )
            NowPlayingCoverOverflowItem(
                tag = "overflow_lyrics",
                icon = nowPlayingLyricsToolbarIcon(lyricsAdjustBehavior = false),
                label = stringResource(
                    nowPlayingLyricsToolbarDescription(lyricsAdjustBehavior = false)
                ),
                enabled = actions.lyricsAvailable,
                tint = if (actions.lyricsShowing) {
                    MaterialTheme.colorScheme.primary
                } else {
                    LocalContentColor.current
                },
                onClick = {
                    expanded = false
                    actions.onLyrics()
                }
            )
            NowPlayingCoverOverflowItem(
                tag = "overflow_add",
                icon = Icons.AutoMirrored.Outlined.PlaylistAdd,
                label = stringResource(CoreCommonR.string.playlist_add_to),
                onClick = {
                    expanded = false
                    actions.onAddToPlaylist()
                }
            )
            NowPlayingCoverOverflowItem(
                tag = "overflow_more",
                icon = Icons.Outlined.Tune,
                label = stringResource(CoreCommonR.string.nowplaying_more_options),
                onClick = {
                    expanded = false
                    actions.onMoreOptions()
                }
            )
        }
    }
}

@Composable
private fun NowPlayingCoverOverflowItem(
    tag: String,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = LocalContentColor.current
) {
    DropdownMenuItem(
        modifier = Modifier.testTag(tag),
        text = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        },
        enabled = enabled,
        onClick = onClick
    )
}
