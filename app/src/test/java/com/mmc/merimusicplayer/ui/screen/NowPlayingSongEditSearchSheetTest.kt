package com.mmc.merimusicplayer.ui.screen

import com.mmc.merimusicplayer.common.R as CoreCommonR
import com.mmc.merimusicplayer.data.model.music.MusicPlatform
import com.mmc.merimusicplayer.ui.screen.nowplaying.edit.canSearchEditSong
import com.mmc.merimusicplayer.ui.screen.nowplaying.edit.musicPlatformLabelResource
import com.mmc.merimusicplayer.ui.viewmodel.ManualSearchState
import org.junit.Assert.assertEquals
import org.junit.Test
import com.mmc.merimusicplayer.R as AppR

class NowPlayingSongEditSearchSheetTest {
    @Test
    fun `cloud search requires its session while other platforms remain available`() {
        assertEquals(true, canSearchEditSong(ManualSearchState()))
        assertEquals(false,
            canSearchEditSong(ManualSearchState(selectedPlatform = MusicPlatform.CLOUD_MUSIC))
        )
        assertEquals(
            true,
            canSearchEditSong(
                ManualSearchState(
                    selectedPlatform = MusicPlatform.CLOUD_MUSIC,
                    isCloudMusicAvailable = true
                )
            )
        )
    }

    @Test
    fun `search platform tabs keep their existing labels`() {
        assertEquals(AppR.string.platform_netease_short,
            musicPlatformLabelResource(MusicPlatform.CLOUD_MUSIC)
        )
        assertEquals(CoreCommonR.string.settings_qq_music, musicPlatformLabelResource(MusicPlatform.QQ_MUSIC))
    }
}
