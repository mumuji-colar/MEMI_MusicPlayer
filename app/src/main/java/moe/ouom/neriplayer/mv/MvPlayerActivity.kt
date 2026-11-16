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

import android.content.Context
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
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
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import kotlin.math.max
import kotlin.math.min
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
import moe.ouom.neriplayer.ui.theme.NeriMochiTheme
import moe.ouom.neriplayer.util.format.formatDuration
import ui.feice.mochi.basic.MochiSlider
import ui.feice.mochi.basic.MochiSliderDefaults

internal enum class MvPhase { Loading, Ready, Failed }

/** 画面填充方式：真正的比例由视频自身决定，这里只决定多出来的空间怎么处理。 */
internal enum class MvScaleMode(val storageValue: String, val label: String) {
    /** 完整显示，留黑边。 */
    Fit("fit", "适应屏幕"),

    /** 铺满并裁掉溢出部分。 */
    Crop("crop", "裁剪铺满"),

    /** 拉伸铺满，不保持比例。 */
    Fill("fill", "拉伸铺满");

    companion object {
        fun fromStorage(value: String?): MvScaleMode =
            entries.firstOrNull { it.storageValue == value } ?: Fit
    }
}

internal data class MvQualityOption(val height: Int, val label: String)

internal data class MvUiState(
    val phase: MvPhase = MvPhase.Loading,
    val message: String = "",
    val title: String = "",
    val subtitle: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val aspectRatio: Float = 0f,
    val scaleMode: MvScaleMode = MvScaleMode.Fit,
    val qualities: List<MvQualityOption> = emptyList(),
    val selectedHeight: Int = 0,
    val codecLabel: String = ""
)

/** MV 页自己的偏好：画面模式与上次选的清晰度，都跟当前账号无关，存在本地就行。 */
internal object MvPlaybackPrefs {
    private const val FILE_NAME = "mv_player_prefs"
    private const val KEY_SCALE_MODE = "scale_mode"
    private const val KEY_QUALITY_HEIGHT = "quality_height"

    fun scaleMode(context: Context): MvScaleMode =
        MvScaleMode.fromStorage(prefs(context).getString(KEY_SCALE_MODE, null))

    fun setScaleMode(context: Context, mode: MvScaleMode) {
        prefs(context).edit().putString(KEY_SCALE_MODE, mode.storageValue).apply()
    }

    fun qualityHeight(context: Context): Int = prefs(context).getInt(KEY_QUALITY_HEIGHT, 0)

    fun setQualityHeight(context: Context, height: Int) {
        prefs(context).edit().putInt(KEY_QUALITY_HEIGHT, height).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
}

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

    /** 当前档位下标（对应 [MvStream.variants]）。 */
    private var variantIndex = 0

    /** 当前档位里正在用的备用地址下标。 */
    private var urlIndex = 0

    /** 用户上次选的清晰度，0 表示没选过。 */
    private var preferredHeight = 0

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

        preferredHeight = MvPlaybackPrefs.qualityHeight(this)
        ui.update {
            it.copy(
                title = target.title,
                subtitle = target.artist,
                scaleMode = MvPlaybackPrefs.scaleMode(this)
            )
        }

        // 进入 MV 前先让主播放器让位，避免两路音频打架
        resumeAfterExit = PlayerManager.isPlayingFlow.value
        if (resumeAfterExit) runCatching { PlayerManager.pause() }

