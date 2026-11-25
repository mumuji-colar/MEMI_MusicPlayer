package com.mmc.merimusicplayer.ui.screen.tab.settings.component.lyrics

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import com.mmc.merimusicplayer.common.R as CoreCommonR
import com.mmc.merimusicplayer.data.model.settings.lyrics.BluetoothMetadataMode
import com.mmc.merimusicplayer.data.settings.AutoSettingsSchema
import com.mmc.merimusicplayer.data.settings.SettingsRepository
import com.mmc.merimusicplayer.ui.screen.tab.settings.component.AutoSettingSpecListItem
import com.mmc.merimusicplayer.ui.screen.tab.settings.miuix.MiuixSettingsChoiceRow
import com.mmc.merimusicplayer.ui.screen.tab.settings.miuix.MiuixSettingsDialog
import com.mmc.merimusicplayer.ui.screen.tab.settings.miuix.MiuixSettingsTextButton

@Composable
internal fun BluetoothMetadataModeSetting(
    repository: SettingsRepository,
    highlightTargetId: String? = null,
    highlightPulse: Int = 0,
    onHighlightFinished: (() -> Unit)? = null,
) {
    val mode by repository.bluetoothMetadataModeFlow.collectAsState(initial = BluetoothMetadataMode.SongAndLyrics)
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    AutoSettingSpecListItem(
        setting = AutoSettingsSchema.lyrics.bluetoothMetadataMode,
        supportingContent = { Text(stringResource(bluetoothMetadataModeLabel(mode))) },
        highlightTargetId = highlightTargetId,
        highlightPulse = highlightPulse,
        onHighlightFinished = onHighlightFinished,
        onClick = { showDialog = true },
    )
    if (!showDialog) return
    MiuixSettingsDialog(
        onDismissRequest = { showDialog = false },
        title = { Text(stringResource(CoreCommonR.string.settings_bluetooth_metadata_mode)) },
        text = {
            Column {
                Text(stringResource(CoreCommonR.string.settings_bluetooth_metadata_mode_desc))
                BluetoothMetadataMode.entries.forEach { option ->
                    MiuixSettingsChoiceRow(
                        title = stringResource(bluetoothMetadataModeLabel(option)),
                        selected = option == mode,
                        onClick = {
                            scope.launch { repository.setBluetoothMetadataMode(option) }
                            showDialog = false
                        },
                    )
                }
            }
        },
        confirmButton = {
            MiuixSettingsTextButton(onClick = { showDialog = false }) {
                Text(stringResource(CoreCommonR.string.action_close))
            }
        },
    )
}

internal fun bluetoothMetadataModeLabel(mode: BluetoothMetadataMode): Int = when (mode) {
    BluetoothMetadataMode.Lyrics -> CoreCommonR.string.settings_bluetooth_mode_lyrics
    BluetoothMetadataMode.SongInfo -> CoreCommonR.string.settings_bluetooth_mode_song_info
    BluetoothMetadataMode.SongAndLyrics -> CoreCommonR.string.settings_bluetooth_mode_song_and_lyrics
}
