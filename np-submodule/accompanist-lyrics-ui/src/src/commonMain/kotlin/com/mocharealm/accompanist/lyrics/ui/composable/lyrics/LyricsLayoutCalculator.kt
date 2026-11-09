package com.mocharealm.accompanist.lyrics.ui.composable.lyrics

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.ui.utils.isArabic
import com.mocharealm.accompanist.lyrics.ui.utils.isCjk
import com.mocharealm.accompanist.lyrics.ui.utils.isJapanese
import com.mocharealm.accompanist.lyrics.ui.utils.isKorean
import com.mocharealm.accompanist.lyrics.ui.utils.isDevanagari
import com.mocharealm.accompanist.lyrics.ui.utils.isPunctuation
import com.mocharealm.accompanist.lyrics.ui.utils.isPureCjk
import kotlin.math.pow

@Stable
data class SyllableLayout(
    val syllable: KaraokeSyllable,
    val textLayoutResult: TextLayoutResult,
    val wordId: Int,
    val useAwesomeAnimation: Boolean,
    val width: Float = textLayoutResult.size.width.toFloat(),
    val position: Offset = Offset.Zero,
    val wordPivot: Offset = Offset.Zero,
    val wordAnimInfo: WordAnimationInfo? = null,
    val charOffsetInWord: Int = 0,
    val charLayouts: List<TextLayoutResult>? = null,
    val charOriginalBounds: List<Rect>? = null,
    val firstBaseline: Float = 0f,
    val phonetic: String? = null,
    val phoneticLayoutResult: TextLayoutResult? = null,
)

@Stable
data class WordAnimationInfo(
    val wordStartTime: Long,
    val wordEndTime: Long,
    val wordContent: String,
    val wordDuration: Long = wordEndTime - wordStartTime
)

@Stable
data class WrappedLine(
    val syllables: List<SyllableLayout>, val totalWidth: Float
)

@Stable
data class RowRenderData(
    val rowLayouts: List<SyllableLayout>,
    val totalMinX: Float,
    val totalMaxX: Float,
    val totalWidth: Float,
    val firstSyllableStart: Int,
    val lastSyllableEnd: Int,
    val layerBounds: Rect
)

fun calculateRowRenderData(
    lineLayouts: List<List<SyllableLayout>>,
    showPhonetic: Boolean,
    density: Float,
    edgePaddingDp: Float = 8f
): List<RowRenderData> {
    return lineLayouts.mapNotNull { rowLayouts ->
        if (rowLayouts.isEmpty()) return@mapNotNull null

        val totalMinX = rowLayouts.minOf { it.position.x }
        val totalMaxX = rowLayouts.maxOf { it.position.x + it.width }
        val totalWidth = totalMaxX - totalMinX
        val minY = rowLayouts.minOf { it.position.y }
        val totalHeight = rowLayouts.maxOf { it.textLayoutResult.size.height }.toFloat()
        val maxPhoneticHeight = if (showPhonetic) {
            rowLayouts.maxOfOrNull { it.phoneticLayoutResult?.size?.height ?: 0 }?.toFloat() ?: 0f
        } else {
            0f
        }

        val verticalPadding = (totalHeight * 0.1f) * density
        val horizontalPadding = ((totalMaxX - totalMinX) * 0.2f) * density
        val edgePaddingPx = edgePaddingDp * density

        RowRenderData(
            rowLayouts = rowLayouts,
            totalMinX = totalMinX,
            totalMaxX = totalMaxX,
            totalWidth = totalWidth,
            firstSyllableStart = rowLayouts.first().syllable.start,
            lastSyllableEnd = rowLayouts.last().syllable.end,
            layerBounds = Rect(
                left = totalMinX - horizontalPadding,
                top = minY - verticalPadding - maxPhoneticHeight - edgePaddingPx,
                right = totalMaxX + horizontalPadding,
                bottom = minY + totalHeight + verticalPadding + edgePaddingPx
            )
        )
    }
}

