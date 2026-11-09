package com.mocharealm.accompanist.lyrics.ui.composable.lyrics

import java.util.Locale

/**
 * 中文词边界（分词）。
 *
 * 背景：歌词断行只按字宽做均衡，中文字与字之间处处等权，于是会把「一起」「知道」这类词
 * 从中间劈开——「我想要和你一 / 起去看海」。这一层用系统自带的 ICU 词典补上词边界，
 * 让断行算法知道「哪里是词与词之间」。
 *
 * 三条约束：
 * 1. **不改 wordId**。wordId 同时驱动单词弹跳动画的缩放原点和逐字进度，拆散它动效会跟着变形；
 *    这里只输出「字符偏移是否落在词边界」，成本由断行算法自己乘。
 * 2. **失败即降级**。JVM 单元测试里没有 android.icu，取不到边界就返回空集合，
 *    断行退回改造前的纯均衡行为，不会崩。
 * 3. **结果缓存**。同一行歌词会在滚动过程中反复排版，按文本缓存边界，避免反复建 BreakIterator。
 */
internal object CjkWordSegmentation {

    /** 缓存上限：一首歌的歌词行数通常在几百，128 条足够覆盖可视范围附近的行。 */
    private const val MaxCachedLines = 128

    private val cache = object : LinkedHashMap<String, Set<Int>>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Set<Int>>): Boolean =
            size > MaxCachedLines
    }

    /**
     * 返回 [text] 的词边界字符偏移集合（含 0 与 text.length）。
     *
     * 取不到词典（非 Android 运行时）时返回空集合，调用方据此不做任何额外加价。
     */
    fun boundaries(text: String): Set<Int> {
        if (text.isEmpty()) return emptySet()
        synchronized(cache) { cache[text] }?.let { return it }

        val computed = runCatching { computeWithIcu(text) }.getOrNull().orEmpty()

        synchronized(cache) { cache[text] = computed }
        return computed
    }

    private fun computeWithIcu(text: String): Set<Int> {
        val iterator = android.icu.text.BreakIterator.getWordInstance(Locale.CHINESE)
        iterator.setText(text)
        val result = mutableSetOf<Int>()
        var index = iterator.first()
        while (index != android.icu.text.BreakIterator.DONE) {
            result += index
            index = iterator.next()
        }
        // 行尾一定要算边界，否则最后一个词永远被当成「词内断行」
        result += text.length
        return result
    }
}
