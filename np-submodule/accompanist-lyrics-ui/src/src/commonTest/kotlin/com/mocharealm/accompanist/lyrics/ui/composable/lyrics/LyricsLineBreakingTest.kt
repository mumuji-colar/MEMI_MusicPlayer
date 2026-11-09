package com.mocharealm.accompanist.lyrics.ui.composable.lyrics

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 歌词断行规则。
 *
 * 这些规则决定「中文字大的时候从哪儿换行」，直接影响观感，
 * 所以每一条都单独锁住：避头点、避尾点、句读优先、拉丁词不拆、以及贪心兜底的搬运字数。
 */
class LyricsLineBreakingTest {

    // ---- 避头点：标点不许出现在行首 ----

    @Test
    fun `common Chinese punctuation is forbidden at line start`() {
        listOf("，", "。", "、", "；", "：", "！", "？", "）", "」", "》", "…").forEach { punctuation ->
            assertTrue(
                "「$punctuation」应被判定为避头点",
                LyricsLineBreaking.isLineStartForbidden(punctuation)
            )
        }
    }

    @Test
    fun `ordinary characters are allowed at line start`() {
        listOf("我", "a", "1", "（", "「").forEach { content ->
            assertFalse(
                "「$content」不该被当成避头点",
                LyricsLineBreaking.isLineStartForbidden(content)
            )
        }
    }

    @Test
    fun `line start check looks through trailing whitespace`() {
        // 音节可能带空格，判定必须看第一个可见字符
        assertTrue(LyricsLineBreaking.isLineStartForbidden(" 。"))
        assertFalse(LyricsLineBreaking.isLineStartForbidden("  你"))
    }

    // ---- 避尾点：开引号、开括号不许留在行尾 ----

    @Test
    fun `opening punctuation is forbidden at line end`() {
        listOf("（", "【", "《", "「", "『").forEach { punctuation ->
            assertTrue(
                "「$punctuation」应被判定为避尾点",
                LyricsLineBreaking.isLineEndForbidden(punctuation)
            )
        }
    }

    @Test
    fun `ordinary characters are allowed at line end`() {
        assertFalse(LyricsLineBreaking.isLineEndForbidden("我"))
        assertFalse(LyricsLineBreaking.isLineEndForbidden("。"))
    }

    // ---- 成本系数：越小越倾向在这里断 ----

    @Test
    fun `breaking before forbidden punctuation is heavily penalized`() {
        val penalized = LyricsLineBreaking.breakScale(prevText = "知道", nextText = "，")
        val neutral = LyricsLineBreaking.breakScale(prevText = "知道", nextText = "你")
        assertTrue("避头点必须比普通断点昂贵得多", penalized > neutral * 4f)
    }

    @Test
    fun `breaking after opening bracket is heavily penalized`() {
        val penalized = LyricsLineBreaking.breakScale(prevText = "（", nextText = "我")
        val neutral = LyricsLineBreaking.breakScale(prevText = "你", nextText = "我")
        assertTrue("避尾点必须比普通断点昂贵得多", penalized > neutral * 4f)
    }

    @Test
    fun `breaking right after a comma is cheaper than an arbitrary break`() {
        val afterComma = LyricsLineBreaking.breakScale(prevText = "你好，", nextText = "世界")
        val arbitrary = LyricsLineBreaking.breakScale(prevText = "你好", nextText = "世界")
        assertTrue("句读之后断行应当更便宜", afterComma < arbitrary)
    }

    @Test
    fun `neutral break keeps unit scale`() {
        assertEquals(1f, LyricsLineBreaking.breakScale("你好", "世界"), 1e-6f)
    }

    // ---- 拉丁单词不拆 ----

    @Test
    fun `latin word is never split in the middle`() {
        assertTrue(isInsideLatinWord("hel", "lo"))
        assertTrue(isInsideLatinWord("don", "'t"))
    }

    @Test
    fun `cjk to cjk can break anywhere`() {
        assertFalse(isInsideLatinWord("知", "道"))
    }

    @Test
    fun `cjk followed by latin can break`() {
        // 「你feel」这种中英混排，断在中英之间是安全的
        assertFalse(isInsideLatinWord("你", "feel"))
    }

    @Test
    fun `latin followed by cjk can break`() {
        assertFalse(isInsideLatinWord("feel", "你"))
    }

