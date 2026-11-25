package com.mmc.merimusicplayer.lanltw.playback

import com.mmc.merimusicplayer.data.model.lan.LanMessage
import com.mmc.merimusicplayer.lanltw.core.LanNtpEstimator

/** Absolute deadlines are consumed by the audio engine before the deadline, not a UI timer. */
interface LanScheduledAudioPort {
    fun audioDiagnostics(): String = "[]"
    val outputLatencyMs: Double
    suspend fun initializeAudio()
    suspend fun resetAudioClock()
    suspend fun receiveProbe(message: LanMessage, receivedAt: Double): LanNtpEstimator.Estimate?
    fun updateAudioClock(offsetMs: Double, receivedAt: Double) {}
    suspend fun scheduleAudio(message: LanMessage, offsetMs: Double, nudgeMs: Int): Boolean
    fun cancelAudioSchedule()
    fun changeAudioNudge(value: Int, delta: Int)
}