fun String.shouldUseSimpleAnimation(): Boolean {
    val cleanedStr = this.filter { !it.isWhitespace() && !it.toString().isPunctuation() }
    if (cleanedStr.isEmpty()) return false
    return cleanedStr.isPureCjk() || cleanedStr.any { it.isArabic() || it.isDevanagari() }
}

fun groupIntoWords(syllables: List<KaraokeSyllable>): List<List<KaraokeSyllable>> {
    if (syllables.isEmpty()) return emptyList()
    val words = mutableListOf<List<KaraokeSyllable>>()
    var currentWord = mutableListOf<KaraokeSyllable>()
    syllables.forEach { syllable ->
        currentWord.add(syllable)
        if (syllable.content.trimEnd().length < syllable.content.length) {
            words.add(currentWord.toList())
            currentWord = mutableListOf()
        }
    }
    if (currentWord.isNotEmpty()) {
        words.add(currentWord.toList())
    }
    return words
}

fun measureSyllablesAndDetermineAnimation(
    syllables: List<KaraokeSyllable>,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    phoneticStyle: TextStyle,
    isAccompanimentLine: Boolean,
    spaceWidth: Float
): List<SyllableLayout> {
    val words = groupIntoWords(syllables)
    val fastCharAnimationThresholdMs = 200f

    return words.flatMapIndexed { wordIndex, word ->
        val wordContent = word.joinToString("") { it.content }
        val wordDuration = if (word.isNotEmpty()) word.last().end - word.first().start else 0
        val perCharDuration = if (wordContent.isNotEmpty() && wordDuration > 0) {
            wordDuration.toFloat() / wordContent.length
        } else {
            0f
        }

        val useAwesomeAnimation =
            perCharDuration > fastCharAnimationThresholdMs && wordDuration >= 1000
                    && !wordContent.shouldUseSimpleAnimation()
                    && !isAccompanimentLine

        word.map { syllable ->
            val layoutResult = textMeasurer.measure(syllable.content, style)
            val phoneticLayout = if (!syllable.phonetic.isNullOrBlank()) {
                textMeasurer.measure(syllable.phonetic!!, phoneticStyle)
            } else null

            // --- Fix: 修正尾部空格宽度丢失 ---
            var layoutWidth = layoutResult.size.width.toFloat()
            if (syllable.content.endsWith(" ")) {
                val trimmedWidth = textMeasurer.measure(syllable.content.trimEnd(), style).size.width.toFloat()
                if (layoutWidth <= trimmedWidth) {
                    val spaceCount = syllable.content.length - syllable.content.trimEnd().length
                    layoutWidth = trimmedWidth + (spaceWidth * spaceCount)
                }
            }
            // -----------------------------

            // Ensure width is at least phonetic width
            val finalWidth = maxOf(layoutWidth, phoneticLayout?.size?.width?.toFloat() ?: 0f)

            // 新增：如果需要高级动画，预先测量每个字符
            val (charLayouts, charBounds) = if (useAwesomeAnimation) {
                val layouts = syllable.content.map { char ->
                    textMeasurer.measure(char.toString(), style)
                }
                val bounds = syllable.content.indices.map { index ->
                    layoutResult.getBoundingBox(index)
                }
                layouts to bounds
            } else {
                null to null
            }

            SyllableLayout(
                syllable = syllable,
                textLayoutResult = layoutResult,
                wordId = wordIndex,
                useAwesomeAnimation = useAwesomeAnimation,
                width = finalWidth, 
                charLayouts = charLayouts,      // 存入缓存
                charOriginalBounds = charBounds,
                firstBaseline = layoutResult.firstBaseline,
                phonetic = syllable.phonetic,
                phoneticLayoutResult = phoneticLayout
            )
        }
    }
}

