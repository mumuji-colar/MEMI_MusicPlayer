package com.mmc.merimusicplayer.core.player.testing

import android.app.Application
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import com.mmc.merimusicplayer.core.player.host.PlayerDependencies
import com.mmc.merimusicplayer.core.player.host.PlayerDownloadAccess
import com.mmc.merimusicplayer.core.player.host.PlayerEnvironment
import com.mmc.merimusicplayer.core.player.host.PlayerListenTogetherAccess
import com.mmc.merimusicplayer.core.player.host.PlayerPresentationHost
import com.mmc.merimusicplayer.core.player.host.PlayerRepositoryDependencies
import com.mmc.merimusicplayer.data.model.ltw.room.ListenTogetherRoomState
import com.mmc.merimusicplayer.data.model.ltw.session.ListenTogetherSessionState
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

internal object PlayerTestEnvironment {
    // PlayerDependencies only ever accepts one Application per process.
    val application: Application = mock(Application::class.java).also {
        `when`(it.applicationContext).thenReturn(it)
    }

    fun install(
        listenTogether: PlayerListenTogetherAccess = FakeListenTogetherAccess(),
        repositories: PlayerRepositoryDependencies = mock(PlayerRepositoryDependencies::class.java),
        downloads: PlayerDownloadAccess = mock(PlayerDownloadAccess::class.java),
        presentation: PlayerPresentationHost = mock(PlayerPresentationHost::class.java)
    ) {
        PlayerDependencies.install(
            PlayerEnvironment(
                application = application,
                repositories = repositories,
                downloads = downloads,
                listenTogether = listenTogether,
                presentation = presentation,
                isReady = { false },
                launchBackgroundIo = { Job().apply { complete() } }
            )
        )
    }

    fun reset() = install()
}

internal class FakeListenTogetherAccess : PlayerListenTogetherAccess {
    override val roomState = MutableStateFlow<ListenTogetherRoomState?>(null)
    override val sessionState = MutableStateFlow(ListenTogetherSessionState())
    val unavailableAudioLinks = mutableSetOf<Pair<String, String>>()
    var resumeRequests = 0
        private set

    override fun resumeListenerAfterSafetyPause() {
        resumeRequests += 1
    }

    override fun isControllerAudioLinkUnavailable(roomId: String, stableKey: String): Boolean =
        (roomId to stableKey) in unavailableAudioLinks
}
