package com.mmc.merimusicplayer.data.sync.store.secure

import android.content.SharedPreferences
import com.mmc.merimusicplayer.common.storage.SecurePreferencesEvent
import com.mmc.merimusicplayer.common.storage.SecurePreferencesOpener

internal object SyncPreferenceRecovery {
    fun open(
        name: String,
        create: () -> SharedPreferences,
        delete: () -> Unit,
        onOpenFailure: (Throwable) -> Unit,
        onDeleteFailure: (Throwable) -> Unit,
        recoverOnFailure: Boolean = true
    ): SharedPreferences = SecurePreferencesOpener.open(
        name = name,
        create = create,
        delete = delete,
        rebuildCorrupted = recoverOnFailure,
        retryVolatile = true,
        report = { event, error ->
            if (event == SecurePreferencesEvent.DELETE_FAILED) onDeleteFailure(error) else onOpenFailure(error)
        }
    )
}
