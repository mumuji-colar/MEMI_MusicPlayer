package com.mmc.merimusicplayer.ui

import com.mmc.merimusicplayer.ui.navigation.shouldShowMiniPlayer
import com.mmc.merimusicplayer.ui.navigation.shouldShowFloatingPlaybackChrome
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppMiniPlayerVisibilityTest {
    @Test
    fun miniPlayerIsVisibleOnlyWithSongAndCollapsedNowPlaying() {
        assertFalse(shouldShowMiniPlayer(hasSong = false, showNowPlaying = false))
        assertFalse(shouldShowMiniPlayer(hasSong = true, showNowPlaying = true))
        assertTrue(shouldShowMiniPlayer(hasSong = true, showNowPlaying = false))
    }

    @Test
    fun requestedPlaybackChromeStepsAsideForTheKeyboard() {
        assertTrue(shouldShowFloatingPlaybackChrome(requested = true, yieldsToIme = false))
        assertFalse(shouldShowFloatingPlaybackChrome(requested = true, yieldsToIme = true))
    }

    @Test
    fun keyboardDismissalDoesNotRevealChromeThatThePlaybackPageStillHides() {
        assertFalse(shouldShowFloatingPlaybackChrome(requested = false, yieldsToIme = true))
        assertFalse(shouldShowFloatingPlaybackChrome(requested = false, yieldsToIme = false))
    }
}
