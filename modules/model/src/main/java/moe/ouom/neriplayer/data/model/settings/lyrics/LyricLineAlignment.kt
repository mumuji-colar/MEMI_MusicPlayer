package moe.ouom.neriplayer.data.model.settings.lyrics

/**
 * 歌词（高级歌词/AMLL 逐字歌词）的水平对齐方式。
 *
 * 原来高级歌词的行起点写死为「贴左」，只有 RTL 语言和 TTML 对唱（v1/v2）才会贴右，
 * 在长条屏和大屏上显得偏在一边。这里把它做成可选项。
 *
 * [Follow] 是默认值，语义和改动前完全一致：普通行按语言方向贴边、对唱行左右分列。
 * 默认不改行为，老用户升级后画面不会突然变化。
 */
enum class LyricLineAlignment(val storageValue: String) {
    /** 跟随内容：普通行贴左（RTL 贴右），对唱行 v1 左 / v2 右 */
    Follow("follow"),

    /** 居中：普通行居中；对唱行仍保留左右分列，避免两句词叠在屏幕中间 */
    Center("center");

    companion object {
        val DEFAULT = Follow

        fun fromStorage(value: String?): LyricLineAlignment =
            entries.firstOrNull { it.storageValue == value } ?: DEFAULT
    }
}

/** DataStore 里 `lyric_line_alignment` 的默认值 */
const val DEFAULT_LYRIC_LINE_ALIGNMENT = "follow"
