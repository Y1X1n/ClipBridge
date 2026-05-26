package com.clipbridge.app

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class WebSocketClient(
    private val deviceId: String,
    private val onStatus: (String) -> Unit,
    private val onMessage: (String) -> Unit,
) {
    private val client = OkHttpClient()
    private var socket: WebSocket? = null

    fun connect(host: String, port: Int) {
        val request = Request.Builder()
            .url("ws://$host:$port")
            .build()

        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                onStatus("${AppText.CONNECTED_TO} $host:$port")
                webSocket.send(Protocol.hello(deviceId))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                onMessage(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onStatus(AppText.DISCONNECTED)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onStatus(t.message ?: AppText.CONNECTION_FAILED)
            }
        })
    }

    fun sendClipboard(content: String) {
        socket?.send(Protocol.clipboardUpdate(deviceId, content))
    }
}
