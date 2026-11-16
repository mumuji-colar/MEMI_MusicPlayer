/*
 * MEMI輕音 / MemiPlayer - A modified build of NeriPlayer.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * Copyright (C) 2026 MMC
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * File: moe.ouom.neriplayer.mv/MvPlayerActivity
 */

package moe.ouom.neriplayer.mv

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.SurfaceView
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.core.player.PlayerManager
import moe.ouom.neriplayer.data.model.mv.MvPlatform
import moe.ouom.neriplayer.data.model.mv.MvStream
import moe.ouom.neriplayer.data.model.mv.MvTarget
import moe.ouom.neriplayer.platform.bilibili.playback.mv.resolveBiliMvStream
import moe.ouom.neriplayer.platform.netease.mv.resolveNeteaseMvStream
import moe.ouom.neriplayer.util.format.formatDuration

internal enum class MvPhase { Loading, Ready, Failed }

internal data class MvUiState(
    val phase: MvPhase = MvPhase.Loading,
    val message: String = "",
    val title: String = "",
    val subtitle: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)

/**
 * MV 全屏播放页。
 *
 * 设计取舍：MV 不用主播放器，而是独立的 ExoPlayer 实例 + SurfaceView。
 * 主播放器在这期间保持暂停，退出时按进入前的状态决定是否续播——
 * 这样不必去动播放队列和音频焦点状态机，风险最低。
 */
@UnstableApi
class MvPlayerActivity : ComponentActivity() {

    private var player: ExoPlayer? = null
    private var videoSurface: SurfaceView? = null
    private var resumeAfterExit = false
    private var progressJob: Job? = null
    private var stream: MvStream? = null
    private var urlIndex = 0

    private val ui = MutableStateFlow(MvUiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        val target = intent.mvTargetOrNull()
        if (target == null) {
            finish()
            return
        }

        ui.update { it.copy(title = target.title, subtitle = target.artist) }

        // 进入 MV 前先让主播放器让位，避免两路音频打架
        resumeAfterExit = PlayerManager.isPlayingFlow.value
        if (resumeAfterExit) runCatching { PlayerManager.pause() }

        setContent {
            MaterialTheme {
                MvPlayerScreen(
                    state = ui.collectAsStateWithLifecycle().value,
                    onClose = { finish() },
                    onTogglePlay = ::togglePlay,
                    onSeek = ::seekTo,
                    onRetry = { startPlayback(target) },
                    onSurfaceReady = { view ->
                        videoSurface = view
                        player?.setVideoSurfaceView(view)
                    }
                )
            }
        }

        startPlayback(target)
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        progressJob?.cancel()
        releasePlayer()
        if (resumeAfterExit) runCatching { PlayerManager.play() }
        super.onDestroy()
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun startPlayback(target: MvTarget) {
        releasePlayer()
        urlIndex = 0
        ui.update { it.copy(phase = MvPhase.Loading, message = "解析中…") }
        lifecycleScope.launch {
            val result = runCatching { resolveMvStream(target) }
            result.onFailure { error ->
                ui.update {
                    it.copy(
                        phase = MvPhase.Failed,
                        message = error.message ?: "解析失败"
                    )
                }
            }.onSuccess { resolved ->
                stream = resolved
                buildPlayer(resolved, 0)
            }
        }
    }

    private suspend fun resolveMvStream(target: MvTarget): MvStream = when (target.platform) {
        MvPlatform.BILIBILI -> resolveBiliMvStream(AppContainer.biliClient, target)
        MvPlatform.NETEASE -> resolveNeteaseMvStream(AppContainer.neteaseClient, target)
    }

    private fun buildPlayer(resolved: MvStream, index: Int) {
        val videoUrl = resolved.videoUrls.getOrNull(index)
        if (videoUrl == null) {
            ui.update { it.copy(phase = MvPhase.Failed, message = "没有可用的视频流") }
            return
        }
        // 音轨数量可能与视频不一致，缺了就用第一条
        val audioUrl = resolved.audioUrls.getOrNull(index) ?: resolved.audioUrls.firstOrNull()

        releasePlayer()

        val factory = mvDataSourceFactory(resolved.headers)
        val videoSource = ProgressiveMediaSource.Factory(factory)
            .createMediaSource(MediaItem.fromUri(videoUrl))
        val mediaSource = if (audioUrl != null) {
            MergingMediaSource(
                videoSource,
                ProgressiveMediaSource.Factory(factory)
                    .createMediaSource(MediaItem.fromUri(audioUrl))
            )
        } else {
            videoSource
        }

        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> ui.update {
                            it.copy(
                                phase = MvPhase.Ready,
                                isPlaying = isPlaying,
                                durationMs = duration
                                    .takeIf { value -> value > 0L }
                                    ?: it.durationMs
                            )
                        }

                        Player.STATE_ENDED -> ui.update {
                            it.copy(isPlaying = false, positionMs = it.durationMs)
                        }

                        else -> Unit
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    tryNextSource(resolved, error)
                }
            })
            videoSurface?.let { setVideoSurfaceView(it) }
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
        }
        startProgressLoop()
    }

    /** PCDN 节点会抽风，dash 里每个流都带备用地址，失败就换一个再试。 */
    private fun tryNextSource(resolved: MvStream, error: PlaybackException) {
        val sourceCount = maxOf(resolved.videoUrls.size, resolved.audioUrls.size)
        if (urlIndex + 1 < sourceCount) {
            urlIndex += 1
            ui.update { it.copy(phase = MvPhase.Loading, message = "换备用地址重试…") }
            // 不能在播放器回调栈里直接 release 自己，推到下一帧
            window.decorView.post {
                if (!isFinishing && !isDestroyed) buildPlayer(resolved, urlIndex)
            }
            return
        }
        ui.update {
            it.copy(
                phase = MvPhase.Failed,
                message = "${error.errorCodeName} ${error.message.orEmpty().take(80)}".trim()
            )
        }
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = lifecycleScope.launch {
            while (isActive) {
                player?.let { active ->
                    val duration = active.duration.takeIf { it > 0L }
                    ui.update {
                        it.copy(
                            isPlaying = active.isPlaying,
                            positionMs = active.currentPosition.coerceAtLeast(0L),
                            durationMs = duration ?: it.durationMs
                        )
                    }
                }
                delay(400)
            }
        }
    }

    private fun togglePlay() {
        val active = player ?: return
        if (active.isPlaying) active.pause() else active.play()
        ui.update { it.copy(isPlaying = active.isPlaying) }
    }

    private fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
        ui.update { it.copy(positionMs = positionMs) }
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }

    private fun Intent.mvTargetOrNull(): MvTarget? {
        @Suppress("DEPRECATION")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(EXTRA_TARGET, MvTarget::class.java)
        } else {
            getParcelableExtra(EXTRA_TARGET)
        }
    }

    companion object {
        internal const val EXTRA_TARGET = "moe.ouom.neriplayer.mv.extra.TARGET"
    }
}

