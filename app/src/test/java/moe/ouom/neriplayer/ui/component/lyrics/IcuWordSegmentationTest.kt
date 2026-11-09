package moe.ouom.neriplayer.ui.component.lyrics

import android.icu.text.BreakIterator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/**
 * 平台 ICU 的词典分词是否可用。
 *
 * 歌词断行把「词边界」交给 `android.icu.text.BreakIterator`（见
 * `accompanist-lyrics-ui` 的 `CjkWordSegmentation`）。如果某个系统版本上它退化成
 * 「每个汉字都是边界」，断行的拆词惩罚会变成常数项、完全不起作用，
 * 而界面不会有任何报错——所以这里单独守住这个平台能力。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class IcuWordSegmentationTest {

    @Test
    fun `dictionary segmentation keeps two character words together`() {
        val boundaries = wordBoundaries("因为爱情不会轻易悲伤")

        // 「因为」是一个词：第 1 个字后面不该是边界
        assertFalse("「因为」被拆成了单字", 1 in boundaries)
        // 反过来，「爱情」开始的位置（第 2 个字后）必须是边界
        assertTrue("「爱情」前应当有词边界", 2 in boundaries)
        assertTrue("词数过少，可能退化成了整串一个词", boundaries.size >= 4)
    }

    @Test
    fun `segmentation never splits latin words`() {
        val boundaries = wordBoundaries("hello world")

        // hello 内部不得出现边界
        assertFalse(1 in boundaries)
        assertFalse(2 in boundaries)
        assertTrue("空格后应当是词边界", 6 in boundaries || 5 in boundaries)
    }

    private fun wordBoundaries(text: String): Set<Int> {
        val iterator = BreakIterator.getWordInstance(Locale.CHINESE)
        iterator.setText(text)
        val result = mutableSetOf<Int>()
        var index = iterator.first()
        while (index != BreakIterator.DONE) {
            result += index
            index = iterator.next()
        }
        return result
    }
}
