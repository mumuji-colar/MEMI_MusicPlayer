package moe.ouom.neriplayer.ui.screen.tab.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.ui.theme.NeriDesign

/** LargeTopAppBar 自带的标题水平内边距，首页大标题据此补齐到卡片左边缘 */
internal val TopAppBarTitleInset = 16.dp

/**
 * 分区标题图标与网格卡片左边缘对齐。
 *
 * [onMoreClick] 非空时整行可点，末尾补一个 `›` 箭头做入口提示——
 * 只描一个箭头而不是「查看全部」文字，标题才不会被按钮抢走视线。
 */
@Composable
internal fun SectionHeader(
    icon: ImageVector,
    title: String,
    /** 非空时整行可点，用于进入该分区的完整列表页 */
    onMoreClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onMoreClick != null) {
        Modifier.clickable(role = Role.Button, onClick = onMoreClick)
    } else {
        Modifier
    }
    if (NeriDesign.isClean) {
        // clean style carries hierarchy by weight alone; the accent stays reserved for actions
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = clickableModifier.padding(top = 18.dp, bottom = 10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (onMoreClick != null) SectionHeaderChevron()
        }
        return
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f)
        )
        if (onMoreClick != null) SectionHeaderChevron()
    }
}

@Composable
private fun SectionHeaderChevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        contentDescription = stringResource(CoreCommonR.string.home_section_view_all),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(22.dp)
    )
}
