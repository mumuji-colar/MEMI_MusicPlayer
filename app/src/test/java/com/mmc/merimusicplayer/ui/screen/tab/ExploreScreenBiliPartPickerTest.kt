package com.mmc.merimusicplayer.ui.screen.tab

import com.mmc.merimusicplayer.core.player.PlayerManager
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.ui.screen.tab.explore.shouldShowBiliPartsPicker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreScreenBiliPartPickerTest {

    @Test
    fun `Bilibili root and selected part tags open the parts picker`() {
        assertTrue(shouldShowBiliPartsPicker(song(PlayerManager.BILI_SOURCE_TAG)))
        assertTrue(
            shouldShowBiliPartsPicker(
                song("${PlayerManager.BILI_SOURCE_TAG}|40519994983")
            )
        )
    }

    @Test
    fun `unrelated album tag does not open the parts picker`() {
        assertFalse(shouldShowBiliPartsPicker(song("BilibiliArchive")))
    }

    private fun song(album: String): SongItem {
        return SongItem(
            id = 1L,
            name = "part",
            artist = "uploader",
            album = album,
            albumId = 0L,
            durationMs = 0L,
            coverUrl = null
        )
    }
}