@Composable
private fun MvPlayerScreen(
    state: MvUiState,
    onClose: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onRetry: () -> Unit,
    onSurfaceReady: (SurfaceView) -> Unit
) {
    var controlsVisible by remember { mutableStateOf(true) }

    BackHandler(enabled = true, onBack = onClose)

    LaunchedEffect(controlsVisible, state.isPlaying, state.phase) {
        if (controlsVisible && state.phase == MvPhase.Ready && state.isPlaying) {
            delay(3_200)
            controlsVisible = false
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { controlsVisible = !controlsVisible }
        ) {
            AndroidView(
                factory = { context ->
                    SurfaceView(context).also(onSurfaceReady)
                },
                modifier = Modifier.fillMaxSize()
            )

            if (state.phase == MvPhase.Loading) {
                LoadingOverlay(state.message)
            }
            if (state.phase == MvPhase.Failed) {
                ErrorOverlay(state.message, onRetry, onClose)
            }

            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(Modifier.fillMaxSize()) {
                    TopBar(state, onClose, Modifier.align(Alignment.TopCenter))
                    if (state.phase == MvPhase.Ready) {
                        BottomBar(
                            state = state,
                            onTogglePlay = onTogglePlay,
                            onSeek = onSeek,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingOverlay(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = message.ifBlank { "解析中…" },
            color = Color.White.copy(alpha = 0.75f),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ErrorOverlay(message: String, onRetry: () -> Unit, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "MV 打不开",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            color = Color.White.copy(alpha = 0.72f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onRetry) { Text("重试") }
            TextButton(onClick = onClose) { Text("返回") }
        }
    }
}

@Composable
private fun TopBar(state: MvUiState, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.62f), Color.Transparent)
                )
            )
            .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = state.title.ifBlank { "MV" },
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (state.subtitle.isNotBlank()) {
                Text(
                    text = state.subtitle,
                    color = Color.White.copy(alpha = 0.62f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = "关闭 MV", tint = Color.White)
        }
    }
}

@Composable
private fun BottomBar(
    state: MvUiState,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragPosition by remember { mutableStateOf<Float?>(null) }
    val duration = state.durationMs.coerceAtLeast(1L)
    val sliderValue = (dragPosition ?: state.positionMs.toFloat()).coerceIn(0f, duration.toFloat())

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                )
            )
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.16f),
                modifier = Modifier.size(44.dp)
            ) {
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        imageVector = if (state.isPlaying) {
                            Icons.Filled.Pause
                        } else {
                            Icons.Filled.PlayArrow
                        },
                        contentDescription = if (state.isPlaying) "暂停" else "播放",
                        tint = Color.White
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = formatDuration(state.positionMs),
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelMedium
            )
            Slider(
                value = sliderValue,
                onValueChange = { dragPosition = it },
                onValueChangeFinished = {
                    dragPosition?.let { onSeek(it.toLong()) }
                    dragPosition = null
                },
                valueRange = 0f..duration.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White.copy(alpha = 0.9f),
                    inactiveTrackColor = Color.White.copy(alpha = 0.28f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            Text(
                text = formatDuration(duration),
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