    @Test
    fun `punctuation adjacent to latin can break`() {
        assertFalse(isInsideLatinWord("hello", "，"))
        assertFalse(isInsideLatinWord("，", "hello"))
    }

    // ---- 贪心兜底的避头点搬运 ----

    @Test
    fun `no detach when next syllable is not forbidden at start`() {
        val count = LyricsLineBreaking.headAvoidingDetachCount(
            lineContents = listOf("你", "好"),
            nextContent = "世"
        )
        assertEquals(0, count)
    }

    @Test
    fun `detach one char when next syllable is forbidden punctuation`() {
        // 行尾是「道」，下一行要起头的是「，」——把「道」搬下去组成「道，」
        val count = LyricsLineBreaking.headAvoidingDetachCount(
            lineContents = listOf("知", "道"),
            nextContent = "，"
        )
        assertEquals(1, count)
    }

    @Test
    fun `keep detaching while the carried char is itself forbidden`() {
        // 行尾连着一串标点，必须一起搬走，否则新行首还是标点
        val count = LyricsLineBreaking.headAvoidingDetachCount(
            lineContents = listOf("好", "。", "。"),
            nextContent = "，"
        )
        assertEquals(3, count)
    }

    @Test
    fun `never split a latin word while detaching`() {
        // 行尾是拉丁单词，宁可让标点顶头，也不搬半个单词
        val count = LyricsLineBreaking.headAvoidingDetachCount(
            lineContents = listOf("hello"),
            nextContent = "，"
        )
        assertEquals(0, count)
    }

    @Test
    fun `detach stops at latin word boundary`() {
        val count = LyricsLineBreaking.headAvoidingDetachCount(
            lineContents = listOf("你", "hello"),
            nextContent = "，"
        )
        assertEquals(0, count)
    }

    @Test
    fun `detach can take all chars when the line is pure punctuation cascade`() {
        val count = LyricsLineBreaking.headAvoidingDetachCount(
            lineContents = listOf("，", "。"),
            nextContent = "，"
        )
        assertEquals(2, count)
    }

    // ---- 可断单元判定 ----

    @Test
    fun `cjk char and cjk punctuation are breakable units`() {
        assertTrue("我".isBreakableLyricUnit())
        assertTrue("，".isBreakableLyricUnit())
    }

    @Test
    fun `latin word is not a breakable unit`() {
        assertFalse("hello".isBreakableLyricUnit())
    }

    @Test
    fun `blank is not a breakable unit`() {
        assertFalse("   ".isBreakableLyricUnit())
    }

    // ---- 断行允许性：词边界一律放行 ----

    @Test
    fun `canBreakBetween allows any word boundary`() {
        assertTrue(
            LyricsLineBreaking.canBreakBetween(
                prevWordId = 0, prevContent = "hello",
                nextWordId = 1, nextContent = "你"
            )
        )
    }

    @Test
    fun `canBreakBetween allows intra-word break for cjk`() {
        // 中文歌词整句共用一个 wordId，字与字之间必须允许断开
        assertTrue(
            LyricsLineBreaking.canBreakBetween(
                prevWordId = 7, prevContent = "知",
                nextWordId = 7, nextContent = "道"
            )
        )
    }

    @Test
    fun `canBreakBetween forbids intra-word break for latin`() {
        assertFalse(
            LyricsLineBreaking.canBreakBetween(
                prevWordId = 3, prevContent = "hel",
                nextWordId = 3, nextContent = "lo"
            )
        )
    }

    @Test
    fun `canBreakBetween allows break between cjk and latin inside one word`() {
        // 「你feel」共用一个 wordId 时，断在中英之间是安全的
        assertTrue(
            LyricsLineBreaking.canBreakBetween(
                prevWordId = 2, prevContent = "你",
                nextWordId = 2, nextContent = "feel"
            )
        )
    }

    // ---- 断点系数：词内断行略贵于词边界 ----

    @Test
    fun `intra word break costs more than word boundary break`() {
        val sameWord = LyricsLineBreaking.breakScale(
            prevWordId = 0, prevContent = "知",
            nextWordId = 0, nextContent = "道"
        )
        val crossWord = LyricsLineBreaking.breakScale(
            prevWordId = 0, prevContent = "知",
            nextWordId = 1, nextContent = "道"
        )
        assertTrue("同一个 wordId 内断行应略贵，好让词边界优先", sameWord > crossWord)
    }