        setContent {
            // MV 页永远是纯黑底，这里给 Mochi 组件（进度条）一套深色主题，
            // 别让它退回到默认浅色配色
            val darkScheme = darkColorScheme()
            MaterialTheme(colorScheme = darkScheme) {
                NeriMochiTheme(colorScheme = darkScheme) {
                    MvPlayerScreen(
                        state = ui.collectAsStateWithLifecycle().value,
                        onClose = { finish() },
                        onTogglePlay = ::togglePlay,
                        onSeek = ::seekTo,
                        onRetry = { startPlayback(target) },
                        onSelectQuality = ::selectQuality,
                        onSelectScaleMode = ::setScaleMode,
                        onSurfaceReady = { view ->
                            videoSurface = view
                            player?.setVideoSurfaceView(view)
                        }
                    )
                }
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
        variantIndex = 0
        urlIndex = 0
        ui.update { it.copy(phase = MvPhase.Loading, message = "解析中…", qualities = emptyList()) }
        lifecycleScope.launch {
            val result = runCatching { resolveMvStream(target) }
            result.onFailure { error ->
                ui.update {
                    it.copy(
                        phase = MvPhase.Failed,
                        message = error.message ?: "解析失败"
                    )
                }
            }.onSuccess { resolved -> applyStream(resolved) }
        }
    }

    private suspend fun resolveMvStream(target: MvTarget): MvStream = when (target.platform) {
        MvPlatform.BILIBILI -> resolveBiliMvStream(AppContainer.biliClient, target)
        MvPlatform.NETEASE -> resolveNeteaseMvStream(AppContainer.neteaseClient, target)
    }

    private fun applyStream(resolved: MvStream) {
        if (resolved.variants.isEmpty()) {
            ui.update { it.copy(phase = MvPhase.Failed, message = "没有可用的视频流") }
            return
        }
        stream = resolved
        variantIndex = resolved.variantIndexFor(preferredHeight)
        urlIndex = 0
        val variant = resolved.variants[variantIndex]
        ui.update {
            it.copy(
                phase = MvPhase.Loading,
                message = "加载中…",
                qualities = resolved.variants.map { item -> MvQualityOption(item.height, item.label) },
                selectedHeight = variant.height,
                codecLabel = variant.codecLabel,
                durationMs = resolved.durationMs ?: it.durationMs
            )
        }
        buildPlayer(resolved, variantIndex, 0L, true)
    }

    private fun buildPlayer(
        resolved: MvStream,
        index: Int,
        startPositionMs: Long,
        autoPlay: Boolean
    ) {
        val variant = resolved.variants.getOrNull(index)
        val videoUrl = variant?.videoUrls?.getOrNull(urlIndex) ?: variant?.videoUrls?.firstOrNull()
        if (variant == null || videoUrl == null) {
            ui.update { it.copy(phase = MvPhase.Failed, message = "没有可用的视频流") }
            return
        }
        // 音轨数量可能与视频不一致，缺了就用第一条
        val audioUrl = variant.audioUrls.getOrNull(urlIndex) ?: variant.audioUrls.firstOrNull()

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
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    val height = videoSize.height
                    if (height <= 0 || videoSize.width <= 0) return
                    val ratio = videoSize.width * videoSize.pixelWidthHeightRatio / height
                    if (ratio > 0f) ui.update { it.copy(aspectRatio = ratio) }
                }

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
                    tryNextSource(resolved, index, currentPosition, isPlaying, error)
                }
            })
            videoSurface?.let { setVideoSurfaceView(it) }
            setMediaSource(mediaSource)
            if (startPositionMs > 0L) seekTo(startPositionMs)
            prepare()
            playWhenReady = autoPlay
        }
        startProgressLoop()
    }

    /** PCDN 节点会抽风，每条流都带备用地址，失败就换一个再试。 */
    private fun tryNextSource(
        resolved: MvStream,
        index: Int,
        positionMs: Long,
        autoPlay: Boolean,
        error: PlaybackException
    ) {
        val sourceCount = resolved.variants.getOrNull(index)?.videoUrls?.size ?: 0
        if (urlIndex + 1 < sourceCount) {
            urlIndex += 1
            ui.update { it.copy(phase = MvPhase.Loading, message = "换备用地址重试…") }
            // 不能在播放器回调栈里直接 release 自己，推到下一帧
            window.decorView.post {
                if (!isFinishing && !isDestroyed) {
                    buildPlayer(resolved, index, positionMs, autoPlay)
                }
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

    /** 切清晰度：同一首 MV 的档位在解析时就全拿到了，这里只换流、不重新请求。 */
    private fun selectQuality(height: Int) {
        val resolved = stream ?: return
        val index = resolved.variants.indexOfFirst { it.height == height }
        if (index < 0 || index == variantIndex) return
        val position = player?.currentPosition ?: 0L
        val playing = player?.isPlaying ?: true
        preferredHeight = height
        MvPlaybackPrefs.setQualityHeight(this, height)
        variantIndex = index
        urlIndex = 0
        val variant = resolved.variants[index]
        ui.update {
            it.copy(
                phase = MvPhase.Loading,
                message = "切换清晰度…",
                selectedHeight = variant.height,
                codecLabel = variant.codecLabel
            )
        }
        buildPlayer(resolved, index, position, playing)
    }

    private fun setScaleMode(mode: MvScaleMode) {
        MvPlaybackPrefs.setScaleMode(this, mode)
        ui.update { it.copy(scaleMode = mode) }
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
    onSelectQuality: (Int) -> Unit,
    onSelectScaleMode: (MvScaleMode) -> Unit,
    onSurfaceReady: (SurfaceView) -> Unit
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var settingsVisible by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (settingsVisible) settingsVisible = false else onClose()
    }

    LaunchedEffect(controlsVisible, settingsVisible, state.isPlaying, state.phase) {
        if (controlsVisible && !settingsVisible && state.phase == MvPhase.Ready && state.isPlaying) {
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
                ) {
                    if (settingsVisible) {
                        settingsVisible = false
                    } else {
                        controlsVisible = !controlsVisible
                    }
                }
        ) {
            MvVideoLayer(state = state, onSurfaceReady = onSurfaceReady)

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
                    TopBar(
                        state = state,
                        settingsOpen = settingsVisible,
                        onSettings = {
                            settingsVisible = !settingsVisible
                            controlsVisible = true
                        },
                        onClose = onClose,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
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

            AnimatedVisibility(
                visible = settingsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 62.dp, end = 14.dp)
            ) {
                MvSettingsPanel(
                    state = state,
                    onSelectQuality = onSelectQuality,
                    onSelectScaleMode = onSelectScaleMode
                )
            }
        }
    }
}

/**
 * 画面层：按 [MvUiState.scaleMode] 决定 SurfaceView 的实际尺寸。
 *
 * SurfaceView 的画面总是铺满自身，所以「适应」只需把尺寸算成视频比例，
 * 「裁剪」则把尺寸放大到能盖住容器，再让外层裁掉溢出部分。
 */
@Composable
private fun MvVideoLayer(state: MvUiState, onSurfaceReady: (SurfaceView) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()
        val aspect = state.aspectRatio

        val videoModifier = if (aspect <= 0f || state.scaleMode == MvScaleMode.Fill) {
            Modifier.fillMaxSize()
        } else {
            val fitWidth = min(containerWidth, containerHeight * aspect)
            val fitHeight = fitWidth / aspect
            val scale = if (state.scaleMode == MvScaleMode.Crop) {
                max(containerWidth / fitWidth, containerHeight / fitHeight)
            } else {
                1f
            }
            Modifier.requiredSize(
                width = with(density) { (fitWidth * scale).toDp() },
                height = with(density) { (fitHeight * scale).toDp() }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { context -> SurfaceView(context).also(onSurfaceReady) },
                modifier = videoModifier
            )
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
private fun TopBar(
    state: MvUiState,
    settingsOpen: Boolean,
    onSettings: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            val meta = listOfNotNull(
                state.subtitle.takeIf { it.isNotBlank() },
                state.qualities.firstOrNull { it.height == state.selectedHeight }?.label,
                state.codecLabel.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    color = Color.White.copy(alpha = 0.62f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = onSettings) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "MV 设置",
                tint = if (settingsOpen) Color.White else Color.White.copy(alpha = 0.82f)
            )
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = "关闭 MV", tint = Color.White)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MvSettingsPanel(
    state: MvUiState,
    onSelectQuality: (Int) -> Unit,
    onSelectScaleMode: (MvScaleMode) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.82f),
        modifier = Modifier
            .width(300.dp)
            // 吃掉点击，别让「点面板空白处」当成点画面而把面板关掉
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {}
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "清晰度",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelMedium
            )
            if (state.qualities.isEmpty()) {
                Text(
                    text = "解析中…",
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.qualities.forEach { option ->
                        MvOptionChip(
                            text = option.label,
                            selected = option.height == state.selectedHeight,
                            onClick = { onSelectQuality(option.height) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(2.dp))
            Text(
                text = "画面",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelMedium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MvScaleMode.entries.forEach { mode ->
                    MvOptionChip(
                        text = mode.label,
                        selected = mode == state.scaleMode,
                        onClick = { onSelectScaleMode(mode) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MvOptionChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.12f),
        modifier = Modifier
            .clipToBounds()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Text(
            text = text,
            color = if (selected) Color.Black.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.86f),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
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
            // 胶囊轨道 + 胶囊滑块，和 App 其他页面的 Mochi 滑块保持同一套手感
            MochiSlider(
                value = sliderValue,
                onValueChange = { dragPosition = it },
                onValueChangeFinished = {
                    dragPosition?.let { onSeek(it.toLong()) }
                    dragPosition = null
                },
                valueRange = 0f..duration.toFloat(),
                colors = MochiSliderDefaults.colors(
                    Color.White.copy(alpha = 0.92f),
                    Color.White.copy(alpha = 0.26f),
                    Color.White,
                    Color.Black.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            )
            Text(
                text = formatDuration(duration),
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
