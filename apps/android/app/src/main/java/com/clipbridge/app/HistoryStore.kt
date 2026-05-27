package com.clipbridge.app

data class HistoryItem(
    val direction: String,
    val sourceDevice: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
)

object HistoryStore {
    private val items = mutableListOf<HistoryItem>()

    @Synchronized
    fun add(item: HistoryItem) {
        items.add(0, item)
        if (items.size > 100) {
            items.removeLast()
        }
    }

    @Synchronized
    fun list(): List<HistoryItem> = items.toList()
}
