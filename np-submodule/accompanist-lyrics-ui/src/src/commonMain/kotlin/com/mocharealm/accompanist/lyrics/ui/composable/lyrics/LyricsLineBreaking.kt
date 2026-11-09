package com.mocharealm.accompanist.lyrics.ui.composable.lyrics

import com.mocharealm.accompanist.lyrics.ui.utils.isCjk
import com.mocharealm.accompanist.lyrics.ui.utils.isJapanese
import com.mocharealm.accompanist.lyrics.ui.utils.isKorean
import com.mocharealm.accompanist.lyrics.ui.utils.isPunctuation

/**
 * 歌词断行规则。
 *
 * 背景：原来的 [groupIntoWords] 只在「音节以空格结尾」时切词，所以一整句中文会被
 * 当成一个超长单词，行内不允许断开；宽度装不下时只能退化成逐字硬塞，
 * 于是出现「各行字数不均衡」「行首顶着一个逗号」这类难看的断句。
 *
 * 这里把断行判据抽成独立的纯函数，好处有两个：
 * 一是行为可以被单元测试逐条锁住，二是动态规划只消费成本系数，不关心渲染细节。
 *
 * 三条规则：
 * 1. 避头点（。，、！？……）不允许出现在行首；
 * 2. 避尾点（（「【……）不允许出现在行尾；
 * 3. 句读标点之后换行更符合阅读习惯，因此给一点成本折扣。
 *
 * 所有偏好都用「成本系数」表达而不是硬性禁止：
 * 系数越小越倾向于在这里断行。这样即使容器窄到极端，动态规划也总能找到解，
 * 不会因为某处被禁止而整体退化回逐字硬塞。
 */
internal object LyricsLineBreaking {

    /**
     * 避头点：不允许出现在行首。
     *
     * 收尾类标点（句号、逗号、右括号、右书名号等）顶在行首会显得突兀，
     * 排版上必须把它们拽回上一行。
     */
    private val lineStartForbidden =
        "。，、；：！？…‥·．）〉》」』】〗〕｝!?;:,.)]}>»".toSet()

    /**
     * 避尾点：不允许出现在行尾。
     *
     * 开启类标点（左括号、左书名号等）后面必须紧跟内容，
     * 孤零零留在行尾会让读者误以为后面没有下文。
     */
    private val lineEndForbidden = "（〈《「『【〖〔｛([{<«“‘〝".toSet()

    /** 句读标点：在这些字符之后换行更自然。 */
    private val preferredBreakAfter = "。！？；：，、…—!?;:,".toSet()

    /** 行首出现避头点时的成本系数。取值足够大以强烈规避，但有限以免把解逼到更差的一侧。 */
    const val FORBIDDEN_AT_LINE_START_SCALE = 6f

    /** 行尾出现避尾点时的成本系数。 */
    const val FORBIDDEN_AT_LINE_END_SCALE = 6f

    /** 在句读标点之后断行的成本折扣。 */
    const val PUNCTUATION_BREAK_SCALE = 0.75f

    /** 在中文音节内部断行的成本，比词边界略贵，好让词边界优先被选中。 */
    const val INTRA_WORD_BREAK_SCALE = 1.15f

    /** 去掉首部空白后的第一个字符；全是空白时返回 null。 */
    private fun String.firstVisible(): Char? = trimStart().firstOrNull()

    /** 去掉尾部空白后的最后一个字符；全是空白时返回 null。 */
    private fun String.lastVisible(): Char? = trimEnd().lastOrNull()

    /** 断点右侧的第一个可见字符是否属于避头点（即断在这里会把标点顶到行首）。 */
    fun isLineStartForbidden(nextText: String): Boolean {
        val character = nextText.firstVisible() ?: return false
        return character in lineStartForbidden
    }

    /** 断点左侧的最后一个可见字符是否属于避尾点（即断在这里会把标点留在行尾）。 */
    fun isLineEndForbidden(prevText: String): Boolean {
        val character = prevText.lastVisible() ?: return false
        return character in lineEndForbidden
    }

    /**
     * 在 [prevText] 与 [nextText] 之间断行的成本系数，越小越倾向在这里断。
     *
     * 避头点与避尾点会显著抬高系数；句读标点之后换行则打折。
     * 三种规则不会同时叠加，避头/避尾优先。
     */
    fun breakScale(prevText: String, nextText: String): Float {
        var scale = 1f
        if (isLineStartForbidden(nextText)) scale *= FORBIDDEN_AT_LINE_START_SCALE
        if (isLineEndForbidden(prevText)) scale *= FORBIDDEN_AT_LINE_END_SCALE
        if (scale == 1f) {
            val last = prevText.lastVisible()
            if (last != null && last in preferredBreakAfter) scale = PUNCTUATION_BREAK_SCALE
        }
        return scale
    }