fun calculateGreedyWrappedLines(
    syllableLayouts: List<SyllableLayout>,
    availableWidthPx: Float,
    textMeasurer: TextMeasurer,
    style: TextStyle
): List<WrappedLine> {
    val lines = mutableListOf<WrappedLine>()
    val currentLine = mutableListOf<SyllableLayout>()
    var currentLineWidth = 0f

    val wordGroups = mutableListOf<List<SyllableLayout>>()
    if (syllableLayouts.isNotEmpty()) {
        var currentWordGroup = mutableListOf<SyllableLayout>()
        var currentWordId = syllableLayouts.first().wordId

        syllableLayouts.forEach { layout ->
            if (layout.wordId != currentWordId) {
                wordGroups.add(currentWordGroup)
                currentWordGroup = mutableListOf()
                currentWordId = layout.wordId
            }
            currentWordGroup.add(layout)
        }
        wordGroups.add(currentWordGroup)
    }

    // 按单词进行排版
    wordGroups.forEach { wordSyllables ->
        val wordWidth = wordSyllables.sumOf { it.width.toDouble() }.toFloat()

        // 如果当前行能放下整个单词
        if (currentLineWidth + wordWidth <= availableWidthPx) {
            currentLine.addAll(wordSyllables)
            currentLineWidth += wordWidth
        } else {
            // 放不下，先换行（如果当前行不是空的）
            if (currentLine.isNotEmpty()) {
                val trimmedDisplayLine = trimDisplayLineTrailingSpaces(currentLine, textMeasurer, style)
                if (trimmedDisplayLine.syllables.isNotEmpty()) {
                    lines.add(trimmedDisplayLine)
                }
                currentLine.clear()
                currentLineWidth = 0f
            }

            // 换行后，检查新行能不能放下这个单词
            if (wordWidth <= availableWidthPx) {
                // 能放下，直接加入
                currentLine.addAll(wordSyllables)
                currentLineWidth += wordWidth
            } else {
                // 特殊情况: 单个单词比一整行还宽（拉丁语系的长复合词，或没有任何标点的短句）。
                // 此时必须破坏单词完整性，退化回按音节换行。
                // 注意：中文歌词现在由动态规划直接处理，正常不会再走到这里，
                // 这条分支只作为「单音节也放不下」时的兜底，保证一定有解。
                wordSyllables.forEach { syllable ->
                    if (currentLineWidth + syllable.width > availableWidthPx && currentLine.isNotEmpty()) {
                        // 避头点：若换行会把标点顶到行首，就把上一行的末字一起挪到下一行。
                        // 宁可让上一行短一个字，也不让标点孤零零地起头。
                        val detached = detachTrailingLineStartForbidden(currentLine, syllable)
                        val trimmedLine = trimDisplayLineTrailingSpaces(currentLine, textMeasurer, style)
                        if (trimmedLine.syllables.isNotEmpty()) lines.add(trimmedLine)
                        currentLine.clear()
                        currentLineWidth = 0f
                        currentLine.addAll(detached)
                        currentLineWidth += detached.sumOf { it.width.toDouble() }.toFloat()
                    }
                    currentLine.add(syllable)
                    currentLineWidth += syllable.width
                }
            }
        }
    }

    // 处理最后一行
    if (currentLine.isNotEmpty()) {
        val trimmedDisplayLine = trimDisplayLineTrailingSpaces(currentLine, textMeasurer, style)
        if (trimmedDisplayLine.syllables.isNotEmpty()) {
            lines.add(trimmedDisplayLine)
        }
    }
    return lines
}

/**
 * 相邻两个音节之间是否允许断行。
 *
 * 判据完全交给 [LyricsLineBreaking.canBreakBetween]，这里只做拆包。
 */
internal fun canBreakBetween(previous: SyllableLayout, next: SyllableLayout): Boolean =
    LyricsLineBreaking.canBreakBetween(
        prevWordId = previous.wordId,
        prevContent = previous.syllable.content,
        nextWordId = next.wordId,
        nextContent = next.syllable.content
    )

/**
 * 断在中文词边界上的成本折扣，让词与词之间比随便断便宜一点。
 *
 * 调参见 `docs/lyrics-line-breaking.md` 的「还能怎么调」。
 */
