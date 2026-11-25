package com.mmc.merimusicplayer.core.startup.safemode

import android.content.Context
import android.net.Uri
import com.mmc.merimusicplayer.data.backup.BackupManager
import com.mmc.merimusicplayer.data.config.ConfigExportOptions
import com.mmc.merimusicplayer.data.config.ConfigFileManager

internal class SafeModeDataExports(
    context: Context
) {
    private val appContext = context.applicationContext

    suspend fun exportConfigBackup(
        destination: Uri,
        options: ConfigExportOptions = ConfigExportOptions(),
        password: CharArray? = null
    ): Result<String> {
        return ConfigFileManager(appContext).exportConfig(destination, options, password)
    }

    suspend fun exportPlaylistBackup(destination: Uri): Result<String> {
        return BackupManager(appContext).exportPlaylists(destination)
    }

    fun generateConfigBackupFileName(): String {
        return ConfigFileManager(appContext).generateBackupFileName()
    }

    fun generatePlaylistBackupFileName(): String {
        return BackupManager(appContext).generateBackupFileName()
    }
}
