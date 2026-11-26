package com.mmc.merimusicplayer.activity

import android.content.pm.ActivityInfo
import com.mmc.merimusicplayer.util.platform.PHONE_SMALLEST_SCREEN_WIDTH_DP

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
        // 平板分两段：主界面锁横屏，播放页放开旋转。
        //
        // 主界面锁横屏：侧边导航栏、三列歌曲列表都只在横屏成立，跟随系统转成竖屏
        // 会退化成手机单列版式，等于白做一套平板布局。用 SENSOR_LANDSCAPE 而不是
        // LANDSCAPE：只禁竖屏，仍允许 180° 翻转，平板倒过来拿也能正着看。
        //
        // 播放页放开：竖屏有一套专门的上下分区版式（上半唱片 + 歌词，下半 Coverflow
        // + 迷你条），横屏仍是左右分栏，两种朝向都有对应布局，没有理由再锁死。
        // 主界面不参与这条例外，退出播放页时立即回到横屏。
        smallestScreenWidthDp >= PHONE_SMALLEST_SCREEN_WIDTH_DP ->
            if (playerOpen) ActivityInfo.SCREEN_ORIENTATION_FULL_USER
            else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        // 手机：主界面竖屏；播放页放开旋转（横屏有独立版式），
        // 但从横屏播放页返回时先转回竖屏，避免主界面横着。
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
