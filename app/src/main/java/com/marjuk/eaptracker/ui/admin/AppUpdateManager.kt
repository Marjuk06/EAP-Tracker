package com.marjuk.eaptracker.ui.admin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.marjuk.eaptracker.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val versionCode: Int = 0,
    val versionName: String = "",
    val apkUrl: String = "",
    val title: String = "🚀 New Update Available",
    val changelog: String = "",
    val isForceUpdate: Boolean = false,
    val publishedAt: Long = System.currentTimeMillis()
)

object AppUpdateManager {

    private const val TAG = "AppUpdateManager"
    private const val RTDB_URL = "https://eap-tracker-default-rtdb.firebaseio.com"
    private const val NOTIF_CHANNEL_UPDATE = "eap_app_updates_channel"
    private const val NOTIF_ID_DOWNLOAD = 99102

    // Reactive Compose States
    val availableUpdate = mutableStateOf<AppUpdateInfo?>(null)
    val isUpdateDialogOpen = mutableStateOf(false)
    val isDownloading = mutableStateOf(false)
    val downloadProgress = mutableFloatStateOf(0f)
    val downloadProgressText = mutableStateOf("")
    val isDownloadedReadyToInstall = mutableStateOf(false)
    val downloadedApkFile = mutableStateOf<File?>(null)

