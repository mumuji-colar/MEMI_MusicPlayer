package moe.ouom.neriplayer.activity

import android.content.pm.ActivityInfo
import moe.ouom.neriplayer.util.platform.PHONE_SMALLEST_SCREEN_WIDTH_DP

internal data class NowPlayingOrientationState(
    val playerOpen: Boolean = false,
    val portraitRequested: Boolean = false
) {
    fun onPlayerOpenChanged(open: Boolean): NowPlayingOrientationState = copy(
        playerOpen = open,
        portraitRequested = portraitRequested && playerOpen && open
    )

    fun returnToPortrait(): NowPlayingOrientationState = copy(portraitRequested = playerOpen)

    fun requestedOrientation(smallestScreenWidthDp: Int): Int = when {
        // 平板整机锁定横屏：播放页左右分栏、侧边导航栏、三列歌曲列表都只在横屏成立，
        // 跟随系统转成竖屏会退化成手机单列版式，等于白做一套平板布局。
        // 用 SENSOR_LANDSCAPE 而不是 LANDSCAPE：只禁竖屏，仍允许 180° 翻转，
        // 平板倒过来拿（接口朝左/朝右）也能正着看。
        smallestScreenWidthDp >= PHONE_SMALLEST_SCREEN_WIDTH_DP ->
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        playerOpen && !portraitRequested -> ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}

internal fun shouldHideTabletNavigationBar(
    smallestScreenWidthDp: Int,
    isLandscape: Boolean,
    safeModeActive: Boolean
): Boolean = smallestScreenWidthDp >= PHONE_SMALLEST_SCREEN_WIDTH_DP && isLandscape && !safeModeActive

/**
 * 手机横屏播放页是否隐藏状态栏（沉浸式）。
 *
 * 横屏的状态栏依然横在屏幕顶端，会吃掉 24~32dp 的整条高度，
 * 把封面、歌词、播放控件整体往下挤；而横屏的时间/电量信息本身价值不高，
 * 所以播放页横屏时改为隐藏，允许下滑临时唤出。
 *
 * 竖屏不隐藏：状态栏正好落在摄像头挖孔那一侧，隐藏反而会让内容顶到挖孔下面。
 * 平板横屏也不隐藏：既有的左右分栏版式没有因此被抱怨，先不动。
 */
internal fun shouldHideNowPlayingStatusBar(
    playerOpen: Boolean,
    isLandscape: Boolean,
    smallestScreenWidthDp: Int,
    safeModeActive: Boolean
): Boolean = playerOpen && isLandscape &&
    smallestScreenWidthDp < PHONE_SMALLEST_SCREEN_WIDTH_DP && !safeModeActive
