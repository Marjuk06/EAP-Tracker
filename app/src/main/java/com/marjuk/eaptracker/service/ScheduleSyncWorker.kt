package com.marjuk.eaptracker.service

import android.content.Context
import android.os.PowerManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.marjuk.eaptracker.ui.NotificationHelper
import com.marjuk.eaptracker.ui.NotificationHistoryRepository
import com.marjuk.eaptracker.ui.ProfileRepository
import com.marjuk.eaptracker.ui.SettingsRepository
import com.marjuk.eaptracker.ui.admin.RemoteSyncManager
import com.marjuk.eaptracker.ui.admin.StudentTelemetryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Background WorkManager Worker.
 * Ensures that notifications (Daily Routine, Personal Mentor Notices, Broadcast Alerts, and Schedule Updates)
 * are reliably fetched and delivered even when the application is completely closed or killed.
 */
class ScheduleSyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val BASE_URL = "https://eap-tracker-default-rtdb.firebaseio.com"
        private const val AUTH_KEY = "yleDrwUbDfga8ueOKchQEZ47XCPlrzlRNiCF1NAw"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "EAPTracker:ScheduleSyncWorkerWakeLock"
        )
        wakeLock?.acquire(60_000L) // 60 seconds max

        try {
            NotificationHelper.init(context)
            NotificationHistoryRepository.init(context)
            RemoteSyncManager.init(context)
            SettingsRepository.init(context)
            ProfileRepository.init(context)

            // 1. Deliver today's routine schedule if not already delivered today
            if (SettingsRepository.isSetupCompleted.value) {
                NotificationHelper.checkAndDeliverTodaysScheduleOnFirstOpen(context)
            }

            // 2. Poll for Realtime Broadcast Notification (Admin instant notices)
            fetchAndDeliverBroadcastNotification()

            // 3. Poll for Master EAP Data Changes (Class routines, exam schedules, syllabus)
            fetchAndDeliverMasterEapData()

            // 4. Poll for Personal Mentor Notice for this student
            val studentKey = StudentTelemetryManager.getStudentKey()
            if (studentKey != "student_unregistered") {
                fetchAndDeliverPersonalNotice(studentKey)
            }

            // 5. Poll for Global Broadcast Notifications
            fetchAndDeliverGlobalNotification()

            // 6. Poll for Target Exam Countdown & Daily Quote
            fetchAndDeliverTargetExamAndQuote()

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        } finally {
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock.release()
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Polls /broadcast_notification.json to deliver instant admin notices
     */
    private fun fetchAndDeliverBroadcastNotification() {
        try {
            val urlStr = "$BASE_URL/broadcast_notification.json?auth=$AUTH_KEY"
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                if (responseText.isNotBlank() && responseText != "null") {
                    val json = JSONObject(responseText)
                    val timestamp = json.optLong("timestamp", 0L)
                    val prefs = context.getSharedPreferences("eap_remote_sync_prefs", Context.MODE_PRIVATE)
                    val lastSeen = prefs.getLong("last_seen_broadcast_time", 0L)

                    if (timestamp > 0L && timestamp != lastSeen) {
                        val title = json.optString("title", "EAP Notice").ifBlank { "EAP Notice" }
                        val message = json.optString("message", "")
                        val category = json.optString("category", "Notice")
                        val actionUrl = json.optString("actionUrl", "").ifBlank { null }
                        val actionButtonText = json.optString("actionButtonText", "").ifBlank { null }
                        val actionRoute = json.optString("actionRoute", "").ifBlank { null }

                        if (message.isNotBlank()) {
                            NotificationHistoryRepository.addNotification(
                                title = title,
                                message = message,
                                category = category,
                                timestamp = timestamp,
                                actionRoute = actionRoute,
                                actionUrl = actionUrl,
                                actionButtonText = actionButtonText
                            )
                            // Disabled polling-based notifications per architectural requirements (FCM is the primary push mechanism)
                            /* NotificationHelper.showScheduleUpdateNotification(
                                context = context,
                                title = title,
                                message = message,
                                actionUrl = actionUrl,
                                actionButtonText = actionButtonText,
                                actionRoute = actionRoute
                            ) */
                        }
                        prefs.edit().putLong("last_seen_broadcast_time", timestamp).apply()
                    }
                }
            }
            conn.disconnect()
        } catch (_: Exception) {}
    }

    /**
     * Polls /eap_data.json and updates dynamic routines, exams, and syllabus
     */
    private fun fetchAndDeliverMasterEapData() {
        try {
            val urlStr = "$BASE_URL/eap_data.json?auth=$AUTH_KEY"
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 12000
            conn.readTimeout = 12000

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                if (responseText.isNotBlank() && responseText != "null") {
                    RemoteSyncManager.importMasterPayload(responseText, context)
                }
            }
            conn.disconnect()
        } catch (_: Exception) {}
    }

    /**
     * Polls personal mentor notice for individual student
     */
    private fun fetchAndDeliverPersonalNotice(studentKey: String) {
        try {
            val urlStr = "$BASE_URL/students/$studentKey/personal_notice.json?auth=$AUTH_KEY"
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                if (responseText.isNotBlank() && responseText != "null") {
                    val json = JSONObject(responseText)
                    val title = json.optString("title", "Personal Mentor Update")
                    val message = json.optString("message", "")
                    val actionUrl = json.optString("actionUrl", "")
                    val buttonText = json.optString("actionButtonText", "View Resource")
                    val timestamp = json.optLong("timestamp", System.currentTimeMillis())

                    if (message.isNotBlank()) {
                        val prefs = context.getSharedPreferences("notice_sync_prefs", Context.MODE_PRIVATE)
                        val lastDeliveredTs = prefs.getLong("last_personal_notice_ts", 0L)

                        if (timestamp > lastDeliveredTs) {
                            // Fallback: show system notification (FCM is the primary path;
                            // this fires when the app was killed before FCM token was registered,
                            // or when FCM delivery failed for any reason)
                            NotificationHelper.showPersonalNoticeNotification(
                                context = context,
                                title = title,
                                message = message,
                                actionUrl = actionUrl.ifBlank { null },
                                buttonText = buttonText.ifBlank { null }
                            )
                            NotificationHistoryRepository.addNotification(
                                title = title,
                                message = message,
                                category = "Personal",
                                actionUrl = actionUrl.ifBlank { null },
                                actionButtonText = buttonText.ifBlank { null }
                            )
                            prefs.edit().putLong("last_personal_notice_ts", timestamp).apply()
                        }
                    }
                }
            }
            conn.disconnect()
        } catch (_: Exception) {}
    }

    /**
     * Polls global notices
     */
    private fun fetchAndDeliverGlobalNotification() {
        try {
            val urlStr = "$BASE_URL/global_notifications/latest.json?auth=$AUTH_KEY"
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                if (responseText.isNotBlank() && responseText != "null") {
                    val json = JSONObject(responseText)
                    val title = json.optString("title", "Important Academic Announcement")
                    val message = json.optString("message", "")
                    val actionUrl = json.optString("actionUrl", "")
                    val buttonText = json.optString("actionButtonText", "Open")
                    val timestamp = json.optLong("timestamp", System.currentTimeMillis())

                    if (message.isNotBlank()) {
                        val prefs = context.getSharedPreferences("notice_sync_prefs", Context.MODE_PRIVATE)
                        val lastGlobalTs = prefs.getLong("last_global_notice_ts", 0L)

                        if (timestamp > lastGlobalTs) {
                            // Fallback: show system notification (FCM topic eap_updates is the primary path;
                            // this fires if FCM was not yet delivered when WorkManager ran)
                            NotificationHelper.showPersonalNoticeNotification(
                                context = context,
                                title = title,
                                message = message,
                                actionUrl = actionUrl.ifBlank { null },
                                buttonText = buttonText.ifBlank { null }
                            )
                            NotificationHistoryRepository.addNotification(
                                title = title,
                                message = message,
                                category = "Academic",
                                actionUrl = actionUrl.ifBlank { null },
                                actionButtonText = buttonText.ifBlank { null }
                            )
                            prefs.edit().putLong("last_global_notice_ts", timestamp).apply()
                        }
                    }
                }
            }
            conn.disconnect()
        } catch (_: Exception) {}
    }

    /**
     * Polls official target exam countdown and daily quote
     */
    private fun fetchAndDeliverTargetExamAndQuote() {
        try {
            // Target exam countdown
            val examUrlStr = "$BASE_URL/target_exam_config.json?auth=$AUTH_KEY"
            val examUrl = URL(examUrlStr)
            val examConn = examUrl.openConnection() as HttpURLConnection
            examConn.requestMethod = "GET"
            examConn.connectTimeout = 8000
            examConn.readTimeout = 8000
            if (examConn.responseCode == 200) {
                val resp = examConn.inputStream.bufferedReader().use(BufferedReader::readText)
                if (resp.isNotBlank() && resp != "null") {
                    val json = JSONObject(resp)
                    val examName = json.optString("examName", "")
                    val examEpoch = json.optLong("examEpoch", 0L)
                    if (examName.isNotBlank() && examEpoch > 0L) {
                        ProfileRepository.updateCountdown(examName, examEpoch)
                    }
                }
            }
            examConn.disconnect()
        } catch (_: Exception) {}

        try {
            // Daily motivation quote
            val quoteUrlStr = "$BASE_URL/daily_quote_config.json?auth=$AUTH_KEY"
            val quoteUrl = URL(quoteUrlStr)
            val quoteConn = quoteUrl.openConnection() as HttpURLConnection
            quoteConn.requestMethod = "GET"
            quoteConn.connectTimeout = 8000
            quoteConn.readTimeout = 8000
            if (quoteConn.responseCode == 200) {
                val resp = quoteConn.inputStream.bufferedReader().use(BufferedReader::readText)
                if (resp.isNotBlank() && resp != "null") {
                    val json = JSONObject(resp)
                    val quoteText = json.optString("quoteText", "")
                    val author = json.optString("author", "Admission Mentor")
                    val timestamp = json.optLong("timestamp", System.currentTimeMillis())
                    if (quoteText.isNotBlank()) {
                        val prefs = context.getSharedPreferences("eap_remote_sync_prefs", Context.MODE_PRIVATE)
                        val prevQuote = prefs.getString("quote", null)
                        ProfileRepository.quote.value = quoteText
                        prefs.edit().putString("quote", quoteText).apply()

                        if (prevQuote != quoteText) {
                            NotificationHistoryRepository.addNotification(
                                title = "Daily Inspiration",
                                message = "\u201c$quoteText\u201d — $author",
                                category = "Motivation",
                                timestamp = timestamp
                            )
                        }
                    }
                }
            }
            quoteConn.disconnect()
        } catch (_: Exception) {}
    }
}

