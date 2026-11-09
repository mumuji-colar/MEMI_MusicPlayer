package com.mocharealm.accompanist.lyrics.ui.composable.lyrics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine

/**
 * 没有逐字时间轴的歌词行（高级歌词里的普通行）也要按词断行。
 *
 * 这类行走的是普通 Compose `Text`，默认对中日韩逐字断行，会把「一起」劈到两行；
 * 与整行滚动版用同一套策略：短语（词典）断行 + Android 13 起的均衡断行。
 */
private val SyncedLineBreak: LineBreak = LineBreak(
    strategy = LineBreak.Strategy.Balanced,
    strictness = LineBreak.Strictness.Strict,
    wordBreak = LineBreak.WordBreak.Phrase
)

@Composable
fun SyncedLineText(
    line: SyncedLine,
    isLineRtl: Boolean,
    textStyle: TextStyle,
    textColor: Color,
    modifier: Modifier = Modifier,
    showTranslation: Boolean = true,
    // 默认沿用原来的「按语言方向贴边」，传 0.5f 即居中
    horizontalBias: Float = if (isLineRtl) 1f else 0f
) {
    val textAlign = when {
        horizontalBias <= 0.25f -> TextAlign.Start
        horizontalBias >= 0.75f -> TextAlign.End
        else -> TextAlign.Center
    }
    val columnAlignment = resolveColumnAlignment(horizontalBias)
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalAlignment = columnAlignment
    ) {
        Text(
            text = line.content,
            style = textStyle.copy(lineBreak = SyncedLineBreak),
            color = textColor,
            textAlign = textAlign
        )
        if (showTranslation) {
            line.translation?.let {
                Text(
                    text = it,
                    style = LocalTextStyle.current.copy(lineBreak = SyncedLineBreak),
                    color = textColor.copy(alpha = 0.6f),
                    textAlign = textAlign
                )
            }
        }
    }
}

