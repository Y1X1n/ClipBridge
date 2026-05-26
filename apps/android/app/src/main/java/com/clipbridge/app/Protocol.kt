package com.clipbridge.app

import org.json.JSONObject
import java.util.UUID

object Protocol {
    fun clipboardUpdate(fromDeviceId: String, content: String): String {
        return JSONObject()
            .put("version", 1)
            .put("type", "clipboard.update")
            .put("messageId", UUID.randomUUID().toString())
            .put("fromDeviceId", fromDeviceId)
            .put("toDeviceId", "windows")
            .put("contentType", "text")
            .put("content", content)
            .put("timestamp", System.currentTimeMillis())
            .toString()
    }

    fun hello(fromDeviceId: String): String {
        return JSONObject()
            .put("version", 1)
            .put("type", "device.hello")
            .put("messageId", UUID.randomUUID().toString())
            .put("fromDeviceId", fromDeviceId)
            .put("toDeviceId", "windows")
            .put("timestamp", System.currentTimeMillis())
            .toString()
    }
}
