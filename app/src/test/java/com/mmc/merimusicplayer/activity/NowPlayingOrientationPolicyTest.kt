package com.mmc.merimusicplayer.activity

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
    fun tabletsStayLandscapeOutsidePlayerAndRotateInsidePlayer() {
        // 平板主界面锁横屏（侧边栏、三列列表只在横屏成立）
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            NowPlayingOrientationState().requestedOrientation(600))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            NowPlayingOrientationState().requestedOrientation(800))
        // 播放页放开旋转：竖屏有上下分区版式，横屏是左右分栏，两种朝向都成立
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_FULL_USER,
            NowPlayingOrientationState(playerOpen = true).requestedOrientation(600))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_FULL_USER,
            NowPlayingOrientationState(playerOpen = true).requestedOrientation(800))
    }

    @Test
    fun tabletPlayerIgnoresPhoneOnlyPortraitRequest() {
        // 回到竖屏入口只挂在手机横屏页上；即便状态被置位，平板播放页仍保持可旋转，
        // 退出播放页后回到横屏
        val portrait = NowPlayingOrientationState().onPlayerOpenChanged(true).returnToPortrait()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_FULL_USER, portrait.requestedOrientation(1200))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            portrait.onPlayerOpenChanged(false).requestedOrientation(1200))
    }
}
