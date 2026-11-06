package moe.ouom.neriplayer.ui.component.lyrics

import androidx.compose.runtime.staticCompositionLocalOf
import moe.ouom.neriplayer.data.settings.AutoSettingsSchema

/**
 * 歌词点击「先高亮、再点才跳」的偏好。
 *
 * 状态机很小，但散落在两套渲染器里（播放页的 SyncedLyricsView 和子模块的
 * KaraokeLyricsView），所以判定逻辑抽成纯函数，两边共用同一套语义：
 *
 * - 开关关闭：点一下直接跳（原来的行为）
 * - 开关开启：第一次点某行只高亮，第二次点同一行才跳转
 */
val LocalLyricClickConfirmEnabled = staticCompositionLocalOf {
    AutoSettingsSchema.lyrics.lyricClickConfirmEnabled.defaultValue
}

/** 高亮持续多久没人点第二次就自动熄灭 */
const val LYRIC_SELECTION_CONFIRM_TIMEOUT_MS = 4_000L

internal enum class LyricTapOutcome {
    /** 开关关闭，点击即跳 */
    SeekImmediately,

    /** 第一次点击，只把这一行标记为选中 */
    HighlightOnly,

    /** 第二次点击同一行，执行跳转 */
    Commit
}

internal fun resolveLyricTapOutcome(
    confirmEnabled: Boolean,
    pendingSelectionIndex: Int?,
    tappedIndex: Int
): LyricTapOutcome = when {
    !confirmEnabled -> LyricTapOutcome.SeekImmediately
    pendingSelectionIndex == tappedIndex -> LyricTapOutcome.Commit
    else -> LyricTapOutcome.HighlightOnly
}