    @Test
    fun `intra word penalty still yields to balanced line breaking`() {
        // 词内断行只是「略贵」，不能贵到让动态规划放弃均衡断行
        val intraWord = LyricsLineBreaking.breakScale(
            prevWordId = 0, prevContent = "知",
            nextWordId = 0, nextContent = "道"
        )
        assertTrue("词内断行惩罚应当温和（<1.5x）", intraWord < 1.5f)
    }

    @Test
    fun `break scale with word ids still penalizes forbidden punctuation`() {
        val penalized = LyricsLineBreaking.breakScale(
            prevWordId = 0, prevContent = "知",
            nextWordId = 0, nextContent = "，"
        )
        val neutral = LyricsLineBreaking.breakScale(
            prevWordId = 0, prevContent = "知",
            nextWordId = 0, nextContent = "道"
        )
        assertTrue("避头点即便在同一个词内也必须昂贵", penalized > neutral * 3f)
    }

    // ---- 动态规划内核：核心收益是「长短均衡」，不能再退化成逐字硬塞 ----

    /** 造一组等宽音节，宽度全为 1，方便按「字数」推算可用宽度。 */
    private fun uniformWidths(count: Int) = FloatArray(count) { 1f }

    @Test
    fun `fourteen chars split into two balanced lines of seven`() {
        // 改造前的关键症状：中文整句是一个超长单词，行内不允许断，
        // DP 无解后回退贪心，把第一行塞满才换行。现在应当给出 7 + 7。
        val starts = computeBalancedBreakStarts(
            widths = uniformWidths(14),
            availableWidthPx = 7.4f,
            breakAllowed = { true },
            breakScale = { 1f }
        )
        assertNotNull(starts)
        assertArrayEquals(intArrayOf(0, 7), starts)
    }

    @Test
    fun `balanced split beats greedy hard packing`() {
        // 14 字 / 每行最多 10 字：贪心会得到 10 + 4，
        // 均衡算法应当选择 7 + 7，避免出现「一行很挤、一行很空」。
        val starts = computeBalancedBreakStarts(
            widths = uniformWidths(14),
            availableWidthPx = 10f,
            breakAllowed = { true },
            breakScale = { 1f }
        )
        assertNotNull(starts)
        assertArrayEquals(intArrayOf(0, 7), starts)
    }

    @Test
    fun `single line when everything fits`() {
        val starts = computeBalancedBreakStarts(
            widths = uniformWidths(8),
            availableWidthPx = 100f,
            breakAllowed = { true },
            breakScale = { 1f }
        )
        assertNotNull(starts)
        assertArrayEquals(intArrayOf(0), starts)
    }

    @Test
    fun `break not allowed in the middle forces the word to stay together`() {
        // 前 4 个音节是一个拉丁单词，内部（断点 1..3）不允许断开。
        val breaksForbidden = setOf(1, 2, 3)
        val starts = computeBalancedBreakStarts(
            widths = uniformWidths(8),
            availableWidthPx = 5f,
            breakAllowed = { index -> index <= 0 || index !in breaksForbidden },
            breakScale = { 1f }
        )
        assertNotNull(starts)
        // 第一个断点只能是 4（把整个单词完整放进第一行），不能落在 1..3
        assertEquals(4, starts!![1])
    }

    @Test
    fun `break cost steers the split away from a forbidden position`() {
        // 10 字、每行最多 6 字。若断点 5 代价极高（比如那里是避头点），
        // 算法应当宁可选择 4 + 6 也不要 5 + 5。
        val starts = computeBalancedBreakStarts(
            widths = uniformWidths(10),
            availableWidthPx = 6.4f,
            breakAllowed = { true },
            breakScale = { index -> if (index == 5) 100f else 1f }
        )
        assertNotNull(starts)
        assertFalse("不能把断点放在被惩罚的位置", starts!!.contains(5))
    }

    @Test
    fun `returns null when a single syllable cannot fit on its own line`() {
        // 容器比一个字还窄，任何断法都无解，调用方据此退化为贪心
        val starts = computeBalancedBreakStarts(
            widths = uniformWidths(4),
            availableWidthPx = 0.5f,
            breakAllowed = { true },
            breakScale = { 1f }
        )
        assertNull(starts)
    }

