package com.mmc.merimusicplayer.api.ltw.http

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.mmc.merimusicplayer.listentogether.protocol.listenTogetherProtocolJson
import com.mmc.merimusicplayer.listentogether.protocol.readListenTogetherResponse
import com.mmc.merimusicplayer.data.model.ltw.ListenTogetherServerTestResult
import com.mmc.merimusicplayer.data.model.ltw.message.http.ListenTogetherControlResponse
import com.mmc.merimusicplayer.data.model.ltw.message.http.ListenTogetherCreateRoomRequest
import com.mmc.merimusicplayer.data.model.ltw.message.event.ListenTogetherEvent
import com.mmc.merimusicplayer.data.model.ltw.message.http.ListenTogetherInitialSnapshot
import com.mmc.merimusicplayer.data.model.ltw.message.http.ListenTogetherJoinRoomRequest
import com.mmc.merimusicplayer.data.model.ltw.message.http.ListenTogetherLeaveRoomRequest
import com.mmc.merimusicplayer.data.model.ltw.message.http.ListenTogetherLeaveRoomResponse
import com.mmc.merimusicplayer.data.model.ltw.message.http.ListenTogetherRoomResponse
import com.mmc.merimusicplayer.data.model.ltw.message.http.ListenTogetherStateResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class ListenTogetherApi(
    private val okHttpClient: OkHttpClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val json = listenTogetherProtocolJson()

    suspend fun createRoom(
        baseUrl: String,
        userUuid: String,
        nickname: String,
        initialSnapshot: ListenTogetherInitialSnapshot
    ): ListenTogetherRoomResponse {
        return post(
            url = "${baseUrl.normalizeBaseUrl()}/api/rooms",
            body = ListenTogetherCreateRoomRequest(
                userUuid = userUuid,
                nickname = nickname,
                initialSnapshot = initialSnapshot
            )
        )
    }

    suspend fun joinRoom(
        baseUrl: String,
        roomId: String,
        userUuid: String,
        nickname: String,
        memberSecret: String? = null,
        joinSecret: String? = null,
        bearerToken: String? = null
    ): ListenTogetherRoomResponse {
        return post(
            url = "${baseUrl.normalizeBaseUrl()}/api/rooms/$roomId/join",
            body = ListenTogetherJoinRoomRequest(
                userUuid = userUuid,
                nickname = nickname,
                memberSecret = memberSecret,
                joinSecret = joinSecret
            ),
            bearerToken = bearerToken
        )
    }

    suspend fun getRoomState(
        baseUrl: String,
        roomId: String,
        bearerToken: String? = null
    ): ListenTogetherStateResponse {
        return get(
            url = "${baseUrl.normalizeBaseUrl()}/api/rooms/$roomId/state",
            bearerToken = bearerToken
        )
    }

    suspend fun leaveRoom(
        baseUrl: String,
        roomId: String,
        token: String
    ): ListenTogetherLeaveRoomResponse {
        return post(
            url = "${baseUrl.normalizeBaseUrl()}/api/rooms/$roomId/leave",
            body = ListenTogetherLeaveRoomRequest,
            bearerToken = token
        )
    }

    suspend fun sendControlEvent(
        baseUrl: String,
        roomId: String,
        token: String,
        event: ListenTogetherEvent
    ): ListenTogetherControlResponse {
        return post(
            url = "${baseUrl.normalizeBaseUrl()}/api/rooms/$roomId/control",
            body = event,
            bearerToken = token
        )
    }

    suspend fun testServerAvailability(baseUrl: String): ListenTogetherServerTestResult = withContext(ioDispatcher) {
        val normalizedBaseUrl = baseUrl.normalizedHttpBaseUrlOrNull()
            ?: return@withContext ListenTogetherServerTestResult(
                ok = false,
                message = "invalid_base_url"
            )
        val request = Request.Builder()
            .url("$normalizedBaseUrl/healthz")
            .get()
            .build()
        runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body.limitedString()
                val looksLikeListenTogetherService = body.contains(
                    "memiplayer-listen-together-worker",
                    ignoreCase = true
                )
                if (looksLikeListenTogetherService) {
                    ListenTogetherServerTestResult(
                        ok = true,
                        message = "reachable"
                    )
                } else {
                    ListenTogetherServerTestResult(
                        ok = false,
                        message = "invalid_response"
                    )
                }
            }
        }.getOrElse {
            ListenTogetherServerTestResult(
                ok = false,
                message = it.message ?: it.javaClass.simpleName
            )
        }
    }

    private suspend inline fun <reified T> get(
        url: String,
        bearerToken: String? = null
    ): T = withContext(ioDispatcher) {
        val requestBuilder = Request.Builder()
            .url(url)
            .get()
        bearerToken?.takeIf { it.isNotBlank() }?.let {
            requestBuilder.header("Authorization", "Bearer $it")
        }
        val request = requestBuilder.build()
        okHttpClient.newCall(request).execute().use { response ->
            val body = response.body.limitedString()
            if (!response.isSuccessful) {
                throw IOException("ListenTogether GET failed (${response.code}): $body")
            }
            json.decodeFromString(body)
        }
    }

    private suspend inline fun <reified RequestBodyT, reified ResponseT> post(
        url: String,
        body: RequestBodyT,
        bearerToken: String? = null
    ): ResponseT = withContext(ioDispatcher) {
        val requestBuilder = Request.Builder()
            .url(url)
            .post(
                json.encodeToString(body)
                    .toRequestBody("application/json; charset=utf-8".toMediaType())
            )
        bearerToken?.takeIf { it.isNotBlank() }?.let {
            requestBuilder.header("Authorization", "Bearer $it")
        }
        okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
            val responseBody = response.body.limitedString()
            if (!response.isSuccessful) {
                throw IOException("ListenTogether POST failed (${response.code}): $responseBody")
            }
            json.decodeFromString(responseBody)
        }
    }
}

private fun ResponseBody.limitedString(): String {
    return readListenTogetherResponse(
        input = byteStream(),
        contentLength = contentLength(),
        charset = contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
    )
}
