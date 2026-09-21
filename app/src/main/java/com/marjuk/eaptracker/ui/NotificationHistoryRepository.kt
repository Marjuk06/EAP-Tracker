package com.marjuk.eaptracker.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject

data class NotificationHistoryItem(
    val id: String,
    val title: String,
    val message: String,
    val category: String, // "Notice", "Motivation", "Alert", "Exam", "Quote", "Backup", "Syllabus"
    val timestamp: Long,
    val isRead: Boolean = false,
    val actionRoute: String? = null,
    val actionUrl: String? = null,
    val actionButtonText: String? = null
)

object NotificationHistoryRepository {
    private const val PREFS_NAME = "eap_notifications_history"
    private const val KEY_HISTORY = "history_json"
    private var prefs: SharedPreferences? = null

    val itemsState = mutableStateListOf<NotificationHistoryItem>()

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadHistory()
    }

    private fun loadHistory() {
        val json = prefs?.getString(KEY_HISTORY, null) ?: return
        try {
            val arr = JSONArray(json)
            val list = mutableListOf<NotificationHistoryItem>()
            val seenKeys = mutableSetOf<String>()

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val title = obj.optString("title", "Notification")
                val message = obj.optString("message", "")
                val key = "${title.trim().lowercase()}_${message.trim().lowercase()}"

                if (!seenKeys.contains(key)) {
                    seenKeys.add(key)
                    list.add(
                        NotificationHistoryItem(
                            id = obj.optString("id", "${obj.optLong("timestamp", System.currentTimeMillis())}_$i"),
                            title = title,
                            message = message,
                            category = obj.optString("category", "Notice"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            isRead = obj.optBoolean("isRead", false),
                            actionRoute = if (obj.has("actionRoute") && !obj.isNull("actionRoute")) obj.getString("actionRoute") else null,
                            actionUrl = if (obj.has("actionUrl") && !obj.isNull("actionUrl")) obj.getString("actionUrl") else null,
                            actionButtonText = if (obj.has("actionButtonText") && !obj.isNull("actionButtonText")) obj.getString("actionButtonText") else null
                        )
                    )
                }
            }
            itemsState.clear()
            itemsState.addAll(list)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addNotification(
        title: String,
        message: String,
        category: String = "Notice",
        timestamp: Long = System.currentTimeMillis(),
        actionRoute: String? = null,
        actionUrl: String? = null,
        actionButtonText: String? = null
    ) {
        if (message.isBlank()) return

        val cleanTitle = title.replace(Regex("[🔥🚀💡⚡✨📢🎯🌟📖]"), "").trim().ifBlank { "EAP Notice" }
        val cleanMessage = message.replace(Regex("[🔥🚀💡⚡✨📢🎯🌟📖]"), "").trim()

        // Check if an identical notification already exists to prevent duplicate spamming
        val duplicate = itemsState.any { 
            it.title.equals(cleanTitle, ignoreCase = true) && it.message.equals(cleanMessage, ignoreCase = true) 
        }
        if (duplicate) return

        val item = NotificationHistoryItem(
            id = "notif_${System.currentTimeMillis()}_${(1000..9999).random()}",
            title = cleanTitle,
            message = cleanMessage,
            category = category,
            timestamp = timestamp,
            isRead = false,
            actionRoute = actionRoute,
            actionUrl = actionUrl,
            actionButtonText = actionButtonText
        )
        itemsState.add(0, item)
        saveHistory()
    }

    fun removeNotification(id: String) {
        itemsState.removeAll { it.id == id }
        saveHistory()
    }

    fun clearAll() {
        itemsState.clear()
        saveHistory()
    }

    fun markAllAsRead() {
        val updated = itemsState.map { it.copy(isRead = true) }
        itemsState.clear()
        itemsState.addAll(updated)
        saveHistory()
    }

    fun getUnreadCount(): Int {
        return itemsState.count { !it.isRead }
    }

    private fun saveHistory() {
        val arr = JSONArray()
        for (item in itemsState) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("message", item.message)
                put("category", item.category)
                put("timestamp", item.timestamp)
                put("isRead", item.isRead)
                item.actionRoute?.let { put("actionRoute", it) }
                item.actionUrl?.let { put("actionUrl", it) }
                item.actionButtonText?.let { put("actionButtonText", it) }
            }
            arr.put(obj)
        }
        prefs?.edit()?.putString(KEY_HISTORY, arr.toString())?.apply()
    }
}
