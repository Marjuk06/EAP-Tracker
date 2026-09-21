package com.marjuk.eaptracker.ui

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

sealed class GoogleAuthResult {
    data class Success(val token: String) : GoogleAuthResult()
    data class ConsentRequired(val intent: Intent) : GoogleAuthResult()
    data class Error(val message: String) : GoogleAuthResult()
}

object GoogleDriveBackupHelper {
    private const val TAG = "GoogleDriveBackup"
    private const val DRIVE_SCOPE = "oauth2:https://www.googleapis.com/auth/drive.appdata"
    const val BACKUP_FILE_NAME = "eap_tracker_backup.json"

    /**
     * Gets token result with recovery intent support from GoogleSignInAccount.
     */
    suspend fun getAccessTokenResult(context: Context, account: GoogleSignInAccount): GoogleAuthResult = withContext(Dispatchers.IO) {
        try {
            val androidAccount = account.account ?: return@withContext GoogleAuthResult.Error("No account found")
            val token = GoogleAuthUtil.getToken(context, androidAccount, DRIVE_SCOPE)
            GoogleAuthResult.Success(token)
        } catch (e: UserRecoverableAuthException) {
            val recoveryIntent = e.intent
            if (recoveryIntent != null) GoogleAuthResult.ConsentRequired(recoveryIntent)
            else GoogleAuthResult.Error(e.localizedMessage ?: "User consent required")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting auth token: ${e.message}", e)
            GoogleAuthResult.Error(e.localizedMessage ?: "Failed to get Google Drive authorization")
        }
    }

    /**
     * Gets token result with recovery intent support using email address.
     */
    suspend fun getAccessTokenResult(context: Context, email: String): GoogleAuthResult = withContext(Dispatchers.IO) {
        try {
            val androidAccount = android.accounts.Account(email, "com.google")
            val token = GoogleAuthUtil.getToken(context, androidAccount, DRIVE_SCOPE)
            GoogleAuthResult.Success(token)
        } catch (e: UserRecoverableAuthException) {
            val recoveryIntent = e.intent
            if (recoveryIntent != null) GoogleAuthResult.ConsentRequired(recoveryIntent)
            else GoogleAuthResult.Error(e.localizedMessage ?: "User consent required")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting auth token for $email: ${e.message}", e)
            GoogleAuthResult.Error(e.localizedMessage ?: "Failed to get Google Drive authorization")
        }
    }

    /**
     * Gets a valid OAuth2 Bearer token for Google Drive API calls from GoogleSignInAccount.
     */
    suspend fun getAccessToken(context: Context, account: GoogleSignInAccount): String? = withContext(Dispatchers.IO) {
        when (val res = getAccessTokenResult(context, account)) {
            is GoogleAuthResult.Success -> res.token
            else -> null
        }
    }

    /**
     * Gets a valid OAuth2 Bearer token for Google Drive API calls using email address.
     */
    suspend fun getAccessToken(context: Context, email: String): String? = withContext(Dispatchers.IO) {
        when (val res = getAccessTokenResult(context, email)) {
            is GoogleAuthResult.Success -> res.token
            else -> null
        }
    }

    /**
     * Searches the user's hidden Google Drive appDataFolder for existing EAPTracker backup.
     */
    suspend fun queryLatestBackup(token: String): CloudBackupInfo? = withContext(Dispatchers.IO) {
        try {
            val queryUrl = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&fields=files(id,name,size,modifiedTime)&q=name='$BACKUP_FILE_NAME'%20and%20trashed=false"
            val url = URL(queryUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
                connectTimeout = 15000
                readTimeout = 15000
            }

            if (conn.responseCode in 200..299) {
                val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseStr)
                val files = json.optJSONArray("files")
                if (files != null && files.length() > 0) {
                    val fileObj = files.getJSONObject(0)
                    val id = fileObj.getString("id")
                    val name = fileObj.optString("name", BACKUP_FILE_NAME)
                    val size = fileObj.optLong("size", 0L)
                    val modTimeStr = fileObj.optString("modifiedTime", "")
                    
                    var timestamp = System.currentTimeMillis()
                    if (modTimeStr.isNotEmpty()) {
                        try {
                            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                            sdf.timeZone = TimeZone.getTimeZone("UTC")
                            timestamp = sdf.parse(modTimeStr)?.time ?: System.currentTimeMillis()
                        } catch (_: Exception) {
                            try {
                                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                                sdf.timeZone = TimeZone.getTimeZone("UTC")
                                timestamp = sdf.parse(modTimeStr)?.time ?: System.currentTimeMillis()
                            } catch (_: Exception) {}
                        }
                    }

                    return@withContext CloudBackupInfo(
                        fileId = id,
                        fileName = name,
                        timestamp = timestamp,
                        sizeBytes = size,
                        deviceName = "Google Cloud"
                    )
                }
            } else {
                Log.e(TAG, "Query failed with HTTP ${conn.responseCode}: ${conn.errorStream?.bufferedReader()?.use { it.readText() }}")
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error querying drive backup: ${e.message}", e)
            null
        }
    }

