package moe.ouom.neriplayer.data.model.settings.playback

enum class NowPlayingControlPlacement {
    LOWER,
    BOTTOM,
    BOTTOM_WITH_PROGRESS;

    val placesControlsAtBottom: Boolean
        get() = this != LOWER

    val placesProgressAtBottom: Boolean
        get() = this == BOTTOM_WITH_PROGRESS
}

enum class PlaybackControlSize(
    val scale: Float
) {
    SMALL(0.9f),
    MEDIUM(1f),
    LARGE(1.2f)
}

/**
 * 播放页封面边长占可用宽度的比例。
 *
 * 以前这里写死 0.60，而封面又只跟宽度挂钩——16:9 上刚好，到了 20:9 这种长条屏，
 * 宽度没变、封面也就没变，多出来的那截高度全变成了留白。
 * 所以默认档换成 [AUTO]：按窗口长宽比在 0.60～0.74 之间插值，屏幕越长封面越大。
 *
 * [widthFraction] 为 null 表示交给 [AUTO] 现算。
 */
enum class NowPlayingCoverScale(
    val widthFraction: Float?
) {
    AUTO(null),

    /** 紧凑：留给歌词/其他内容更多位置 */
    COMPACT(0.52f),

    /** 标准：差不多是 16:9 上原来的观感 */
    STANDARD(0.60f),

    /** 铺满：长条屏上让封面吃掉大部分空白 */
    LARGE(0.70f)
}

data class PlaybackControlLayoutPreferences(
    val nowPlayingPlacement: NowPlayingControlPlacement = NowPlayingControlPlacement.LOWER,
    val nowPlayingSize: PlaybackControlSize = PlaybackControlSize.MEDIUM,
    val lyricsSize: PlaybackControlSize = PlaybackControlSize.MEDIUM,
    val coverScale: NowPlayingCoverScale = NowPlayingCoverScale.AUTO
)
