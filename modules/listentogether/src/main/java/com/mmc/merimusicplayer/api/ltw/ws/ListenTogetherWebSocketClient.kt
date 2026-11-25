package com.mmc.merimusicplayer.api.ltw.ws

import android.os.SystemClock
import com.mmc.merimusicplayer.listentogether.protocol.ListenTogetherSocketCodec
import com.mmc.merimusicplayer.listentogether.protocol.ListenTogetherSocketMessageTooLargeException
import com.mmc.merimusicplayer.data.model.ltw.message.event.ListenTogetherEvent
import com.mmc.merimusicplayer.data.model.ltw.session.ListenTogetherConnectionState
import com.mmc.merimusicplayer.data.model.ltw.message.socket.ListenTogetherSocketEnvelope
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.atomic.AtomicLong

const val LISTEN_TOGETHER_SOCKET_RESPONSE_TIMEOUT_MS = 35_000L

fun shouldReconnectListenTogetherSocket(
    reconnectEnabled: Boolean,
    connectionState: ListenTogetherConnectionState,
    lastMessageAtElapsedMs: Long,
    lastPingSentAtElapsedMs: Long,
    nowElapsedMs: Long,
    responseTimeoutMs: Long = LISTEN_TOGETHER_SOCKET_RESPONSE_TIMEOUT_MS
): Boolean {
    if (!reconnectEnabled || connectionState != ListenTogetherConnectionState.CONNECTED) {
        return false
    }
    if (lastPingSentAtElapsedMs <= 0L || nowElapsedMs < lastPingSentAtElapsedMs) {
        return false
    }
    if (nowElapsedMs - lastPingSentAtElapsedMs < responseTimeoutMs.coerceAtLeast(0L)) {
        return false
    }
    return lastMessageAtElapsedMs <= lastPingSentAtElapsedMs
}

class ListenTogetherWebSocketClient(
    private val okHttpClient: OkHttpClient
) {
    private val codec = ListenTogetherSocketCodec()
    private val lock = Any()

    // 用代次而不是 socket 对象身份来判定回调归属。
    // connect() 里 newWebSocket 返回之前回调就可能到达, 那一刻 webSocket 还没赋值,
    // 身份比较会把合法的 onOpen 丢掉, 连接就永远停在 CONNECTING 状态。
    private val generation = AtomicLong(0L)

    @Volatile
    private var webSocket: WebSocket? = null

    fun connect(
        wsUrl: String,
        listener: Listener
    ) {
        val token = synchronized(lock) {
            webSocket?.close(1000, "client_closed")
            webSocket = null
            generation.incrementAndGet()
        }
        val request = Request.Builder().url(wsUrl).build()
        val socket = okHttpClient.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    if (!isCurrent(token)) return
                    listener.onOpen()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (!isCurrent(token)) return
                    runCatching { codec.decodeEnvelope(text) }
                        .onSuccess(listener::onMessage)
                        .onFailure {
                            if (it is ListenTogetherSocketMessageTooLargeException) {
                                listener.onProtocolError(text.take(256), it)
                                disconnect(code = 1009, reason = "message_too_large")
                            } else {
                                listener.onProtocolError(text, it)
                            }
                        }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (!isCurrent(token)) return
                    listener.onClosed(code, reason)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (!isCurrent(token)) return
                    val message = buildString {
                        append(t.message ?: t.javaClass.simpleName)
                        response?.let {
                            append(" (http=")
                            append(it.code)
                            append(' ')
                            append(it.message)
                            append(')')
                        }
                    }
                    listener.onFailure(IllegalStateException(message, t))
                }
            }
        )
        val accepted = synchronized(lock) {
            if (generation.get() == token) {
                webSocket = socket
                true
            } else {
                false
            }
        }
        if (!accepted) socket.close(1000, "stale_connection")
    }

    private fun isCurrent(token: Long): Boolean = generation.get() == token

    // 与 connect/disconnect 共用同一把锁, 否则建立连接期间的发送会读到中间状态
    fun sendEvent(event: ListenTogetherEvent): Boolean = synchronized(lock) {
        webSocket?.send(codec.encodeEvent(event)) == true
    }

    fun sendPing(sentAtElapsedMs: Long = SystemClock.elapsedRealtime()): Boolean = synchronized(lock) {
        webSocket?.send(codec.encodePing(sentAtElapsedMs)) == true
    }

    fun sendLegacyPing(): Boolean = synchronized(lock) {
        webSocket?.send(codec.encodeLegacyPing()) == true
    }

    fun disconnect(code: Int = 1000, reason: String = "client_closed") {
        val closing = synchronized(lock) {
            generation.incrementAndGet()
            webSocket.also { webSocket = null }
        }
        closing?.close(code, reason)
    }

    interface Listener {
        fun onOpen()
        fun onMessage(message: ListenTogetherSocketEnvelope)
        fun onClosed(code: Int, reason: String)
        fun onFailure(error: Throwable)
        fun onProtocolError(rawText: String, error: Throwable)
    }
}
