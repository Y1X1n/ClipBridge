package com.clipbridge.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat

class ClipBridgeService : Service(), WebSocketClient.Listener {

    private lateinit var clipboard: ClipboardRepository
    private lateinit var client: WebSocketClient
    private var reconnectHost: String? = null
    private var reconnectPort: Int = 7890
    private var reconnectDelay: Long = 1000
    @Volatile private var isWritingFromRemote = false

    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        if (!isWritingFromRemote) {
            val content = clipboard.readText()
            if (content.isNotBlank() && !client.shouldSkipSend(content)) {
                HistoryStore.add(HistoryItem("sent", AppText.ANDROID_DEVICE, content))
                client.sendClipboard(content)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        clipboard = ClipboardRepository(this)
        client = WebSocketClient(deviceId(), this)
        clipboard.addListener(clipboardListener)
        createNotificationChannel()
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val host = intent.getStringExtra(EXTRA_HOST) ?: return START_NOT_STICKY
                val port = intent.getIntExtra(EXTRA_PORT, 7890)
                reconnectHost = host
                reconnectPort = port
                reconnectDelay = 1000
                startForeground(NOTIFICATION_ID, buildNotification(AppText.VALIDATING_LINK))
                client.connect(host, port)
            }
            ACTION_SEND -> {
                val content = intent.getStringExtra(EXTRA_CONTENT) ?: return START_NOT_STICKY
                if (!client.shouldSkipSend(content)) {
                    HistoryStore.add(HistoryItem("sent", AppText.ANDROID_DEVICE, content))
                    client.sendClipboard(content)
                }
            }
            ACTION_DISCONNECT -> {
                client.disconnect()
                reconnectHost = null
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        clipboard.removeListener(clipboardListener)
        client.disconnect()
        instance = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStatusChanged(status: String) {
        statusText = status
        updateNotification(status)

        if (status == AppText.DISCONNECTED || status.startsWith(AppText.CONNECTION_FAILED)) {
            scheduleReconnect()
        } else if (status.startsWith(AppText.LINK_VALIDATED) || status.startsWith(AppText.CONNECTED_TO)) {
            reconnectDelay = 1000
        }
    }

    override fun onMessageReceived(payload: String) {
        val json = org.json.JSONObject(payload)
        if (json.optString("type") == "clipboard.update") {
            val content = json.optString("content")
            if (content.isNotBlank()) {
                client.markReceived(content)
                isWritingFromRemote = true
                clipboard.writeText(content)
                isWritingFromRemote = false
                HistoryStore.add(HistoryItem("received", json.optString("fromDeviceId", "Windows"), content))
            }
        }
    }

    private fun scheduleReconnect() {
        val host = reconnectHost ?: return
        val port = reconnectPort
        val delay = reconnectDelay
        reconnectDelay = (reconnectDelay * 2).coerceAtMost(30_000)

        android.os.Handler(mainLooper).postDelayed({
            if (reconnectHost != null) {
                client.connect(host, port)
            }
        }, delay)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            AppText.NOTIFICATION_CHANNEL,
            NotificationManager.IMPORTANCE_LOW,
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(statusText: String): android.app.Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val disconnectIntent = PendingIntent.getService(
            this, 1,
            Intent(this, ClipBridgeService::class.java).setAction(ACTION_DISCONNECT),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(AppText.NOTIFICATION_TITLE)
            .setContentText(statusText)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(0, AppText.DISCONNECT_ACTION, disconnectIntent)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(statusText))
    }

    private fun deviceId(): String {
        return "android-${Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)}"
    }

    companion object {
        const val ACTION_CONNECT = "com.clipbridge.app.CONNECT"
        const val ACTION_SEND = "com.clipbridge.app.SEND"
        const val ACTION_DISCONNECT = "com.clipbridge.app.DISCONNECT"
        const val EXTRA_HOST = "host"
        const val EXTRA_PORT = "port"
        const val EXTRA_CONTENT = "content"

        private const val CHANNEL_ID = "clipbridge_service"
        private const val NOTIFICATION_ID = 1

        @Volatile
        var statusText: String = AppText.NOT_CONNECTED
            private set

        private var instance: ClipBridgeService? = null

        val isRunning: Boolean get() = instance != null
    }
}