internal const val CJK_WORD_BOUNDARY_BREAK_SCALE = 1.0f

/**
 * 中文词内断行的固定惩罚，单位是「平均字宽的平方」。
 *
 * 为什么不乘在 `breakScale` 上：DP 的代价是 `剩余宽度² × 系数`，
 * 行几乎填满时剩余宽度趋近 0，任何乘数都会被抹平——恰恰这时最容易被拆词。
 * 所以改成与剩余宽度无关的加项。
 *
 * 为什么不用「可用宽度²」：那样在大屏上惩罚会大到离谱（宽度翻倍，惩罚翻四倍），
 * 算法宁可把行拉得极不均衡也要躲开拆词；以平均字宽为单位才是尺度无关的。
 * 2.5 的含义：拆一个词大约等于「宁可多空出 2.5 个字」的代价。
 *
 * 调参见 `docs/lyrics-line-breaking.md` 的「还能怎么调」：症状对照表 + 建议取值范围 + 改完怎么验。
 */
internal const val CJK_WORD_SPLIT_PENALTY_SYLLABLES = 5.0f

/** 汉字（不含假名、谚文与中文标点）：只有它们才需要「别把词拆开」这条约束。 */
private fun Char.isHanLetter(): Boolean =
    isCjk() && !isJapanese() && !isKorean() && isLetter()

/**
 * 这个断点是不是「两侧都是汉字、而且落在词内部」——也就是会拆开一个中文词。
 *
 * 拉丁单词已由 [LyricsLineBreaking.canBreakBetween] 整词禁断，中文标点已由避头/避尾处理，
 * 所以这里只认汉字，避免把同一套惩罚叠加到别处。
 */
internal fun isIntraHanWordBreak(
    prevContent: String,
    nextContent: String,
    breakIsWordBoundary: Boolean
): Boolean {
    if (breakIsWordBoundary) return false
    val previous = prevContent.lastVisibleChar() ?: return false
    val next = nextContent.firstVisibleChar() ?: return false
    return previous.isHanLetter() && next.isHanLetter()
}

/**
 * 带中文词边界的断行成本系数。
 *
 * 只在「词边界 + 两侧汉字」时给一点折扣；拆词的代价不走系数，走 [isIntraHanWordBreak] 的固定惩罚。
 */
internal fun wordAwareBreakScale(
    prevWordId: Int,
    prevContent: String,
    nextWordId: Int,
    nextContent: String,
    breakIsWordBoundary: Boolean
): Float {
    val scale = LyricsLineBreaking.breakScale(prevWordId, prevContent, nextWordId, nextContent)
    val previous = prevContent.lastVisibleChar()
    val next = nextContent.firstVisibleChar()
    if (breakIsWordBoundary && previous != null && next != null &&
        previous.isHanLetter() && next.isHanLetter()
    ) {
        return scale * CJK_WORD_BOUNDARY_BREAK_SCALE
    }
    return scale
}

/**
 * [breakIndex] 这个断点的成本系数，越小越倾向于在这里断行。
 *
 * [breakIndex] 为 0 表示断点落在行首，没有上一行，返回中性值 1。
 */
internal fun breakScaleAt(syllables: List<SyllableLayout>, breakIndex: Int): Float {
    if (breakIndex <= 0) return 1f
    val previous = syllables[breakIndex - 1]
    val next = syllables[breakIndex]
    return LyricsLineBreaking.breakScale(
        prevWordId = previous.wordId,
        prevContent = previous.syllable.content,
        nextWordId = next.wordId,
        nextContent = next.syllable.content
    )
}

/**
 * 贪心兜底用的避头点修正。
 *
 * [pendingSyllable] 是被挤到下一行的音节。如果它本身是避头点，就把 [line] 末尾的
 * 几个音节摘下来一起带走，直到新行首不再是避头点、或本行已被摘空。
 * 返回被摘下的音节（按原顺序），调用方负责把它们放到下一行行首。
 *
 * 摘几个字的判断交给 [LyricsLineBreaking.headAvoidingDetachCount]，这里只负责搬运。
 */
