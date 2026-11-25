package com.mmc.merimusicplayer.core.di

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.mmc.merimusicplayer.network.weblogin.ForegroundWebLoginGuard

private val youTubeAuthChangeMutex = Mutex()

internal suspend fun withYouTubeAuthChange(
    updateAuth: () -> Unit,
    stopRefresh: suspend () -> Unit,
    clearWebState: suspend () -> Unit
) {
    currentCoroutineContext().ensureActive()
    youTubeAuthChangeMutex.withLock {
        currentCoroutineContext().ensureActive()
        withContext(NonCancellable) {
            ForegroundWebLoginGuard.enter("youtube_auth_change").use {
                updateAuth()
                stopRefresh()
                clearWebState()
            }
        }
    }
}
