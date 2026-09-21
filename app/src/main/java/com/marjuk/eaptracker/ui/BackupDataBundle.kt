package com.marjuk.eaptracker.ui

import org.json.JSONObject

data class BackupMetadata(
    val version: Int = 1,
    val appVersion: String = "1.0",
    val timestamp: Long = System.currentTimeMillis(),
    val deviceName: String = android.os.Build.MODEL ?: "Android Device",
    val backupId: String = "EAP_${System.currentTimeMillis()}"
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("version", version)
            put("appVersion", appVersion)
            put("timestamp", timestamp)
            put("deviceName", deviceName)
            put("backupId", backupId)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): BackupMetadata {
            return BackupMetadata(
                version = json.optInt("version", 1),
                appVersion = json.optString("appVersion", "1.0"),
                timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                deviceName = json.optString("deviceName", "Android Device"),
                backupId = json.optString("backupId", "")
            )
        }
    }
}

data class CloudBackupInfo(
    val fileId: String,
    val fileName: String,
    val timestamp: Long,
    val sizeBytes: Long,
    val deviceName: String
) {
    val formattedSize: String get() {
        return if (sizeBytes < 1024) "$sizeBytes B"
        else if (sizeBytes < 1024 * 1024) "${sizeBytes / 1024} KB"
        else String.format("%.1f MB", sizeBytes / (1024f * 1024f))
    }
}
