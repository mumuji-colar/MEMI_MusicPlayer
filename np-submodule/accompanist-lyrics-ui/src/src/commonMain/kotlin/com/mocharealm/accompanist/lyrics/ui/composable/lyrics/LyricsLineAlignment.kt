package com.mocharealm.accompanist.lyrics.ui.composable.lyrics

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment

/**
 * 行级水平对齐方式。
 *
 * - [Content] 保留歌词自带的对齐语义：RTL 行贴右，TTML 对唱（v1 / v2）分别贴左、贴右。
 * - [Center] 把普通行改为水平居中；**对唱行仍然保留左右分列**，
 *   否则两个人的词会叠在屏幕中间，丢掉 TTML 想表达的对话关系。
 */
enum class LyricsLineAlignment {
    Content,
    Center;

    val isCentered: Boolean get() = this == Center
}

/**
 * 一行在可用宽度内的起点系数：`0f` 贴左、`0.5f` 居中、`1f` 贴右。
 *
 * 这个值必须由同一个函数产出并被四处共同消费：
 * 1. Canvas 内每个音节的 x 坐标（见 `calculateStaticLineLayout`）
 * 2. 译文 / 音译 `Text` 的 `textAlign`
 * 3. 聚焦行缩放的 `TransformOrigin`（贴左时原点左下，居中时原点中下）
 * 4. 间奏呼吸点的位置
 *
 * 任何一处单独改成 `TextAlign.Center` 都会和另外三处错位 ——
 * 字挪到了中间、译文还靠左、放大时往右下甩。
 */
/**
 * 一行在画布里的起始 x 坐标。
 *
 * 抽成纯函数是为了能脱离 `TextLayoutResult` 直接测 —— 它在绘制阶段决定每个音节的落点，
 * 算错就是整行文字位置偏掉，值得单独锁住。
 *
 * 效果上等价于 `(canvasWidth - lineWidth) * horizontalBias`：
 * 系数 0 贴左、0.5 居中、1 贴右，任意中间值都线性插值。
 */
fun resolveLineStartX(
    canvasWidth: Float,
    lineWidth: Float,
    horizontalBias: Float
): Float = (canvasWidth - lineWidth) * horizontalBias

/**
 * 非 TTML 行（`SyncedLine`）用这个重载：没有对唱概念，只有语言方向。
 */
fun resolveLyricsLineHorizontalBias(
    lineAlignment: LyricsLineAlignment,
    isContentRightAligned: Boolean,
    isDuetLine: Boolean
): Float = when {
    // 对唱行不参与居中：v1 / v2 左右分列是 TTML 的语义，压到中间会丢掉对话关系
    lineAlignment.isCentered && !isDuetLine -> 0.5f
    isContentRightAligned -> 1f
    else -> 0f
}

/**
 * TTML / Karaoke 行：从 `KaraokeAlignment` 推出「是否对唱」和「是否贴右」。
 */
fun resolveLyricsLineHorizontalBias(
    lineAlignment: LyricsLineAlignment,
    contentAlignment: KaraokeAlignment,
    isRtl: Boolean
): Float = resolveLyricsLineHorizontalBias(
    lineAlignment = lineAlignment,
    isContentRightAligned = when (contentAlignment) {
        KaraokeAlignment.End -> !isRtl
        else -> isRtl
    },
    isDuetLine = contentAlignment != KaraokeAlignment.Unspecified
)

/**
 * 把起点系数映射成 Compose 的水平对齐枚举，供 `Column.horizontalAlignment` 使用。
 */
internal fun resolveColumnAlignment(bias: Float): androidx.compose.ui.Alignment.Horizontal = when {
    bias <= 0.25f -> androidx.compose.ui.Alignment.Start
    bias >= 0.75f -> androidx.compose.ui.Alignment.End
    else -> androidx.compose.ui.Alignment.CenterHorizontally
}