internal fun detachTrailingLineStartForbidden(
    line: MutableList<SyllableLayout>,
    pendingSyllable: SyllableLayout
): List<SyllableLayout> {
    val detachCount = LyricsLineBreaking.headAvoidingDetachCount(
        lineContents = line.map { it.syllable.content },
        nextContent = pendingSyllable.syllable.content
    )
    if (detachCount <= 0) return emptyList()

    val startIndex = (line.size - detachCount).coerceAtLeast(0)
    val detached = line.subList(startIndex, line.size).toList()
    repeat(line.size - startIndex) { line.removeAt(line.lastIndex) }
    return detached
}

/**
 * 均衡断行的动态规划内核，与 Compose 的测量完全解耦。
 *
 * 目标是让每一行的右侧余量都尽量小（余量的平方和最小），这样各行长度看起来才整齐。
 *
 * @param widths 每个音节的宽度（像素）。
 * @param breakAllowed 下标 `k` 处是否允许断行（`k` 为 0 时表示行首，恒为 true）。
 *                   用于禁止把拉丁单词劈开。
 * @param breakScale 下标 `k` 处断行的成本系数，越小越倾向在这里断。
 *                   用于实现避头点 / 避尾点 / 句读优先 / 词内断行略贵。
 * @return 每个断点的起始下标（长度 n，`result[i]` 是第 i 行起点）；
 *         若约束太紧导致无解则返回 null，由调用方退化为贪心。
 */
internal fun computeBalancedBreakStarts(
    widths: FloatArray,
    availableWidthPx: Float,
    breakAllowed: (Int) -> Boolean,
    breakScale: (Int) -> Float,
    breakPenalty: (Int) -> Float = { 0f }
): IntArray? {
    val n = widths.size
    if (n == 0) return IntArray(0)
    if (availableWidthPx <= 0f) return null

    val costs = DoubleArray(n + 1) { Double.POSITIVE_INFINITY }
    val breaks = IntArray(n + 1)
    costs[0] = 0.0

    for (i in 1..n) {
        var currentLineWidth = 0f
        for (j in i downTo 1) {
            // 行首到 j-1 之间若不允许断开，就只能继续把这一行的左边界往左扩，
            // 借此把「不能断」的音节强行留在同一行。
            if (j > 1 && !breakAllowed(j - 1)) {
                currentLineWidth += widths[j - 1]
                if (currentLineWidth > availableWidthPx) break
                continue
            }

            currentLineWidth += widths[j - 1]

            if (currentLineWidth > availableWidthPx) break

            val lineBadness = (availableWidthPx - currentLineWidth).pow(2).toDouble()
            val badness = lineBadness * breakScale(j - 1) + breakPenalty(j - 1)

            if (costs[j - 1] != Double.POSITIVE_INFINITY && costs[j - 1] + badness < costs[i]) {
                costs[i] = costs[j - 1] + badness
                breaks[i] = j - 1
            }
        }
    }

    if (costs[n] == Double.POSITIVE_INFINITY) return null

    val starts = IntArray(n)
    var lineCount = 0
    var currentIndex = n
    while (currentIndex > 0) {
        starts[lineCount++] = breaks[currentIndex]
        currentIndex = breaks[currentIndex]
    }
    return starts.copyOf(lineCount).reversedArray()
}