    /**
     * 相邻两个音节之间是否允许断行，只依赖 wordId 与文本。
     *
     * 词边界（wordId 变化）一律允许；词内部只禁止「拉丁单词被劈成两半」这一种情况。
     * 中文歌词整句是同一个 wordId，但字与字之间是天然断点，必须放行。
     *
     * 刻意不参与 wordId 的改写：wordId 同时驱动单词弹跳动画的缩放原点与逐字进度，
     * 拆散它会让动效跟着变形。
     */
    fun canBreakBetween(
        prevWordId: Int,
        prevContent: String,
        nextWordId: Int,
        nextContent: String
    ): Boolean {
        // 破折号「——」、省略号「……」由两个相同符号并排组成，断在中间会变成两行各一半，
        // 这一条与 wordId 无关，必须优先判掉
        if (isDoubledPunctuationPair(prevContent, nextContent)) return false
        if (prevWordId != nextWordId) return true
        return !isInsideLatinWord(prevContent, nextContent)
    }

    /**
     * 相邻两个音节之间断行的成本系数，越小越倾向在这里断。
     *
     * 与 [breakScale] 的区别是多了一条「词内断行略贵」，
     * 让动态规划在宽度允许时优先选择词边界，读起来更自然。
     */
    fun breakScale(
        prevWordId: Int,
        prevContent: String,
        nextWordId: Int,
        nextContent: String
    ): Float {
        var scale = breakScale(prevContent, nextContent)
        if (prevWordId == nextWordId) scale *= INTRA_WORD_BREAK_SCALE
        return scale
    }

    /**
     * 避头点的兜底修正：为了让下一行不以标点起头，需要从当前行末尾摘下几个字一起带走。
     *
     * 返回需要摘下的字数（0 表示无需修正）。贪心算法没有回退能力，
     * 只能事后把上一行的末尾往下一行搬，直到新行首不再是避头点。
     *
     * [lineContents] 是当前行各音节的原始文本（按顺序），[nextContent] 是被挤到下一行的音节。
     * 拉丁单词不拆 —— 宁可让标点顶头，也不把半个单词搬到下一行。
     */
    fun headAvoidingDetachCount(lineContents: List<String>, nextContent: String): Int {
        if (!isLineStartForbidden(nextContent)) return 0
        var count = 0
        for (index in lineContents.indices.reversed()) {
            val content = lineContents[index]
            if (!content.isBreakableLyricUnit()) break
            count++
            if (!isLineStartForbidden(content)) break
        }
        return count
    }
}

/**
 * 相邻两个音节是不是同一组成对符号的两半（「——」「……」）。
 *
 * 这类符号必须整体出现：拆到两行会变成上一行一个短横、下一行一个短横，读者会当成两个符号。
 */
internal fun isDoubledPunctuationPair(prevContent: String, nextContent: String): Boolean {
    val previous = prevContent.lastVisibleChar() ?: return false
    val next = nextContent.firstVisibleChar() ?: return false
    if (previous != next) return false
    return previous in doubledPunctuationChars
}

/** 需要成对出现的符号：破折号、省略号，以及全角连字符。 */
private val doubledPunctuationChars = setOf('—', '…', '－', '─')

/**
 * 这个音节能被当成「可独立断开的排版单位」吗？
 *
 * 中文歌词里一个字、一个中文标点都是天然断点；拉丁单词必须保持完整，
 * 所以只要内容里混入了非中文、非标点的字符就一律返回 false。
 */
internal fun String.isBreakableLyricUnit(): Boolean {
    val trimmed = trim()
    if (trimmed.isEmpty()) return false
    return trimmed.all { it.isCjk() } || trimmed.isPunctuation()
}

/**
 * 拉丁字母/数字——也就是「拆开就会看出破绽」的字符。
 *
 * 判断能不能断行时只看相邻两个音节的这两个字符是否同时为真：
 * 同时为真说明断点落在某个拉丁单词内部，必须禁止；
 * 其余情况（中文接中文、中文接英文、标点接任意）都允许断开。
 */
internal fun Char.isLatinWordChar(): Boolean {
    return isLetterOrDigit() && !isCjk() && !isJapanese() && !isKorean()
}

/**
 * 撇号与连字符：它们夹在拉丁字母之间时属于单词的一部分（don't、well-known），
 * 从这儿断开同样会看出破绽。
 */
private val latinWordJoiners = setOf('\'', '\u2019', '-', '\u2010', '\u2011', '_')

/** [content] 末尾的可见字符；全是空白时返回 null。 */
internal fun String.lastVisibleChar(): Char? = trimEnd().lastOrNull()

/** [content] 首部的可见字符；全是空白时返回 null。 */
internal fun String.firstVisibleChar(): Char? = trimStart().firstOrNull()

/**
 * 断点是否落在某个拉丁单词的内部（例如把 "hello" 劈成 "hel|lo"）。
 *
 * 撇号与连字符两侧只要有一边是拉丁字母，也判定为词内部，避免拆散 don't / well-known。
 */
internal fun isInsideLatinWord(prevText: String, nextText: String): Boolean {
    val previous = prevText.lastVisibleChar() ?: return false
    val next = nextText.firstVisibleChar() ?: return false
    if (next in latinWordJoiners) return previous.isLatinWordChar() || previous in latinWordJoiners
    if (previous in latinWordJoiners) return next.isLatinWordChar()
    return previous.isLatinWordChar() && next.isLatinWordChar()
}
