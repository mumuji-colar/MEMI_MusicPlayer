package moe.ouom.neriplayer.ui.screen.tab.settings.about

/*
 * NeriPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * https://github.com/cwuom/NeriPlayer
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * File: moe.ouom.neriplayer.ui.screen.tab.settings.about/SettingsAboutSection
 * Updated: 2026/3/23
 */

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.ouom.neriplayer.BuildConfig
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.ui.screen.tab.settings.component.settingsItemClickable
import moe.ouom.neriplayer.util.format.convertTimestampToDate
import moe.ouom.neriplayer.R as AppR

internal fun LazyListScope.settingsAboutSection(
    devModeEnabled: Boolean,
    onVersionClick: () -> Unit,
    onCopyValue: (String) -> Unit = {}
) {
    item {
        SettingsAboutContent(
            devModeEnabled = devModeEnabled,
            onVersionClick = onVersionClick,
            onCopyValue = onCopyValue
        )
    }
}

@Composable
internal fun SettingsAboutContent(
    devModeEnabled: Boolean,
    onVersionClick: () -> Unit,
    onCopyValue: (String) -> Unit
) {
    var showLicenseDialog by remember { mutableStateOf(false) }

    SettingsAboutIntroItem()
    SettingsBuildUuidItem(onCopyValue)
    SettingsVersionItem(
        devModeEnabled = devModeEnabled,
        onVersionClick = onVersionClick,
        onCopyValue = onCopyValue
    )
    SettingsBuildTimeItem(onCopyValue)
    SettingsSpecialThanksItem()
    SettingsSourceCodeItem()
    SettingsOpenSourceLicenseItem(onClick = { showLicenseDialog = true })

    if (showLicenseDialog) {
        OpenSourceLicenseDialog(onDismiss = { showLicenseDialog = false })
    }
}

@Composable
private fun SettingsAboutIntroItem() {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(CoreCommonR.string.settings_about),
                tint = MaterialTheme.colorScheme.onSurface
            )
        },
        headlineContent = {
            Text(
                text = stringResource(CoreCommonR.string.settings_about),
                style = MaterialTheme.typography.titleMedium
            )
        },
        supportingContent = { Text(stringResource(AppR.string.about_app_footer)) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun SettingsBuildUuidItem(onCopyValue: (String) -> Unit) {
    val buildUuid = BuildConfig.BUILD_UUID

    ListItem(
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.Verified,
                contentDescription = stringResource(CoreCommonR.string.settings_build_uuid),
                tint = MaterialTheme.colorScheme.onSurface
            )
        },
        headlineContent = {
            Text(
                text = stringResource(CoreCommonR.string.settings_build_uuid),
                style = MaterialTheme.typography.titleMedium
            )
        },
        supportingContent = { Text(buildUuid) },
        modifier = androidx.compose.ui.Modifier.settingsItemClickable {
            onCopyValue(buildUuid)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun SettingsVersionItem(
    devModeEnabled: Boolean,
    onVersionClick: () -> Unit,
    onCopyValue: (String) -> Unit
) {
    val suffix = if (devModeEnabled) {
        " (${stringResource(CoreCommonR.string.settings_version_debug_suffix)})"
    } else {
        ""
    }
    val versionName = "${BuildConfig.VERSION_NAME}$suffix"

    ListItem(
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.Update,
                contentDescription = stringResource(CoreCommonR.string.settings_version),
                tint = MaterialTheme.colorScheme.onSurface
            )
        },
        headlineContent = {
            Text(
                text = stringResource(CoreCommonR.string.common_version),
                style = MaterialTheme.typography.titleMedium
            )
        },
        supportingContent = { Text(versionName) },
        modifier = androidx.compose.ui.Modifier.settingsItemClickable {
            onCopyValue(versionName)
            onVersionClick()
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun SettingsBuildTimeItem(onCopyValue: (String) -> Unit) {
    val buildTime = convertTimestampToDate(BuildConfig.BUILD_TIMESTAMP)

    ListItem(
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.Timer,
                contentDescription = stringResource(CoreCommonR.string.settings_build_time),
                tint = MaterialTheme.colorScheme.onSurface
            )
        },
        headlineContent = {
            Text(
                text = stringResource(CoreCommonR.string.common_build_time),
                style = MaterialTheme.typography.titleMedium
            )
        },
        supportingContent = { Text(buildTime) },
        modifier = androidx.compose.ui.Modifier.settingsItemClickable {
            onCopyValue(buildTime)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun SettingsSpecialThanksItem() {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.FavoriteBorder,
                contentDescription = stringResource(AppR.string.about_special_thanks),
                tint = MaterialTheme.colorScheme.onSurface
            )
        },
        headlineContent = { Text(stringResource(AppR.string.about_special_thanks)) },
        supportingContent = { Text(stringResource(AppR.string.about_special_thanks_names)) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

private const val OpenSourceLicenseAssetPath = "licenses/GPL-3.0.txt"

/** GPLv3 §6d：以目标代码分发时，须让接收者能获得对应源码。 */
internal const val SourceRepositoryUrl = "https://github.com/mumuji-colar/MEMI_MusicPlayer"

@Composable
private fun SettingsSourceCodeItem() {
    val context = LocalContext.current
    ListItem(
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.Code,
                contentDescription = stringResource(AppR.string.about_source_code),
                tint = MaterialTheme.colorScheme.onSurface
            )
        },
        headlineContent = { Text(stringResource(AppR.string.about_source_code)) },
        supportingContent = { Text(stringResource(AppR.string.about_source_code_desc)) },
        modifier = androidx.compose.ui.Modifier.settingsItemClickable {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(SourceRepositoryUrl))
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

/**
 * GPLv3 §5d 要求带交互界面的程序展示"适当的法律声明"，并说明如何查看许可证副本。
 * 这里同时给出声明与全文入口，全文随 APK 打包（离线可读）。
 */
@Composable
private fun SettingsOpenSourceLicenseItem(onClick: () -> Unit) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.Description,
                contentDescription = stringResource(AppR.string.about_open_source_license),
                tint = MaterialTheme.colorScheme.onSurface
            )
        },
        headlineContent = { Text(stringResource(AppR.string.about_open_source_license)) },
        supportingContent = {
            Text(stringResource(AppR.string.about_open_source_license_desc))
        },
        modifier = androidx.compose.ui.Modifier.settingsItemClickable { onClick() },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun OpenSourceLicenseDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val licenseText by produceState(initialValue = "") {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open(OpenSourceLicenseAssetPath)
                    .bufferedReader()
                    .use { reader -> reader.readText() }
            }.getOrDefault("")
        }
    }
    val licenseBlocks = remember(licenseText) {
        licenseText.split("\n\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(AppR.string.about_license_dialog_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(AppR.string.about_license_close)
                        )
                    }
                }
                HorizontalDivider()

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(AppR.string.about_license_notice),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                    items(licenseBlocks) { block ->
                        Text(
                            text = block,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(AppR.string.about_license_close))
                    }
                }
            }
        }
    }
}