fun calculateBalancedLines(
    syllableLayouts: List<SyllableLayout>,
    availableWidthPx: Float,
    textMeasurer: TextMeasurer,
    style: TextStyle
): List<WrappedLine> {
    if (syllableLayouts.isEmpty()) return emptyList()

    val widths = FloatArray(syllableLayouts.size) { syllableLayouts[it].width }
    // 拆词惩罚的长度单位：用平均字宽而不是可用宽度，保证小屏大屏的惩罚力度一致
    val averageSyllableWidth = widths.average().toFloat().takeIf { it > 0f } ?: 1f
    val contents = syllableLayouts.map { it.syllable.content }

    // 每个音节在整行文本里的起始偏移，用来把「音节断点」换算成「字符偏移」去查词边界
    val textStarts = IntArray(contents.size + 1)
    var textOffset = 0
    contents.forEachIndexed { index, content ->
        textStarts[index] = textOffset
        textOffset += content.length
    }
    textStarts[contents.size] = textOffset
    val wordBoundaries = CjkWordSegmentation.boundaries(contents.joinToString(""))

    val startIndexes = computeBalancedBreakStarts(
        widths = widths,
        availableWidthPx = availableWidthPx,
        breakAllowed = { breakIndex ->
            breakIndex <= 0 || canBreakBetween(
                syllableLayouts[breakIndex - 1], syllableLayouts[breakIndex]
            )
        },
        breakScale = { breakIndex ->
            if (breakIndex <= 0) {
                1f
            } else {
                wordAwareBreakScale(
                    prevWordId = syllableLayouts[breakIndex - 1].wordId,
                    prevContent = contents[breakIndex - 1],
                    nextWordId = syllableLayouts[breakIndex].wordId,
                    nextContent = contents[breakIndex],
                    breakIsWordBoundary = textStarts[breakIndex] in wordBoundaries
                )
            }
        },
        breakPenalty = { breakIndex ->
            if (breakIndex <= 0) {
                0f
            } else if (
                isIntraHanWordBreak(
                    prevContent = contents[breakIndex - 1],
                    nextContent = contents[breakIndex],
                    breakIsWordBoundary = textStarts[breakIndex] in wordBoundaries
                )
            ) {
                averageSyllableWidth * averageSyllableWidth * CJK_WORD_SPLIT_PENALTY_SYLLABLES
            } else {
                0f
            }
        }
    ) ?: return calculateGreedyWrappedLines(syllableLayouts, availableWidthPx, textMeasurer, style)

    val lines = mutableListOf<WrappedLine>()
    startIndexes.forEachIndexed { lineIndex, startIndex ->
        val endIndex = startIndexes.getOrNull(lineIndex + 1) ?: syllableLayouts.size
        val lineSyllables = syllableLayouts.subList(startIndex, endIndex)
        lines.add(trimDisplayLineTrailingSpaces(lineSyllables, textMeasurer, style))
    }

    return lines
}

/**
 * 修复版：移除 Alignment 依赖，使用布尔值控制布局起始点，解决 RTL 居中问题
 */
