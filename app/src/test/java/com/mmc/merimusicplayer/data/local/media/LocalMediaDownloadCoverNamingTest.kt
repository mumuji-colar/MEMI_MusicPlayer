package com.mmc.merimusicplayer.data.local.media

import com.mmc.merimusicplayer.core.player.download.AudioDownloadManager
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalMediaDownloadCoverNamingTest {
    @Test
    fun `local cover sidecar naming matches downloaded cover naming`() {
        val baseName = "Artist - Song - netease"
        val stableKey = "1|netease|"

        assertEquals(
            AudioDownloadManager.buildCoverSidecarFileName(baseName, stableKey),
            LocalMediaSupport.localCoverSidecarName(
                baseName = baseName,
                extension = "jpg",
                stableIdentityKey = stableKey
            )
        )
    }
}
