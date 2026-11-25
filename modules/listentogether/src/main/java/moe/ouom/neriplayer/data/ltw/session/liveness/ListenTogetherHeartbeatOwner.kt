package moe.ouom.neriplayer.data.ltw.session.liveness

import moe.ouom.neriplayer.data.ltw.session.state.LISTEN_TOGETHER_PAUSED_HEARTBEAT_INTERVAL_MS
import moe.ouom.neriplayer.data.ltw.session.state.LISTEN_TOGETHER_PLAYING_HEARTBEAT_INTERVAL_MS
import moe.ouom.neriplayer.data.ltw.session.state.resolveListenTogetherHeartbeatIntervalMs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import moe.ouom.neriplayer.common.logging.NPLogger
import moe.ouom.neriplayer.data.model.ltw.session.ListenTogetherConnectionState
import moe.ouom.neriplayer.data.model.ltw.message.event.ListenTogetherEvent
import moe.ouom.neriplayer.data.model.ltw.session.ListenTogetherSessionState
import kotlin.time.Duration.Companion.milliseconds

interface ListenTogetherHeartbeatPort {
    fun session(): ListenTogetherSessionState
    fun isController(session: ListenTogetherSessionState): Boolean
    fun currentTrackShareable(): Boolean
    fun playbackStateName(): String
    fun playbackPositionMs(): Long
    fun buildHeartbeat(state: String, positionMs: Long): ListenTogetherEvent

    /** 返回是否已成功投递; 失败时调用方需要尽快重试而不是空等一个间隔 */
    fun sendHeartbeat(event: ListenTogetherEvent): Boolean
}

class ListenTogetherHeartbeatOwner(
    private val scope: CoroutineScope,
    private val port: ListenTogetherHeartbeatPort,
    private val elapsedRealtimeMs: () -> Long
) {
    private var heartbeatJob: Job? = null
    @Volatile
    private var lastOutboundSyncAtMs = 0L

    fun noteOutboundSync() {
        lastOutboundSyncAtMs = elapsedRealtimeMs()
    }

    fun start() {
        if (heartbeatJob?.isActive == true || !port.isController(port.session())) return
        NPLogger.d(TAG, "startHeartbeat()")
        if (lastOutboundSyncAtMs == 0L) noteOutboundSync()
        heartbeatJob = scope.launch { runHeartbeatLoop() }
    }

    private suspend fun runHeartbeatLoop() {
        while (kotlinx.coroutines.currentCoroutineContext().isActive) {
            delay((heartbeatTick()).milliseconds)
        }
    }

    private fun heartbeatTick(): Long {
        val session = port.session()
        if (session.connectionState != ListenTogetherConnectionState.CONNECTED ||
            !port.isController(session) || !port.currentTrackShareable()
        ) return STATE_RECHECK_INTERVAL_MS
        val now = elapsedRealtimeMs()
        val idleMs = now - lastOutboundSyncAtMs
        val playbackState = port.playbackStateName()
        val intervalMs = resolveListenTogetherHeartbeatIntervalMs(
            isPlaying = playbackState == "playing",
            playingIntervalMs = LISTEN_TOGETHER_PLAYING_HEARTBEAT_INTERVAL_MS,
            pausedIntervalMs = LISTEN_TOGETHER_PAUSED_HEARTBEAT_INTERVAL_MS
        )
        val remainingMs = intervalMs - idleMs
        if (remainingMs > 0L) return minOf(remainingMs, STATE_RECHECK_INTERVAL_MS)
        val event = port.buildHeartbeat(playbackState, port.playbackPositionMs().coerceAtLeast(0L))
        NPLogger.d(TAG, "heartbeat(): eventId=${event.eventId}, positionMs=${event.positionMs}, idleMs=$idleMs")
        // 只有投递成功才推进同步时钟: 失败时保留旧的 idle, 下一个 tick 仍满足发送条件,
        // 于是能在 STATE_RECHECK_INTERVAL_MS 内重试, 而不是等满一整个心跳间隔
        if (port.sendHeartbeat(event)) {
            noteOutboundSync()
            return minOf(intervalMs, STATE_RECHECK_INTERVAL_MS)
        }
        return STATE_RECHECK_INTERVAL_MS
    }

    fun stop() {
        NPLogger.d(TAG, "stopHeartbeat()")
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    fun reset() {
        stop()
        lastOutboundSyncAtMs = 0L
    }

    private companion object {
        const val TAG = "NERI-ListenTogether"
        const val STATE_RECHECK_INTERVAL_MS = 10_000L
    }
}
