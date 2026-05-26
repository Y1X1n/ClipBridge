package com.clipbridge.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

class ClipboardRepository(private val context: Context) {
    private val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun readText(): String {
        val item = manager.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)
        return item?.coerceToText(context)?.toString().orEmpty()
    }

    fun writeText(content: String) {
        manager.setPrimaryClip(ClipData.newPlainText("ClipBridge", content))
    }
}
