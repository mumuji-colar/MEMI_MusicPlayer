package com.mmc.merimusicplayer.ui.component.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import com.mmc.merimusicplayer.ui.i18n.UiText
import com.mmc.merimusicplayer.common.R as CoreCommonR

class LocalSongDialogsTest {
    @Test
    fun `null local media details resolve to unavailable error`() {
        val state = resolveLocalSongDetailsLoadState(
            details = null,
            unavailableMessage = UiText.Resource(CoreCommonR.string.local_song_details_unavailable)
        )

        assertNull(state.details)
        assertEquals(UiText.Resource(CoreCommonR.string.local_song_details_unavailable), state.error)
    }
}
