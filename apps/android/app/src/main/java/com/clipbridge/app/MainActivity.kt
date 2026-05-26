package com.clipbridge.app

import android.app.Activity
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.zxing.integration.android.IntentIntegrator
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                setStatus(AppText.SCAN_CANCELLED)
            } else {
                applyPairingPayload(result.contents)
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun createContentView(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(24))
            setBackgroundColor(INK)
        }

        root.addView(TextView(this).apply {
            text = "ClipBridge"
            textSize = 42f
            letterSpacing = -0.06f
            setTextColor(PAPER)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })

        root.addView(TextView(this).apply {
            text = AppText.SUBTITLE
            textSize = 14f
            setTextColor(MUTED)
            setPadding(0, dp(4), 0, dp(18))
        })

        root.addView(panel().apply {
            addView(sectionLabel("01 · ${AppText.NODE_STATUS}"))
            status = TextView(context).apply {
                text = AppText.NOT_CONNECTED
                textSize = 15f
                setTextColor(ACID)
                setPadding(0, dp(8), 0, 0)
            }
            addView(status)
        })

        root.addView(spacer(14))

        root.addView(panel().apply {
            addView(sectionLabel("02 · ${AppText.LINK_PANEL}"))
            addView(TextView(context).apply {
                text = AppText.MANUAL_HINT
                textSize = 13f
                setTextColor(MUTED)
                setPadding(0, 0, 0, dp(10))
            })

            hostInput = field(AppText.HOST_HINT)
            addView(hostInput)
            portInput = field(AppText.PORT_HINT).apply { setText("7890") }
            addView(portInput)

            addView(actionButton(AppText.SCAN_QR) { scanQr() })
            addView(actionButton(AppText.CONNECT) { connectFromInputs() })
            addView(actionButton(AppText.SEND_CLIPBOARD) { sendCurrentClipboard() })
        })

        root.addView(spacer(14))

        root.addView(panel().apply {
            addView(sectionLabel("03 · ${AppText.RECENT_HISTORY}"))
            historyList = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }
            addView(historyList)
        })

        renderHistory()
        return ScrollView(this).apply {
            setBackgroundColor(INK)
            addView(root)
        }
    }

    private fun scanQr() {
        IntentIntegrator(this)
            .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            .setPrompt(AppText.SCAN_QR)
            .setBeepEnabled(false)
            .setOrientationLocked(true)
            .initiateScan()
    }

    private fun applyPairingPayload(payload: String) {
        try {
            val json = JSONObject(payload)
            if (json.optString("app") != "clipbridge") {
                setStatus(AppText.INVALID_QR)
                return
            }
            val host = json.getString("host")
            val port = json.optInt("port", 7890)
            hostInput.setText(host)
            portInput.setText(port.toString())
            pairing.saveEndpoint(host, port)
            setStatus(AppText.VALIDATING_LINK)
            client.connect(host, port)
        } catch (_: Exception) {
            setStatus(AppText.INVALID_QR)
        }
    }

    private fun connectFromInputs() {
        val host = hostInput.text.toString().trim()
        val port = portInput.text.toString().toIntOrNull() ?: 7890
        if (host.isNotBlank()) {
            pairing.saveEndpoint(host, port)
            client.connect(host, port)
        }
    }

    private fun sendCurrentClipboard() {
        val content = clipboard.readText()
        if (content.isNotBlank()) {
            HistoryStore.add(HistoryItem("sent", AppText.ANDROID_DEVICE, content))
            client.sendClipboard(content)
            renderHistory()
        }
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
        if (HistoryStore.list().isEmpty()) {
            historyList.addView(TextView(this).apply {
                text = AppText.NOT_CONNECTED
                textSize = 13f
                setTextColor(MUTED)
                setPadding(0, dp(8), 0, 0)
            })
            return
        }

        HistoryStore.list().forEach { item ->
            val entry = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                setBackgroundColor(0xFF22261D.toInt())
            }
            entry.addView(TextView(this).apply {
                text = "${if (item.direction == "sent") AppText.SENT else AppText.RECEIVED} · ${item.sourceDevice}"
                textSize = 12f
                setTextColor(ACID)
            })
            entry.addView(TextView(this).apply {
                text = item.content
                textSize = 15f
                setTextColor(PAPER)
                setPadding(0, dp(6), 0, dp(8))
            })
            entry.addView(actionButton(AppText.COPY) { clipboard.writeText(item.content) })
            historyList.addView(entry)
            historyList.addView(spacer(10))
        }
    }

    private fun panel(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(PANEL)
        }
    }

    private fun sectionLabel(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 13f
            letterSpacing = 0.08f
            setTextColor(COPPER)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(8))
        }
    }

    private fun field(hintText: String): EditText {
        return EditText(this).apply {
            hint = hintText
            setSingleLine(true)
            textSize = 15f
            setTextColor(PAPER)
            setHintTextColor(MUTED)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(0xFF171B14.toInt())
        }
    }

    private fun actionButton(label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            gravity = Gravity.CENTER
            setTextColor(INK)
            setBackgroundColor(ACID)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setOnClickListener { onClick() }
        }
    }

    private fun spacer(height: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(height))
        }
    }

    private fun setStatus(value: String) {
        runOnUiThread { status.text = value }
    }

    private fun deviceId(): String {
        return "android-${Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)}"
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val INK = 0xFF10130F.toInt()
        private const val PAPER = 0xFFE7E0CF.toInt()
        private const val PANEL = 0xFF1C1F19.toInt()
        private const val MUTED = 0xFF9B9A8F.toInt()
        private const val ACID = 0xFFD7FF47.toInt()
        private const val COPPER = 0xFFD8894D.toInt()
    }
}
