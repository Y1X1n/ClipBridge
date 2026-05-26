package com.clipbridge.app

import android.content.Context

class PairingRepository(context: Context) {
    private val preferences = context.getSharedPreferences("clipbridge_pairing", Context.MODE_PRIVATE)

    fun saveEndpoint(host: String, port: Int) {
        preferences.edit()
            .putString("host", host)
            .putInt("port", port)
            .apply()
    }

    fun endpoint(): Pair<String, Int>? {
        val host = preferences.getString("host", null) ?: return null
        val port = preferences.getInt("port", 7890)
        return host to port
    }
}
