package com.mmc.merimusicplayer.ui.screen.artist

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import com.mmc.merimusicplayer.data.model.youtube.music.YouTubeMusicCreatorSection
import com.mmc.merimusicplayer.data.model.youtube.music.YouTubeMusicCreatorSummary
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.ui.effect.glass.isolatedAdvancedGlassHorizontalTransition
import com.mmc.merimusicplayer.ui.screen.host.rememberHostPredictiveBackTransition
import com.mmc.merimusicplayer.ui.viewmodel.tab.YouTubeMusicPlaylist

@Composable
fun YouTubeMusicCreatorNavigationScreen(
    creator: YouTubeMusicCreatorSummary,
    onBack: () -> Unit = {},
    onSongClick: (List<SongItem>, Int) -> Unit = { _, _ -> },
    onPlaylistClick: (YouTubeMusicPlaylist) -> Unit = {},
    onCreatorClick: (YouTubeMusicCreatorSummary) -> Unit = {},
    offlineMode: Boolean = false
) {
    var selectedSection by remember(creator.browseId) {
        mutableStateOf<YouTubeMusicCreatorSection?>(null)
    }
    val stateHolder = rememberSaveableStateHolder()
    // 分区页返回时按手势进度拖动横向转场，露出底下的创作者详情
    val sectionTransition = rememberHostPredictiveBackTransition(
        targetState = selectedSection,
        backEnabled = selectedSection != null,
        backTargetState = null,
        onBack = { selectedSection = null },
        label = "youtube_creator_section"
    )

    sectionTransition.AnimatedContent(
        transitionSpec = {
            isolatedAdvancedGlassHorizontalTransition(forward = targetState != null)
                .using(SizeTransform(clip = false))
        }
    ) { section ->
        if (section != null) {
            YouTubeMusicCreatorItemsScreen(
                section = section,
                creatorName = creator.title,
                onBack = { selectedSection = null },
                onSongClick = onSongClick,
                offlineMode = offlineMode
            )
        } else {
            stateHolder.SaveableStateProvider("creator_detail") {
                YouTubeMusicCreatorDetailScreen(
                    creator = creator,
                    onBack = onBack,
                    onSongClick = onSongClick,
                    onPlaylistClick = onPlaylistClick,
                    onCreatorClick = onCreatorClick,
                    onSectionMoreClick = { selectedSection = it },
                    offlineMode = offlineMode
                )
            }
        }
    }
}
