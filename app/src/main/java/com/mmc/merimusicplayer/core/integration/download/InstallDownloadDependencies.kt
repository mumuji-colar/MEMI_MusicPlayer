package com.mmc.merimusicplayer.core.integration.download

import com.mmc.merimusicplayer.core.download.host.DownloadHostBindings
import com.mmc.merimusicplayer.core.download.host.DownloadHosts

internal fun installDownloadDependencies() {
    DownloadHosts.install(
        DownloadHostBindings(
            environment = AndroidDownloadEnvironment,
            sources = AndroidDownloadServices,
            lyrics = AndroidDownloadServices,
            credentials = AndroidDownloadServices,
            playback = AndroidDownloadedPlaybackHost
        )
    )
}
