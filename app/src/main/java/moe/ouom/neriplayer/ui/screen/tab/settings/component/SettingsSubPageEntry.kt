package moe.ouom.neriplayer.ui.screen.tab.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import moe.ouom.neriplayer.ui.screen.tab.settings.page.SettingsPage

/**
 * 一级分类页里指向三级子页面的入口行。
 *
 * 分类页是把若干原子页拼接出来的，像「音效」「USB 独占」这类独立成页的子模块
 * 不会被拼进去，必须由分类页显式提供入口，否则只能靠搜索进入。
 */
@Composable
internal fun SettingsSubPageEntryRow(
    page: SettingsPage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .settingsItemClickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
        },
        headlineContent = { Text(stringResource(page.titleRes)) },
        supportingContent = {
            Text(
                text = stringResource(page.descriptionRes),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

/** 一组三级页面入口，放在分区卡里，带一行卡内标题。 */
@Composable
internal fun SettingsSubPageEntrySection(
    title: String,
    entries: List<Pair<SettingsPage, () -> Unit>>,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        description?.let { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        entries.forEach { (page, onClick) ->
            SettingsSubPageEntryRow(page = page, onClick = onClick)
        }
    }
}