    @Test
    fun `empty input yields empty starts`() {
        val starts = computeBalancedBreakStarts(
            widths = FloatArray(0),
            availableWidthPx = 100f,
            breakAllowed = { true },
            breakScale = { 1f }
        )
        assertEquals(0, starts?.size)
    }

    // ---- 中文词边界：不要从词中间劈开 ----

    @Test
    fun `breaking at a han word boundary is cheaper than breaking inside one`() {
        val atBoundary = wordAwareBreakScale(0, "你", 0, "一起", breakIsWordBoundary = true)
        val insideWord = wordAwareBreakScale(0, "一", 0, "起", breakIsWordBoundary = false)
        assertTrue(
            "词边界应当比词内更便宜：boundary=$atBoundary, inside=$insideWord",
            atBoundary < insideWord
        )
    }

    @Test
    fun `han word split is detected only between han characters inside a word`() {
        assertTrue(isIntraHanWordBreak("一", "起", breakIsWordBoundary = false))
        assertFalse(isIntraHanWordBreak("你", "一起", breakIsWordBoundary = true))
        // 拉丁词内、汉字接标点都不算——它们各有各的规则
        assertFalse(isIntraHanWordBreak("hel", "lo", breakIsWordBoundary = false))
        assertFalse(isIntraHanWordBreak("走", "，", breakIsWordBoundary = false))
    }

    @Test
    fun `balanced breaking keeps a two character chinese word intact`() {
        // 16 个字、每行最多 9 个、词边界在第 7 个字后。
        // 纯均衡会在第 8 个字断（8 + 8 最整齐），但那会把一个词劈成两半；
        // 加上拆词惩罚后应当改断在第 7 个字，接受 7 + 9 的一点点不齐。
        val contents = List(16) { "字" }
        val widths = FloatArray(contents.size) { 1f }
        val wordBoundaries = setOf(0, 7, contents.size)
        val starts = computeBalancedBreakStarts(
            widths = widths,
            availableWidthPx = 9f,
            breakAllowed = { true },
            breakScale = { index ->
                if (index <= 0) 1f
                else wordAwareBreakScale(
                    prevWordId = 0,
                    prevContent = contents[index - 1],
                    nextWordId = 0,
                    nextContent = contents[index],
                    breakIsWordBoundary = index in wordBoundaries
                )
            },
            breakPenalty = { index ->
                if (index <= 0) 0f
                else if (
                    isIntraHanWordBreak(
                        prevContent = contents[index - 1],
                        nextContent = contents[index],
                        breakIsWordBoundary = index in wordBoundaries
                    )
                ) 1f * 1f * CJK_WORD_SPLIT_PENALTY_SYLLABLES
                else 0f
            }
        )
        assertNotNull(starts)
        assertArrayEquals(intArrayOf(0, 7), starts)
    }

    @Test
    fun `without the word split penalty the same line splits at the balanced point`() {
        // 对照组：不传惩罚时会断在更均衡的第 8 个字，证明上一条测试确实由惩罚驱动
        val contents = List(16) { "字" }
        val starts = computeBalancedBreakStarts(
            widths = FloatArray(contents.size) { 1f },
            availableWidthPx = 9f,
            breakAllowed = { true },
            breakScale = { 1f }
        )
        assertNotNull(starts)
        assertArrayEquals(intArrayOf(0, 8), starts)
    }

    // ---- 成对符号：破折号、省略号不许拆到两行 ----

    @Test
    fun `doubled punctuation pair is recognised`() {
        assertTrue(isDoubledPunctuationPair("他走了—", "—然后"))
        assertTrue(isDoubledPunctuationPair("我等…", "…了很久"))
        // 整组符号已经在同一个音节里，不存在拆开的风险
        assertFalse(isDoubledPunctuationPair("我等", "……"))
        assertFalse(isDoubledPunctuationPair("你", "好"))
        assertFalse(isDoubledPunctuationPair("—", "你"))
    }

    @Test
    fun `canBreakBetween refuses to split doubled punctuation even across words`() {
        assertFalse(
            LyricsLineBreaking.canBreakBetween(
                prevWordId = 0, prevContent = "他走了—",
                nextWordId = 1, nextContent = "—然后"
            )
        )
    }

    @Test
    fun `left quotation mark is forbidden at line end`() {
        assertTrue(LyricsLineBreaking.isLineEndForbidden("他说“"))
        assertFalse(LyricsLineBreaking.isLineEndForbidden("他说"))
    }
}