fun calculateStaticLineLayout(
    wrappedLines: List<WrappedLine>,
    /**
     * 行的起点系数：0f 贴左、0.5f 居中、1f 贴右。
     * 用连续值而不是 Boolean，是为了让「居中」不必在图层的每一处再加一个分支。
     */
    horizontalBias: Float,
    canvasWidth: Float,
    lineHeight: Float,
    phoneticHeight: Float,
    isRtl: Boolean
): List<List<SyllableLayout>> {
    val layoutsByWord = mutableMapOf<Int, MutableList<SyllableLayout>>()

    val hasPhoneticInBlock = wrappedLines.any { wl -> wl.syllables.any { it.phoneticLayoutResult != null } }

    val positionedLines = wrappedLines.mapIndexed { lineIndex, wrappedLine ->
        val maxBaselineInLine = wrappedLine.syllables.maxOfOrNull { it.firstBaseline } ?: 0f
        
        val rowHeight = lineHeight + if (hasPhoneticInBlock) (phoneticHeight * 0.7f) else 0f
        val rowTopY = lineIndex * rowHeight + if (hasPhoneticInBlock) (phoneticHeight * 0.7f) else 0f

        // 起点系数：贴左时 0，居中时两侧均分剩余宽度，贴右时整行右移
        val startX = resolveLineStartX(
            canvasWidth = canvasWidth,
            lineWidth = wrappedLine.totalWidth,
            horizontalBias = horizontalBias
        )

        // 下面的逻辑处理 RTL 单词内的排列，保持不变
        var currentX = if (isRtl) startX + wrappedLine.totalWidth else startX

        wrappedLine.syllables.map { initialLayout ->
            val positionX = if (isRtl) {
                currentX - initialLayout.width
            } else {
                currentX
            }
            val verticalOffset = maxBaselineInLine - initialLayout.firstBaseline
            val positionY = rowTopY + verticalOffset
            val positionedLayout = initialLayout.copy(position = Offset(positionX, positionY))
            layoutsByWord.getOrPut(positionedLayout.wordId) { mutableListOf() }
                .add(positionedLayout)

            if (isRtl) {
                currentX -= positionedLayout.width
            } else {
                currentX += positionedLayout.width
            }

            positionedLayout
        }
    }

    val animInfoByWord = mutableMapOf<Int, WordAnimationInfo>()
    val charOffsetsBySyllable = mutableMapOf<SyllableLayout, Int>()

    layoutsByWord.forEach { (wordId, layouts) ->
        if (layouts.first().useAwesomeAnimation) {
            animInfoByWord[wordId] = WordAnimationInfo(
                wordStartTime = layouts.minOf { it.syllable.start }.toLong(),
                wordEndTime = layouts.maxOf { it.syllable.end }.toLong(),
                wordContent = layouts.joinToString("") { it.syllable.content })
            var runningCharOffset = 0
            layouts.forEach { layout ->
                charOffsetsBySyllable[layout] = runningCharOffset
                runningCharOffset += layout.syllable.content.length
            }
        }
    }

    return positionedLines.map { line ->
        line.map { positionedLayout ->
            val wordLayouts = layoutsByWord.getValue(positionedLayout.wordId)
            val minX = wordLayouts.minOf { it.position.x }
            val maxX = wordLayouts.maxOf { it.position.x + it.width }
            val bottomY = wordLayouts.maxOf { it.position.y + it.textLayoutResult.size.height }

            positionedLayout.copy(
                wordPivot = Offset(x = (minX + maxX) / 2f, y = bottomY),
                wordAnimInfo = animInfoByWord[positionedLayout.wordId],
                charOffsetInWord = charOffsetsBySyllable[positionedLayout] ?: 0
            )
        }
    }
}

fun trimDisplayLineTrailingSpaces(
    displayLineSyllables: List<SyllableLayout>, textMeasurer: TextMeasurer, style: TextStyle
): WrappedLine {
    if (displayLineSyllables.isEmpty()) {
        return WrappedLine(emptyList(), 0f)
    }

    val processedSyllables = displayLineSyllables.toMutableList()
    var lastIndex = processedSyllables.lastIndex

    while (lastIndex >= 0 && processedSyllables[lastIndex].syllable.content.isBlank()) {
        processedSyllables.removeAt(lastIndex)
        lastIndex--
    }

    if (processedSyllables.isEmpty()) {
        return WrappedLine(emptyList(), 0f)
    }

    val lastLayout = processedSyllables.last()
    val originalContent = lastLayout.syllable.content
    val trimmedContent = originalContent.trimEnd()

    if (trimmedContent.length < originalContent.length) {
        if (trimmedContent.isNotEmpty()) {
            val trimmedLayoutResult = textMeasurer.measure(trimmedContent, style)
            val trimmedLayout = lastLayout.copy(
                syllable = lastLayout.syllable.copy(content = trimmedContent),
                textLayoutResult = trimmedLayoutResult,
                width = trimmedLayoutResult.size.width.toFloat()
            )
            processedSyllables[processedSyllables.lastIndex] = trimmedLayout
        } else {
            processedSyllables.removeAt(processedSyllables.lastIndex)
        }
    }

    val totalWidth = processedSyllables.sumOf { it.width.toDouble() }.toFloat()
    return WrappedLine(processedSyllables, totalWidth)
}
