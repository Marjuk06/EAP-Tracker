package com.marjuk.eaptracker.ui

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.work.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object BackupRepository {
    private const val TAG = "BackupRepository"
    private const val PREFS_BACKUP_META = "eap_backup_metadata_prefs"

    // SharedPreferences to bundle in backups
    private val PREF_FILES = listOf(
        "eap_syllabus_prefs",
        "eap_exam_marks_prefs",
        "eap_study_tracker_prefs",
        "eap_profile_prefs",
        "eap_settings_prefs"
    )

    private var prefs: SharedPreferences? = null

    // Reactive State Holders for UI
    val connectedAccountEmail = mutableStateOf<String?>(null)
    val connectedAccountName = mutableStateOf<String?>(null)
    val connectedAccountPhotoUrl = mutableStateOf<String?>(null)

    val lastBackupTimestamp = mutableLongStateOf(0L)
    val lastBackupSizeBytes = mutableLongStateOf(0L)
    val lastBackupStatus = mutableStateOf("No backup yet")

    val isBackupInProgress = mutableStateOf(false)
    val isRestoreInProgress = mutableStateOf(false)
    val backupProgressText = mutableStateOf("")

    val autoBackupFrequency = mutableStateOf("Daily") // "Daily", "Weekly", "Off"
    val backupOverWifiOnly = mutableStateOf(false)

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_BACKUP_META, Context.MODE_PRIVATE)
            loadMetadata()
            checkLastSignedInAccount(context)
            if (!connectedAccountEmail.value.isNullOrBlank() && autoBackupFrequency.value != "Off") {
                applyPeriodicBackupSchedule(context)
            }
        }
    }

    private fun loadMetadata() {
        val p = prefs ?: return
        lastBackupTimestamp.longValue = p.getLong("last_backup_time", 0L)
        lastBackupSizeBytes.longValue = p.getLong("last_backup_size", 0L)
        lastBackupStatus.value = p.getString("last_backup_status", "No backup yet") ?: "No backup yet"
        autoBackupFrequency.value = p.getString("auto_backup_freq", "Daily") ?: "Daily"
        backupOverWifiOnly.value = p.getBoolean("backup_wifi_only", false)
        connectedAccountEmail.value = p.getString("last_account_email", null)
    }

    fun checkLastSignedInAccount(context: Context) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            updateAccountState(account, context)
        }
    }

    fun updateAccountState(account: GoogleSignInAccount?, context: Context? = null) {
        if (account != null) {
            connectedAccountEmail.value = account.email
            connectedAccountName.value = account.displayName
            val photo = account.photoUrl?.toString()
            connectedAccountPhotoUrl.value = photo
            ProfileRepository.updateGooglePhotoUrl(photo)
            prefs?.edit()?.putString("last_account_email", account.email)?.apply()
            context?.let { applyPeriodicBackupSchedule(it) }
        } else {
            connectedAccountEmail.value = null
            connectedAccountName.value = null
            connectedAccountPhotoUrl.value = null
            prefs?.edit()?.remove("last_account_email")?.apply()
            context?.let { WorkManager.getInstance(it).cancelUniqueWork("periodic_backup") }
        }
    }

    fun setAccountEmail(email: String, displayName: String? = null, context: Context? = null) {
        connectedAccountEmail.value = email
        connectedAccountName.value = displayName ?: email.substringBefore("@")
        connectedAccountPhotoUrl.value = null
        prefs?.edit()?.putString("last_account_email", email)?.apply()
        context?.let { applyPeriodicBackupSchedule(it) }
    }

    fun saveAutoBackupSettings(context: Context, freq: String, wifiOnly: Boolean) {
        autoBackupFrequency.value = freq
        backupOverWifiOnly.value = wifiOnly
        prefs?.edit()
            ?.putString("auto_backup_freq", freq)
            ?.putBoolean("backup_wifi_only", wifiOnly)
            ?.apply()
        
        applyPeriodicBackupSchedule(context)
    }

    private fun updateLastBackupSuccess(sizeBytes: Long, addHistoryNotification: Boolean = false) {
        val now = System.currentTimeMillis()
        lastBackupTimestamp.longValue = now
        lastBackupSizeBytes.longValue = sizeBytes
        lastBackupStatus.value = "Backed up successfully"
        prefs?.edit()
            ?.putLong("last_backup_time", now)
            ?.putLong("last_backup_size", sizeBytes)
            ?.putString("last_backup_status", "Backed up successfully")
            ?.apply()

        if (addHistoryNotification) {
            NotificationHistoryRepository.addNotification(
                title = "Cloud Backup Complete",
                message = "Your study progress and exam marks (${getFormattedLastBackupSize()}) were successfully saved to Google Drive.",
                category = "Backup",
                actionRoute = "settings",
                actionButtonText = "View in Settings"
            )
        }
    }

    // ================= 1. JSON BUNDLE PACKAGING & REHYDRATION =================

    fun createFullBackupJson(context: Context): String {
        val root = JSONObject()
        val metadata = BackupMetadata(
            timestamp = System.currentTimeMillis(),
            deviceName = android.os.Build.MODEL ?: "Android",
            appVersion = "1.0"
        )
        root.put("metadata", metadata.toJsonObject())

        val prefsJson = JSONObject()
        PREF_FILES.forEach { fileName ->
            val sp = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
            val fileMapJson = JSONObject()
            sp.all.forEach { (key, value) ->
                when (value) {
                    is Boolean -> fileMapJson.put(key, value)
                    is Int -> fileMapJson.put(key, value)
                    is Long -> fileMapJson.put(key, value)
                    is Float -> fileMapJson.put(key, value.toDouble())
                    is String -> fileMapJson.put(key, value)
                    is Set<*> -> {
                        val arr = org.json.JSONArray()
                        value.forEach { if (it is String) arr.put(it) }
                        fileMapJson.put(key, arr)
                    }
                }
            }
            prefsJson.put(fileName, fileMapJson)
        }
        root.put("shared_preferences", prefsJson)

        return root.toString(2)
    }

    fun restoreFromBackupJson(context: Context, jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            val prefsJson = root.optJSONObject("shared_preferences") ?: return false

            PREF_FILES.forEach { fileName ->
                val fileMapJson = prefsJson.optJSONObject(fileName)
                if (fileMapJson != null) {
                    val sp = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
                    val editor = sp.edit()
                    editor.clear() // Clear existing data to avoid leftover state

                    val keys = fileMapJson.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val value = fileMapJson.get(key)
                        when (value) {
                            is Boolean -> editor.putBoolean(key, value)
                            is Int -> editor.putInt(key, value)
                            is Long -> editor.putLong(key, value)
                            is Double -> editor.putFloat(key, value.toFloat())
                            is String -> editor.putString(key, value)
                            is org.json.JSONArray -> {
                                val set = mutableSetOf<String>()
                                for (i in 0 until value.length()) {
                                    set.add(value.getString(i))
                                }
                                editor.putStringSet(key, set)
                            }
                        }
                    }
                    editor.commit() // Commit synchronously to ensure immediate rehydration
                }
            }

            // Reload all in-memory repositories
            rehydrateRepositories(context)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring backup: ${e.message}", e)
            false
        }
    }

    private fun rehydrateRepositories(context: Context) {
        try {
            // Re-initialize / reload in-memory state objects
            ExamMarksRepository.init(context)
            ProfileRepository.init(context)
            SettingsRepository.init(context)
            StudyTrackerRepository.init(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error during rehydration: ${e.message}", e)
        }
    }

    // ================= 2. LOCAL FILE BACKUP & RESTORE (SAF) =================

    suspend fun exportBackupToLocalUri(context: Context, uri: Uri): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val json = createFullBackupJson(context)
            val bytes = json.toByteArray(StandardCharsets.UTF_8)
            context.contentResolver.openOutputStream(uri)?.use { os: OutputStream ->
                os.write(bytes)
                os.flush()
            }
            updateLastBackupSuccess(bytes.size.toLong())
            Result.success(bytes.size.toLong())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export local backup: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun importBackupFromLocalUri(context: Context, uri: Uri): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.use { inputStream: InputStream ->
                inputStream.bufferedReader(StandardCharsets.UTF_8).readText()
            } ?: return@withContext Result.failure(Exception("Could not read file"))

            val success = restoreFromBackupJson(context, json)
            if (success) Result.success(true)
            else Result.failure(Exception("Invalid backup format"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import local backup: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ================= 3. GOOGLE DRIVE CLOUD BACKUP & RESTORE =================

    private fun isNetworkConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            true
        }
    }

    private fun sanitizeErrorMessage(rawMessage: String?): String {
        if (rawMessage.isNullOrBlank()) return "Backup failed"
        if (rawMessage.contains("Unable to resolve host", ignoreCase = true) ||
            rawMessage.contains("No address associated with hostname", ignoreCase = true) ||
            rawMessage.contains("UnknownHostException", ignoreCase = true) ||
            rawMessage.contains("NetworkException", ignoreCase = true) ||
            rawMessage.contains("SocketTimeoutException", ignoreCase = true)
        ) {
            return "No internet connection. Data saved locally."
        }
        return rawMessage
    }

    fun notifyDataChanged(context: Context, delayMs: Long = 300_000L) {
        if (autoBackupFrequency.value == "Off" || connectedAccountEmail.value.isNullOrBlank()) return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (backupOverWifiOnly.value) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder().putBoolean("is_periodic", false).build()
        
        val request = OneTimeWorkRequestBuilder<com.marjuk.eaptracker.worker.AutoBackupWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "smart_delay_backup",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun applyPeriodicBackupSchedule(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val freq = autoBackupFrequency.value

        if (freq == "Off" || connectedAccountEmail.value.isNullOrBlank()) {
            workManager.cancelUniqueWork("periodic_backup")
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (backupOverWifiOnly.value) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val intervalDays = when (freq) {
            "Daily" -> 1L
            "Weekly" -> 7L
            "Monthly" -> 30L
            else -> 1L
        }

        val inputData = Data.Builder().putBoolean("is_periodic", true).build()
        
        val request = PeriodicWorkRequestBuilder<com.marjuk.eaptracker.worker.AutoBackupWorker>(intervalDays, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "periodic_backup",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    suspend fun performGoogleDriveBackup(
        context: Context,
        email: String,
        showNotification: Boolean = true,
        isManual: Boolean = true,
        isPeriodic: Boolean = false,
        onConsentRequired: ((android.content.Intent) -> Unit)? = null,
        onProgress: (String) -> Unit = {}
    ): Result<CloudBackupInfo> = withContext(Dispatchers.IO) {
        if (!isNetworkConnected(context)) {
            val errorMsg = "No internet connection. Data saved locally."
            if (showNotification && isManual) {
                NotificationHelper.showBackupCompleteNotification(context, isRestore = false, isSuccess = false, 0L, errorMsg)
            }
            return@withContext Result.failure(Exception(errorMsg))
        }

        isBackupInProgress.value = true
        backupProgressText.value = "Authenticating with Google..."
        onProgress(backupProgressText.value)
        if (showNotification) {
            NotificationHelper.showBackupProgressNotification(context, isRestore = false, 0L, 0L, "Connecting to Google Drive...")
        }

        try {
            when (val authRes = GoogleDriveBackupHelper.getAccessTokenResult(context, email)) {
                is GoogleAuthResult.ConsentRequired -> {
                    if (showNotification) NotificationHelper.dismissBackupProgressNotification(context)
                    onConsentRequired?.invoke(authRes.intent)
                    return@withContext Result.failure(Exception("Please grant Google Drive access in the system prompt"))
                }
                is GoogleAuthResult.Error -> {
                    val cleanMsg = sanitizeErrorMessage(authRes.message)
                    if (showNotification && isManual) {
                        NotificationHelper.showBackupCompleteNotification(context, isRestore = false, isSuccess = false, 0L, cleanMsg)
                    }
                    return@withContext Result.failure(Exception(cleanMsg))
                }
                is GoogleAuthResult.Success -> {
                    val token = authRes.token
                    backupProgressText.value = "Packaging application data..."
                    onProgress(backupProgressText.value)
                    val json = createFullBackupJson(context)
                    val totalBytes = json.toByteArray(StandardCharsets.UTF_8).size.toLong()

                    if (showNotification) {
                        NotificationHelper.showBackupProgressNotification(context, isRestore = false, 0L, totalBytes, "Uploading study data...")
                    }

                    val uploadResult = GoogleDriveBackupHelper.uploadBackup(token, json) { bytesSent, total ->
                        val percent = if (total > 0L) ((bytesSent.toFloat() / total) * 100).toInt() else 0
                        val status = "Uploading ${NotificationHelper.formatBytes(bytesSent)} / ${NotificationHelper.formatBytes(total)} ($percent%)"
                        backupProgressText.value = status
                        onProgress(status)
                        if (showNotification) {
                            NotificationHelper.showBackupProgressNotification(context, isRestore = false, bytesSent, total, status)
                        }
                    }

                    uploadResult.onSuccess { info ->
                        updateLastBackupSuccess(info.sizeBytes, addHistoryNotification = isManual || isPeriodic)
                        setAccountEmail(email)
                        
                        if (showNotification) {
                            if (isManual || isPeriodic) {
                                NotificationHelper.showBackupCompleteNotification(
                                    context = context,
                                    isRestore = false,
                                    isSuccess = true,
                                    sizeBytes = info.sizeBytes,
                                    message = if (isPeriodic) "EAP Tracker cloud backup completed automatically" else "All study data successfully backed up to EAP Tracker Cloud"
                                )
                            } else {
                                NotificationHelper.dismissBackupProgressNotification(context)
                                NotificationHistoryRepository.addNotification(
                                    title = "Smart Backup Complete",
                                    message = "Recent changes (${info.formattedSize}) were safely synced in the background.",
                                    category = "Backup",
                                    actionRoute = "settings",
                                    actionButtonText = "View in Settings"
                                )
                            }
                        }
                    }.onFailure { err ->
                        val cleanMsg = sanitizeErrorMessage(err.localizedMessage)
                        if (showNotification) {
                            if (isManual || isPeriodic) {
                                NotificationHelper.showBackupCompleteNotification(
                                    context = context,
                                    isRestore = false,
                                    isSuccess = false,
                                    sizeBytes = 0L,
                                    message = cleanMsg
                                )
                            } else {
                                NotificationHelper.dismissBackupProgressNotification(context)
                            }
                        }
                    }

                    uploadResult
                }
            }
        } catch (e: Exception) {
            val cleanMsg = sanitizeErrorMessage(e.localizedMessage)
            if (showNotification && isManual) {
                NotificationHelper.showBackupCompleteNotification(context, isRestore = false, isSuccess = false, 0L, cleanMsg)
            }
            Result.failure(e)
        } finally {
            isBackupInProgress.value = false
            backupProgressText.value = ""
        }
    }

    suspend fun fetchLatestDriveBackupInfo(
        context: Context,
        email: String,
        onConsentRequired: ((android.content.Intent) -> Unit)? = null
    ): CloudBackupInfo? = withContext(Dispatchers.IO) {
        try {
            when (val authRes = GoogleDriveBackupHelper.getAccessTokenResult(context, email)) {
                is GoogleAuthResult.ConsentRequired -> {
                    onConsentRequired?.invoke(authRes.intent)
                    null
                }
                is GoogleAuthResult.Success -> {
                    GoogleDriveBackupHelper.queryLatestBackup(authRes.token)
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching drive backup info: ${e.message}", e)
            null
        }
    }

    suspend fun performGoogleDriveRestore(
        context: Context,
        email: String,
        showNotification: Boolean = true,
        onConsentRequired: ((android.content.Intent) -> Unit)? = null,
        onProgress: (String) -> Unit = {}
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!isNetworkConnected(context)) {
            val errorMsg = "No internet connection. Please check your network."
            if (showNotification) {
                NotificationHelper.showBackupCompleteNotification(context, isRestore = true, isSuccess = false, 0L, errorMsg)
            }
            return@withContext Result.failure(Exception(errorMsg))
        }

        isRestoreInProgress.value = true
        backupProgressText.value = "Connecting to Google Drive..."
        onProgress(backupProgressText.value)
        if (showNotification) {
            NotificationHelper.showBackupProgressNotification(context, isRestore = true, 0L, 0L, "Connecting to Google Drive...")
        }

        try {
            when (val authRes = GoogleDriveBackupHelper.getAccessTokenResult(context, email)) {
                is GoogleAuthResult.ConsentRequired -> {
                    if (showNotification) NotificationHelper.dismissBackupProgressNotification(context)
                    onConsentRequired?.invoke(authRes.intent)
                    return@withContext Result.failure(Exception("Please grant Google Drive access in the system prompt"))
                }
                is GoogleAuthResult.Error -> {
                    val cleanMsg = sanitizeErrorMessage(authRes.message)
                    if (showNotification) {
                        NotificationHelper.showBackupCompleteNotification(context, isRestore = true, isSuccess = false, 0L, cleanMsg)
                    }
                    return@withContext Result.failure(Exception(cleanMsg))
                }
                is GoogleAuthResult.Success -> {
                    val token = authRes.token
                    backupProgressText.value = "Searching for cloud backup..."
                    onProgress(backupProgressText.value)
                    val info = GoogleDriveBackupHelper.queryLatestBackup(token)
                        ?: run {
                            if (showNotification) {
                                NotificationHelper.showBackupCompleteNotification(context, isRestore = true, isSuccess = false, 0L, "No backup found on Google Drive")
                            }
                            return@withContext Result.failure(Exception("No EAPTracker backup found on Google Drive for $email"))
                        }

                    val totalBytes = info.sizeBytes
                    if (showNotification) {
                        NotificationHelper.showBackupProgressNotification(context, isRestore = true, 0L, totalBytes, "Downloading backup (${info.formattedSize})...")
                    }

                    val downloadRes = GoogleDriveBackupHelper.downloadBackupContent(token, info.fileId, expectedSizeBytes = totalBytes) { bytesRead, total ->
                        val percent = if (total > 0L) ((bytesRead.toFloat() / total) * 100).toInt() else 0
                        val status = "Downloading ${NotificationHelper.formatBytes(bytesRead)} / ${NotificationHelper.formatBytes(total)} ($percent%)"
                        backupProgressText.value = status
                        onProgress(status)
                        if (showNotification) {
                            NotificationHelper.showBackupProgressNotification(context, isRestore = true, bytesRead, total, status)
                        }
                    }

                    val jsonContent = downloadRes.getOrNull()
                        ?: run {
                            val err = downloadRes.exceptionOrNull() ?: Exception("Download failed")
                            val cleanMsg = sanitizeErrorMessage(err.localizedMessage)
                            if (showNotification) {
                                NotificationHelper.showBackupCompleteNotification(context, isRestore = true, isSuccess = false, 0L, cleanMsg)
                            }
                            return@withContext Result.failure(Exception(cleanMsg))
                        }

                    backupProgressText.value = "Restoring syllabus, scores & progress..."
                    onProgress(backupProgressText.value)
                    val success = restoreFromBackupJson(context, jsonContent)

                    if (success) {
                        if (showNotification) {
                            NotificationHelper.showBackupCompleteNotification(
                                context = context,
                                isRestore = true,
                                isSuccess = true,
                                sizeBytes = info.sizeBytes,
                                message = "All study progress, syllabus & exam scores restored"
                            )
                        }
                        Result.success(true)
                    } else {
                        if (showNotification) {
                            NotificationHelper.showBackupCompleteNotification(context, isRestore = true, isSuccess = false, 0L, "Failed to parse backup payload")
                        }
                        Result.failure(Exception("Failed to parse backup payload"))
                    }
                }
            }
        } catch (e: Exception) {
            val cleanMsg = sanitizeErrorMessage(e.localizedMessage)
            if (showNotification) {
                NotificationHelper.showBackupCompleteNotification(context, isRestore = true, isSuccess = false, 0L, cleanMsg)
            }
            Result.failure(e)
        } finally {
            isRestoreInProgress.value = false
            backupProgressText.value = ""
        }
    }

    fun getFormattedLastBackupTime(): String {
        val t = lastBackupTimestamp.longValue
        if (t <= 0L) return "Never"
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        return sdf.format(Date(t))
    }

    fun getFormattedLastBackupSize(): String {
        val s = lastBackupSizeBytes.longValue
        if (s <= 0L) return ""
        return if (s < 1024) "$s B"
        else if (s < 1024 * 1024) "${s / 1024} KB"
        else String.format("%.1f MB", s / (1024f * 1024f))
    }
}