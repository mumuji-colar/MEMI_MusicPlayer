package com.mocharealm.accompanist.lyrics.ui.composable.lyrics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine

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
            style = textStyle,
            color = textColor,
            textAlign = textAlign
        )
        if (showTranslation) {
            line.translation?.let {
                Text(
                    text = it,
                    color = textColor.copy(alpha = 0.6f),
                    textAlign = textAlign
                )
            }
        }
    }
}

