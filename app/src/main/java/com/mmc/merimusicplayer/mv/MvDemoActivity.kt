/*
 * MEMI輕音 / MemiPlayer - A modified build of MemiPlayer.
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
 * You should have received a copy of the GNU General Public License
 * along with this software.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * File: com.mmc.merimusicplayer.mv/MvDemoActivity
 */

package com.mmc.merimusicplayer.mv

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.SurfaceView
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * MV 播放的最小验证页：只验证「能不能播、播得对不对」，不接播放器状态机。
 *
 * 用它排除的问题：
 * 1. B 站 DASH 是音视频分离的，必须 MergingMediaSource 拼起来，直接播视频流会没声音；
 * 2. CDN 校验 Referer/UA，缺了就是 403；
 * 3. SurfaceView 与 Activity 生命周期的配合（切后台、旋转、返回释放）。
 *
 * 入口：adb shell am start -n com.mmc.merimusicplayer/com.mmc.merimusicplayer.mv.MvDemoActivity
 */
@UnstableApi
class MvDemoActivity : ComponentActivity() {

    private var player: ExoPlayer? = null
    private var videoSurface: SurfaceView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MvDemoScreen(
                        onPlay = ::play,
                        onRelease = { releasePlayer() },
                        attachSurface = { view ->
                            videoSurface = view
                            player?.setVideoSurfaceView(view)
                        }
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }

    private fun play(bvid: String, onStatus: (String) -> Unit) {
        releasePlayer()
        lifecycleScope.launch {
            onStatus("解析中…")
            val result = withContext(Dispatchers.IO) { runCatching { resolve(bvid) } }
            result.onFailure { error ->
                onStatus("解析失败：${error.message}")
            }.onSuccess { info ->
                onStatus("${info.title}  ${info.width}×${info.height}")
                val client = OkHttpClient.Builder()
                    .connectTimeout(8, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build()
                val dataSourceFactory = OkHttpDataSource.Factory(client)
                    .setDefaultRequestProperties(
                        mapOf(
                            "User-Agent" to UA,
                            "Referer" to "https://www.bilibili.com/"
                        )
                    )
                val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(info.videoUrl))
                val mediaSource = info.audioUrl?.let { audioUrl ->
                    MergingMediaSource(
                        videoSource,
                        ProgressiveMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(MediaItem.fromUri(audioUrl))
                    )
                } ?: videoSource

                player = ExoPlayer.Builder(this@MvDemoActivity).build().apply {
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_READY) {
                                onStatus("播放中 · ${info.width}×${info.height}")
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            onStatus(
                                "播放失败：${error.errorCodeName}" +
                                    " ${error.message.orEmpty().take(80)}"
                            )
                        }
                    })
                    videoSurface?.let { setVideoSurfaceView(it) }
                    setMediaSource(mediaSource)
                    prepare()
                    playWhenReady = true
                }
            }
        }
    }

    private data class Resolved(
        val title: String,
        val videoUrl: String,
        val audioUrl: String?,
        val width: Int,
        val height: Int
    )

    private fun resolve(bvid: String): Resolved {
        val client = OkHttpClient()
        fun get(url: String): JSONObject {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", UA)
                .header("Referer", "https://www.bilibili.com/")
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                require(response.isSuccessful && body.isNotBlank()) { "HTTP ${response.code}" }
                return JSONObject(body)
            }
        }

        val view = get("https://api.bilibili.com/x/web-interface/view?bvid=$bvid")
        require(view.optInt("code") == 0) { "view: ${view.optString("message")}" }
        val data = view.getJSONObject("data")
        val cid = data.getLong("cid")
        val title = data.optString("title")

        val play = get(
            "https://api.bilibili.com/x/player/playurl?bvid=$bvid&cid=$cid" +
                "&fnval=4048&fnver=0&fourk=1&qn=80"
        )
        require(play.optInt("code") == 0) { "playurl: ${play.optString("message")}" }
        val dash = play.getJSONObject("data").optJSONObject("dash")
            ?: error("没有 dash 流（可能需要登录或该视频不支持）")

        val videos = dash.optJSONArray("video") ?: error("没有视频流")
        var bestVideo: JSONObject? = null
        for (index in 0 until videos.length()) {
            val item = videos.getJSONObject(index)
            val height = item.optInt("height")
            if (height > 1080) continue
            if (bestVideo == null || height > bestVideo!!.optInt("height")) bestVideo = item
        }
        val video = bestVideo ?: error("没有可用视频流")

        val audios = dash.optJSONArray("audio")
        var bestAudio: JSONObject? = null
        if (audios != null) {
            for (index in 0 until audios.length()) {
                val item = audios.getJSONObject(index)
                if (bestAudio == null || item.optInt("bandwidth") > bestAudio!!.optInt("bandwidth")) {
                    bestAudio = item
                }
            }
        }

        return Resolved(
            title = title,
            videoUrl = video.getString("baseUrl"),
            audioUrl = bestAudio?.getString("baseUrl"),
            width = video.optInt("width"),
            height = video.optInt("height")
        )
    }

    companion object {
        /**
         * 必须用桌面 UA：playurl 返回的 CDN 地址带 `platform=pc`，节点会按 UA 校验，
         * 移动端 UA（含 "Mobile"）直接 403 openresty 页，表现就是 ERROR_CODE_IO_BAD_HTTP_STATUS。
         */
        private const val UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Safari/537.36"
    }
}

@Composable
private fun MvDemoScreen(
    onPlay: (String, (String) -> Unit) -> Unit,
    onRelease: () -> Unit,
    attachSurface: (SurfaceView) -> Unit
) {
    var bvid by remember { mutableStateOf("BV1GJ411x7h7") }
    var status by remember { mutableStateOf("验证页：填 BV 号 → 解析播放（默认值可直接试）") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = bvid,
                onValueChange = { bvid = it.trim() },
                singleLine = true,
                label = { Text("BV 号") },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { onPlay(bvid) { status = it } }) { Text("解析播放") }
            Button(onClick = onRelease) { Text("停止") }
        }
        Text(status, style = MaterialTheme.typography.bodySmall)
        AndroidView(
            factory = { context ->
                SurfaceView(context).also { surface ->
                    attachSurface(surface)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        DisposableEffect(Unit) {
            onDispose { onRelease() }
        }
    }
}
