package com.clipbridge.app

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class WebSocketClient(
    private val deviceId: String,
    private val listener: Listener,
) {
    interface Listener {
        fun onStatusChanged(status: String)
        fun onMessageReceived(payload: String)
    }

    private val client = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private var socket: WebSocket? = null
    private var lastReceivedContent: String? = null
    @Volatile var isConnected = false; private set

    fun connect(host: String, port: Int) {
        listener.onStatusChanged("${AppText.VALIDATING_LINK} $host:$port")
        socket?.close(1000, "Reconnect")
        isConnected = false

        val request = Request.Builder()
            .url("ws://$host:$port")
            .build()

        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                listener.onStatusChanged("${AppText.CONNECTED_TO} $host:$port")
                webSocket.send(Protocol.hello(deviceId))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.contains("\"type\":\"pairing.confirm\"")) {
                    listener.onStatusChanged("${AppText.LINK_VALIDATED} $host:$port")
                }
                listener.onMessageReceived(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                listener.onStatusChanged(AppText.DISCONNECTED)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                listener.onStatusChanged(t.message ?: AppText.CONNECTION_FAILED)
            }
        })
    }

    fun disconnect() {
        socket?.close(1000, "User disconnect")
        socket = null
        isConnected = false
    }

    fun sendClipboard(content: String) {
        if (content == lastReceivedContent) {
            lastReceivedContent = null
            return
        }
        socket?.send(Protocol.clipboardUpdate(deviceId, content))
    }

    fun shouldSkipSend(content: String): Boolean {
        if (content == lastReceivedContent) {
            lastReceivedContent = null
            return true
        }
        return false
    }

    fun markReceived(content: String) {
        lastReceivedContent = content
    }
}
