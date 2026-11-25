package com.mmc.merimusicplayer.ui

import com.mmc.merimusicplayer.common.R as CoreCommonR
import com.mmc.merimusicplayer.core.download.GlobalDownloadManager
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.model.traffic.TrafficNetworkType
import com.mmc.merimusicplayer.ui.dialog.formatTrafficRiskDownloadMessage
import com.mmc.merimusicplayer.ui.dialog.trafficRiskNetworkLabelResource
import org.junit.Assert.assertEquals
import org.junit.Test

class AppGlobalDialogsPolicyTest {
    @Test
    fun riskMessageUsesTheSongNameForOneDownloadAndCountForBatches() {
        val song = SongItem(1, "Track", "Artist", "Album", 1, 1_000, null)
        fun request(songs: List<SongItem>) = GlobalDownloadManager.TrafficRiskDownloadRequest(
            id = 1,
            songs = songs,
            networkType = TrafficNetworkType.MOBILE,
            isBatch = songs.size > 1
        )
        fun message(songs: List<SongItem>) = formatTrafficRiskDownloadMessage(
            request(songs),
            "mobile",
            singleMessage = { network, name -> "$network:$name" },
            batchMessage = { count, network -> "$network:$count" }
        )

        assertEquals("mobile:Track", message(listOf(song)))
        assertEquals("mobile:", message(emptyList()))
        assertEquals("mobile:2", message(listOf(song, song.copy(id = 2))))
    }

    @Test
    fun trafficRiskNetworkLabelsCoverEveryTransport() {
        assertEquals(
            CoreCommonR.string.traffic_risk_network_roaming,
            trafficRiskNetworkLabelResource(TrafficNetworkType.ROAMING)
        )
        assertEquals(
            CoreCommonR.string.traffic_risk_network_mobile,
            trafficRiskNetworkLabelResource(TrafficNetworkType.MOBILE)
        )
        assertEquals(
            CoreCommonR.string.traffic_risk_network_wifi,
            trafficRiskNetworkLabelResource(TrafficNetworkType.WIFI)
        )
    }
}
