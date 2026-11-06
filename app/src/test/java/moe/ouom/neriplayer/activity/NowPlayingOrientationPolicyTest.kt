package moe.ouom.neriplayer.activity

import android.content.pm.ActivityInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class NowPlayingOrientationPolicyTest {
    @Test
    fun phoneOutsidePlayerRemainsPortrait() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            NowPlayingOrientationState().requestedOrientation(360))
    }

    @Test
    fun openingPlayerAllowsPhoneRotationAndClosingRestoresPortrait() {
        val open = NowPlayingOrientationState().onPlayerOpenChanged(true)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_FULL_USER, open.requestedOrientation(360))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            open.onPlayerOpenChanged(false).requestedOrientation(360))
    }

    @Test
    fun returningToPortraitStaysLockedForThisOpening() {
        val portrait = NowPlayingOrientationState().onPlayerOpenChanged(true).returnToPortrait()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, portrait.requestedOrientation(360))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            portrait.onPlayerOpenChanged(true).requestedOrientation(360))
    }

    @Test
    fun reopeningPlayerAllowsRotationAgain() {
        val reopened = NowPlayingOrientationState().onPlayerOpenChanged(true).returnToPortrait()
            .onPlayerOpenChanged(false).onPlayerOpenChanged(true)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_FULL_USER, reopened.requestedOrientation(360))
    }

    @Test
    fun restoredPortraitRequestDoesNotRestartRotation() {
        val restored = NowPlayingOrientationState(playerOpen = true, portraitRequested = true)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            restored.onPlayerOpenChanged(true).requestedOrientation(360))
    }

    @Test
    fun tabletsAreLockedToLandscapeInEveryPlayerState() {
        for (state in listOf(NowPlayingOrientationState(), NowPlayingOrientationState(playerOpen = true),
            NowPlayingOrientationState(playerOpen = true, portraitRequested = true))) {
            assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, state.requestedOrientation(600))
            assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, state.requestedOrientation(800))
        }
    }

    @Test
    fun tabletsNeverFallBackToPortraitFromPhoneOnlyFlows() {
        // 回到竖屏入口只挂在手机横屏页上，即便状态被置位也不能把平板掰回竖屏
        val portrait = NowPlayingOrientationState().onPlayerOpenChanged(true).returnToPortrait()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, portrait.requestedOrientation(1200))
    }
}