    /**
     * Uploads the JSON backup bundle to Google Drive's hidden appDataFolder.
     * Uses multipart upload (Metadata + Application/JSON content) with chunked byte progress reporting.
     */
    suspend fun uploadBackup(
        token: String, 
        backupJson: String,
        onProgress: ((bytesSent: Long, totalBytes: Long) -> Unit)? = null
    ): Result<CloudBackupInfo> = withContext(Dispatchers.IO) {
        try {
            // First check if an existing backup file exists to overwrite/update or create
            val existing = queryLatestBackup(token)

            val boundary = "======EAPTrackerDriveBackupBoundary${System.currentTimeMillis()}======"
            val uploadUrl = if (existing != null) {
                "https://www.googleapis.com/upload/drive/v3/files/${existing.fileId}?uploadType=multipart"
            } else {
                "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
            }

            val url = URL(uploadUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = if (existing != null) "PATCH" else "POST"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
                doOutput = true
                connectTimeout = 20000
                readTimeout = 20000
            }

            val metadataJson = JSONObject().apply {
                put("name", BACKUP_FILE_NAME)
                if (existing == null) {
                    put("parents", org.json.JSONArray().apply { put("appDataFolder") })
                }
            }.toString()

            val os = DataOutputStream(conn.outputStream)
            val writer = OutputStreamWriter(os, StandardCharsets.UTF_8)

            // Part 1: Metadata
            writer.write("--$boundary\r\n")
            writer.write("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            writer.write(metadataJson)
            writer.write("\r\n")

            // Part 2: Media Data
            writer.write("--$boundary\r\n")
            writer.write("Content-Type: application/json\r\n\r\n")
            writer.flush()

            val bytes = backupJson.toByteArray(StandardCharsets.UTF_8)
            val totalBytes = bytes.size.toLong()
            val chunkSize = 1024
            var offset = 0
            while (offset < bytes.size) {
                val count = minOf(chunkSize, bytes.size - offset)
                os.write(bytes, offset, count)
                offset += count
                os.flush()
                onProgress?.invoke(offset.toLong(), totalBytes)
                kotlinx.coroutines.delay(35)
            }

            writer.write("\r\n--$boundary--\r\n")
            writer.flush()
            os.close()

            if (conn.responseCode in 200..299) {
                val res = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(res)
                val fileId = json.getString("id")
                val backupInfo = CloudBackupInfo(
                    fileId = fileId,
                    fileName = BACKUP_FILE_NAME,
                    timestamp = System.currentTimeMillis(),
                    sizeBytes = bytes.size.toLong(),
                    deviceName = android.os.Build.MODEL ?: "Android"
                )
                Result.success(backupInfo)
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP ${conn.responseCode}"
                Log.e(TAG, "Upload failed: $err")
                Result.failure(Exception("Drive upload failed: $err"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Drive upload: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Downloads the backup JSON string from Google Drive with chunked byte progress reporting.
     */
    suspend fun downloadBackupContent(
        token: String, 
        fileId: String,
        expectedSizeBytes: Long = 0L,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val downloadUrl = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
            val url = URL(downloadUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
                connectTimeout = 20000
                readTimeout = 20000
            }

            if (conn.responseCode in 200..299) {
                val totalBytes = if (expectedSizeBytes > 0L) expectedSizeBytes else conn.contentLengthLong.coerceAtLeast(0L)
                val inputStream = conn.inputStream
                val outputStream = ByteArrayOutputStream()
                val buffer = ByteArray(1024)
                var bytesRead = 0L
                var count: Int
                while (inputStream.read(buffer).also { count = it } != -1) {
                    outputStream.write(buffer, 0, count)
                    bytesRead += count
                    onProgress?.invoke(bytesRead, totalBytes)
                    kotlinx.coroutines.delay(35)
                }
                val content = outputStream.toString(StandardCharsets.UTF_8.name())
                Result.success(content)
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP ${conn.responseCode}"
                Result.failure(Exception("Download failed: $err"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception downloading backup: ${e.message}", e)
            Result.failure(e)
        }
    }
}
