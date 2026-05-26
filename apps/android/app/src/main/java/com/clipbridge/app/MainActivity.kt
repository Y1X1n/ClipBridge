package com.clipbridge.app

import android.app.Activity
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var clipboard: ClipboardRepository
    private lateinit var pairing: PairingRepository
    private lateinit var client: WebSocketClient
    private lateinit var status: TextView
    private lateinit var historyList: LinearLayout
    private lateinit var hostInput: EditText
    private lateinit var portInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        clipboard = ClipboardRepository(this)
        pairing = PairingRepository(this)
        client = WebSocketClient(deviceId(), ::setStatus, ::handleMessage)

        setContentView(createContentView())
        pairing.endpoint()?.let { (host, port) ->
            hostInput.setText(host)
            portInput.setText(port.toString())
        }

        intent.getStringExtra("sharedText")?.takeIf { it.isNotBlank() }?.let { sharedText ->
            HistoryStore.add(HistoryItem("sent", AppText.ANDROID_SHARE, sharedText))
            client.sendClipboard(sharedText)
            renderHistory()
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra("sharedText")?.takeIf { it.isNotBlank() }?.let { sharedText ->
            HistoryStore.add(HistoryItem("sent", AppText.ANDROID_SHARE, sharedText))
            client.sendClipboard(sharedText)
            renderHistory()
        }
    }

    private fun createContentView(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 44, 36, 36)
        }

        root.addView(TextView(this).apply {
            text = "ClipBridge"
            textSize = 34f
            setTextColor(0xFF0F172A.toInt())
        })

        root.addView(TextView(this).apply {
            text = AppText.SUBTITLE
            textSize = 16f
            setTextColor(0xFF475569.toInt())
        })

        status = TextView(this).apply {
            text = AppText.NOT_CONNECTED
            textSize = 15f
            setPadding(0, 28, 0, 16)
        }
        root.addView(status)

        hostInput = EditText(this).apply {
            hint = AppText.HOST_HINT
            setSingleLine(true)
        }
        root.addView(hostInput)

        portInput = EditText(this).apply {
            hint = AppText.PORT_HINT
            setText("7890")
            setSingleLine(true)
        }
        root.addView(portInput)

        root.addView(Button(this).apply {
            text = AppText.CONNECT
            setOnClickListener {
                val host = hostInput.text.toString().trim()
                val port = portInput.text.toString().toIntOrNull() ?: 7890
                if (host.isNotBlank()) {
                    pairing.saveEndpoint(host, port)
                    client.connect(host, port)
                }
            }
        })

        root.addView(Button(this).apply {
            text = AppText.SEND_CLIPBOARD
            setOnClickListener {
                val content = clipboard.readText()
                if (content.isNotBlank()) {
                    HistoryStore.add(HistoryItem("sent", AppText.ANDROID_DEVICE, content))
                    client.sendClipboard(content)
                    renderHistory()
                }
            }
        })

        root.addView(TextView(this).apply {
            text = AppText.RECENT_HISTORY
            textSize = 22f
            setPadding(0, 36, 0, 12)
        })

        historyList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(historyList)
        renderHistory()

        return ScrollView(this).apply { addView(root) }
    }

    private fun handleMessage(payload: String) {
        runOnUiThread {
            val json = JSONObject(payload)
            if (json.optString("type") == "clipboard.update") {
                val content = json.optString("content")
                if (content.isNotBlank()) {
                    HistoryStore.add(HistoryItem("received", json.optString("fromDeviceId", "Windows"), content))
                    renderHistory()
                }
            } else {
                setStatus("${AppText.RECEIVED} ${json.optString("type")}")
            }
        }
    }

    private fun renderHistory() {
        historyList.removeAllViews()
        HistoryStore.list().forEach { item ->
            val entry = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 16, 0, 16)
            }
            entry.addView(TextView(this).apply {
                text = "${if (item.direction == "sent") AppText.SENT else AppText.RECEIVED} · ${item.sourceDevice}"
                textSize = 13f
                setTextColor(0xFF64748B.toInt())
            })
            entry.addView(TextView(this).apply {
                text = item.content
                textSize = 16f
                setTextColor(0xFF0F172A.toInt())
            })
            entry.addView(Button(this).apply {
                text = AppText.COPY
                gravity = Gravity.CENTER
                setOnClickListener { clipboard.writeText(item.content) }
            })
            historyList.addView(entry)
        }
    }

    private fun setStatus(value: String) {
        runOnUiThread { status.text = value }
    }

    private fun deviceId(): String {
        return "android-${Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)}"
    }
}
