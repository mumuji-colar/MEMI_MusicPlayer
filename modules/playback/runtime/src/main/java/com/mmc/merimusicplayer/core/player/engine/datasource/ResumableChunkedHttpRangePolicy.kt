@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.mmc.merimusicplayer.core.player.engine.datasource

import com.mmc.merimusicplayer.network.range.ResumableHttpRangeSupport
import android.net.Uri
import androidx.media3.datasource.DataSpec
import com.mmc.merimusicplayer.core.player.resolver.netease.normalizeNeteaseFlacResponseContentType
import com.mmc.merimusicplayer.core.player.resolver.netease.shouldUseNeteaseFlacResumableRange
import com.mmc.merimusicplayer.core.player.resolver.netease.validateNeteaseFlacRangeResponse
import com.mmc.merimusicplayer.platform.youtube.api.transport.range.YouTubeGoogleVideoRangeSupport

private const val NETEASE_FLAC_RANGE_BYTES = 8L * 1024L * 1024L

internal data class ResumableChunkedHttpRangePolicy(
    val shouldUse: (DataSpec) -> Boolean,
    val normalizeResponseHeaders: (
        uri: Uri,
        responseHeaders: Map<String, List<String>>
    ) -> Map<String, List<String>> = { _, responseHeaders -> responseHeaders },
    val validateResponse: (
        uri: Uri,
        responseCode: Int,
        responseHeaders: Map<String, List<String>>,
        requestedStartPosition: Long
    ) -> Unit = { _, _, _, _ -> },
    val initialChunkSizeOverride: (Uri) -> Long? = { null }
)

internal fun shouldUseResumableChunkedHttpRange(dataSpec: DataSpec): Boolean {
    return dataSpec.httpMethod == DataSpec.HTTP_METHOD_GET &&
        !ResumableHttpRangeSupport.hasExplicitRangeHeader(dataSpec.httpRequestHeaders) &&
        (
            YouTubeGoogleVideoRangeSupport.shouldUseChunkedRange(dataSpec.uri) ||
                shouldUseNeteaseFlacResumableRange(dataSpec.uri)
            )
}

internal val defaultResumableChunkedHttpRangePolicy = ResumableChunkedHttpRangePolicy(
    shouldUse = ::shouldUseResumableChunkedHttpRange,
    normalizeResponseHeaders = ::normalizeNeteaseFlacResponseContentType,
    validateResponse = ::validateNeteaseFlacRangeResponse,
    // 高码率 FLAC 用较大的范围减少等待响应的次数，响应体仍然边读边播
    initialChunkSizeOverride = { uri ->
        NETEASE_FLAC_RANGE_BYTES.takeIf { shouldUseNeteaseFlacResumableRange(uri) }
    }
)
