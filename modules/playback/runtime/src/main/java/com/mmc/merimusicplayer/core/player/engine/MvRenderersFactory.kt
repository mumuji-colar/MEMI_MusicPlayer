package com.mmc.merimusicplayer.core.player.engine

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
 * File: com.mmc.merimusicplayer.core.player.engine/MvRenderersFactory
 */

import android.content.Context
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.mmc.merimusicplayer.core.player.audio.processing.StereoBalanceAudioProcessor
import com.mmc.merimusicplayer.core.player.audio.processing.VolumeNormalizationAudioProcessor

/**
 * MV 播放页专用的渲染器工厂。
 *
 * MV 用的是独立于主播放队列的 ExoPlayer 实例，默认的音频链里没有主播放器的处理器，
 * 所以响度均衡与声道平衡在 MV 里会直接失效。这里把这两级挂到 MV 的音频链上，
 * 读的是同一套全局状态（[com.mmc.merimusicplayer.core.player.audio.processing.PlaybackVolumeNormalizationState]
 * 与 [com.mmc.merimusicplayer.core.player.audio.processing.PlaybackVolumeBalanceState]），
 * 因此设置页的开关和滑块对 MV 同样即时生效。
 *
 * 有意不挂的：
 * - [TeeAudioProcessor] 与可视化：MV 页没有频谱动画，白跑一遍 PCM 浪费 CPU
 * - DSP 音效链（均衡器/低音增强等）：那套依赖 [com.mmc.merimusicplayer.core.player.audio.effects.AudioEffectsAudioSink]
 *   与全局音效激活状态，套到 MV 上收益小、风险大，先只对齐"音量类"处理
 */
@UnstableApi
class MvRenderersFactory(context: Context) : DefaultRenderersFactory(context) {

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean
    ): AudioSink {
        val processors = arrayOf<AudioProcessor>(
            VolumeNormalizationAudioProcessor(),
            StereoBalanceAudioProcessor()
        )
        return DefaultAudioSink.Builder(context)
            .setAudioProcessors(processors)
            .setEnableFloatOutput(enableFloatOutput)
            // 与主播放器保持一致：极低倍速下走平台 PlaybackParams 会有明显失真
            .setEnableAudioOutputPlaybackParameters(false)
            .build()
    }
}