    fun getCurrentVersionCode(context: Context): Long {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
        } catch (_: Exception) {
            1L
        }
    }

    fun init(context: Context) {
        createNotificationChannel(context)
        checkLatestReleaseFromCloud(context)
    }

    /**
     * Listen or fetch the latest release metadata from Firebase RTDB
     */
    fun checkLatestReleaseFromCloud(context: Context, onResult: ((AppUpdateInfo?) -> Unit)? = null) {
        try {
            val database = FirebaseDatabase.getInstance(RTDB_URL)
            val updateRef = database.getReference("app_config").child("latest_update")

            updateRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val versionCode = (snapshot.child("versionCode").value as? Long)?.toInt() ?: 0
                        val versionName = snapshot.child("versionName").value as? String ?: ""
                        val apkUrl = snapshot.child("apkUrl").value as? String ?: ""
                        val title = snapshot.child("title").value as? String ?: "🚀 New Update Available ($versionName)"
                        val changelog = snapshot.child("changelog").value as? String ?: ""
                        val isForceUpdate = snapshot.child("isForceUpdate").value as? Boolean ?: false
                        val publishedAt = (snapshot.child("publishedAt").value as? Long) ?: System.currentTimeMillis()

                        val currentVersionCode = getCurrentVersionCode(context)

                        if (versionCode > currentVersionCode && apkUrl.isNotBlank()) {
                            val info = AppUpdateInfo(
                                versionCode = versionCode,
                                versionName = versionName,
                                apkUrl = apkUrl,
                                title = title,
                                changelog = changelog,
                                isForceUpdate = isForceUpdate,
                                publishedAt = publishedAt
                            )
                            availableUpdate.value = info
                            isUpdateDialogOpen.value = true
                            onResult?.invoke(info)
                        } else {
                            availableUpdate.value = null
                            onResult?.invoke(null)
                        }
                    } else {
                        onResult?.invoke(null)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Update check cancelled: ${error.message}")
                    onResult?.invoke(null)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error checking latest release", e)
        }
    }

    /**
     * Trigger update from push notification or FCM payload
     */
    fun handleRemoteUpdatePayload(
        context: Context,
        apkUrl: String,
        versionName: String,
        versionCode: Int,
        title: String,
        changelog: String,
        isForceUpdate: Boolean
    ) {
        val currentVersionCode = getCurrentVersionCode(context)
        if (versionCode > currentVersionCode || currentVersionCode <= 1L) {
            val info = AppUpdateInfo(
                versionCode = versionCode,
                versionName = versionName,
                apkUrl = apkUrl,
                title = title,
                changelog = changelog,
                isForceUpdate = isForceUpdate
            )
            availableUpdate.value = info
            isUpdateDialogOpen.value = true
        }
    }

    /**
     * Downloads the APK file in the background with progress reporting, then launches PackageInstaller
     */
    fun startApkDownload(context: Context, updateInfo: AppUpdateInfo) {
        if (isDownloading.value) return
        if (updateInfo.apkUrl.isBlank()) {
            Toast.makeText(context, "Invalid APK download link", Toast.LENGTH_SHORT).show()
            return
        }

        isDownloading.value = true
        downloadProgress.floatValue = 0f
        downloadProgressText.value = "Connecting to download server..."
        isDownloadedReadyToInstall.value = false

        CoroutineScope(Dispatchers.IO).launch {
            var connection: HttpURLConnection? = null
            var outputStream: FileOutputStream? = null
            try {
                val updateDir = File(context.getExternalFilesDir(null) ?: context.cacheDir, "updates")
                if (!updateDir.exists()) updateDir.mkdirs()

                val cleanVer = updateInfo.versionName.ifBlank { "v${updateInfo.versionCode}" }
                val targetFile = File(updateDir, "EAPTracker_${cleanVer}.apk")
                if (targetFile.exists()) targetFile.delete()

                val url = URL(updateInfo.apkUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20000
                    readTimeout = 30000
                    instanceFollowRedirects = true
                    setRequestProperty("Accept-Encoding", "identity")
                }
                connection.connect()

                if (connection.responseCode !in 200..299) {
                    throw Exception("Server returned HTTP ${connection.responseCode}")
                }

                val totalLength = connection.contentLength.toLong()
                val inputStream = connection.inputStream
                outputStream = FileOutputStream(targetFile)

                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                var downloadedBytes = 0L
                var lastProgressUpdate = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    val now = System.currentTimeMillis()
                    if (now - lastProgressUpdate > 100 || downloadedBytes == totalLength) {
                        lastProgressUpdate = now
                        val pct = if (totalLength > 0) downloadedBytes.toFloat() / totalLength.toFloat() else 0f
                        val downloadedMB = downloadedBytes / (1024f * 1024f)
                        val totalMB = totalLength / (1024f * 1024f)
                        val pctInt = (pct * 100).toInt()

                        val statusStr = if (totalLength > 0) {
                            String.format("%.1f MB / %.1f MB (%d%%)", downloadedMB, totalMB, pctInt)
                        } else {
                            String.format("%.1f MB downloaded", downloadedMB)
                        }

                        withContext(Dispatchers.Main) {
                            downloadProgress.floatValue = pct
                            downloadProgressText.value = statusStr
                        }

                        updateDownloadNotification(context, pctInt, totalLength <= 0, statusStr)
                    }
                }

                outputStream.flush()
                dismissDownloadNotification(context)

                withContext(Dispatchers.Main) {
                    isDownloading.value = false
                    downloadProgress.floatValue = 1f
                    downloadProgressText.value = "Download Complete! Ready to install."
                    isDownloadedReadyToInstall.value = true
                    downloadedApkFile.value = targetFile

                    // Automatically launch package installer
                    promptInstallApk(context, targetFile)
                }
            } catch (e: Exception) {
                Log.e(TAG, "APK Download failed", e)
                dismissDownloadNotification(context)
                withContext(Dispatchers.Main) {
                    isDownloading.value = false
                    downloadProgress.floatValue = 0f
                    downloadProgressText.value = "Download failed: ${e.localizedMessage}"
                    Toast.makeText(context, "Update download failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            } finally {
                try { outputStream?.close() } catch (_: Exception) {}
                try { connection?.disconnect() } catch (_: Exception) {}
            }
        }
    }

    /**
     * Launches Android PackageInstaller Intent via FileProvider
     */
    fun promptInstallApk(context: Context, apkFile: File) {
        try {
            if (!apkFile.exists()) {
                Toast.makeText(context, "APK file not found", Toast.LENGTH_SHORT).show()
                return
            }

            // Android 8.0+ Unknown App Sources Permission check
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Toast.makeText(context, "Please allow EAP Tracker to install updates", Toast.LENGTH_LONG).show()
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    return
                }
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch installer", e)
            Toast.makeText(context, "Cannot open package installer: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    // =========================================================================
    // System Notification Helper for In-Progress Downloads
    // =========================================================================
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_UPDATE,
                "App Updates",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows downloading and installation progress for EAP Tracker updates"
                setShowBadge(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun updateDownloadNotification(context: Context, percent: Int, indeterminate: Boolean, status: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val notif = NotificationCompat.Builder(context, NOTIF_CHANNEL_UPDATE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Downloading EAP Tracker Update...")
            .setContentText(status)
            .setProgress(100, percent, indeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        manager.notify(NOTIF_ID_DOWNLOAD, notif)
    }

    private fun dismissDownloadNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(NOTIF_ID_DOWNLOAD)
    }
}
