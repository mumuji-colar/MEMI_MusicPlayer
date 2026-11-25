package com.mmc.merimusicplayer.ui.screen.tab.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.mmc.merimusicplayer.data.model.stats.UsageEntry
import com.mmc.merimusicplayer.common.logging.NPLogger
import com.mmc.merimusicplayer.data.playlist.usage.PlaylistUsageRepository

internal data class HomeUsageSnapshot(
    val entries: List<UsageEntry> = emptyList(),
    val isLoaded: Boolean = false
)

@Composable
internal fun rememberHomeUsageSnapshot(
    key: Any?,
    readDispatcher: CoroutineDispatcher = Dispatchers.IO,
    readRepository: suspend () -> PlaylistUsageRepository
): State<HomeUsageSnapshot> = produceState(
    initialValue = HomeUsageSnapshot(),
    key1 = key
) {
    val usageFlow = withContext(readDispatcher) {
        readRepository().loadedFrequentPlaylistsFlow
    }
    usageFlow.collect { entries ->
        value = HomeUsageSnapshot(entries = entries, isLoaded = true)
    }
}

@Composable
internal fun HomeContinueDiagnostics(setting: Boolean, snapshot: HomeUsageSnapshot) {
    val count = snapshot.entries.size
    val localCount = snapshot.entries.count { it.source == PlaylistUsageRepository.SOURCE_LOCAL }
    val visible = shouldShowHomeContinueSection(setting, snapshot.isLoaded, count > 0)
    LaunchedEffect(setting, snapshot.isLoaded, count, localCount) {
        NPLogger.d(
            "HomeContinue",
            "setting=$setting loaded=${snapshot.isLoaded} count=$count localCount=$localCount visible=$visible"
        )
    }
}
