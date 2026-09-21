package com.marjuk.eaptracker.service

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.marjuk.eaptracker.ui.NotificationHelper
import com.marjuk.eaptracker.ui.NotificationHistoryRepository
import com.marjuk.eaptracker.ui.admin.RemoteSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.google.firebase.database.FirebaseDatabase
import com.marjuk.eaptracker.ui.admin.StudentTelemetryManager

class EAPFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token generated: $token")
        // Automatically subscribe device to the master updates topic
        subscribeToTopics()
        // Automatically upload device token to Firebase RTDB under student's record
        uploadFcmToken(applicationContext, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Instant FCM Push Received from: ${remoteMessage.from}")

        // 1. Extract notification attributes from payload or data map
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "📅 EAP Schedule Updated!"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: "New class routine or exam schedule has been published. Tap to view latest dates."

        val category = remoteMessage.data["category"] ?: "Notice"
        val actionUrl = remoteMessage.data["actionUrl"]?.ifBlank { null }
        val actionButtonText = (remoteMessage.data["actionButtonText"] ?: remoteMessage.data["buttonText"])?.ifBlank { null }
        val actionRoute = (remoteMessage.data["actionRoute"] ?: remoteMessage.data["route"])?.ifBlank { null }
        val timestamp = (remoteMessage.data["timestamp"]?.toLongOrNull()) ?: System.currentTimeMillis()

        val apkUrl = (remoteMessage.data["apkUrl"] ?: remoteMessage.data["apk_url"])?.ifBlank { null }
        val versionName = remoteMessage.data["versionName"] ?: remoteMessage.data["version_name"] ?: ""
        val versionCode = (remoteMessage.data["versionCode"] ?: remoteMessage.data["version_code"])?.toIntOrNull() ?: 0
        val isForceUpdate = (remoteMessage.data["isForce"] ?: remoteMessage.data["is_force"])?.toBooleanStrictOrNull() ?: false
        val isAppUpdateType = remoteMessage.data["type"] == "app_update" || !apkUrl.isNullOrBlank() || category.equals("Update", ignoreCase = true)

        // 2. Initialize Repositories
        NotificationHelper.init(applicationContext)
        NotificationHistoryRepository.init(applicationContext)
        RemoteSyncManager.init(applicationContext)
        com.marjuk.eaptracker.ui.admin.AppUpdateManager.init(applicationContext)

        if (isAppUpdateType && !apkUrl.isNullOrBlank()) {
            com.marjuk.eaptracker.ui.admin.AppUpdateManager.handleRemoteUpdatePayload(
                context = applicationContext,
                apkUrl = apkUrl,
                versionName = versionName,
                versionCode = versionCode,
                title = title,
                changelog = body,
                isForceUpdate = isForceUpdate
            )
        }

        // 3. Mark timestamp as seen so local listeners won't double-fire
        try {
            val prefs = applicationContext.getSharedPreferences("eap_remote_sync_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putLong("last_seen_broadcast_time", timestamp)
                .putLong("last_notified_config_version", timestamp)
                .apply()
        } catch (_: Exception) {}

        // 4. Record to Notification Inbox History
        NotificationHistoryRepository.addNotification(
            title = title,
            message = body,
            category = if (isAppUpdateType) "Update" else category,
            timestamp = timestamp,
            actionRoute = actionRoute ?: if (isAppUpdateType) "app_update" else null,
            actionUrl = actionUrl ?: apkUrl,
            actionButtonText = actionButtonText ?: if (isAppUpdateType) "Update Now" else null
        )

        // 5. Show Instant High-Priority Android System Notification
        NotificationHelper.showScheduleUpdateNotification(
            context = applicationContext,
            title = title,
            message = body,
            actionUrl = actionUrl ?: apkUrl,
            actionButtonText = actionButtonText ?: if (isAppUpdateType) "Update Now" else null,
            actionRoute = actionRoute ?: if (isAppUpdateType) "app_update" else null
        )

        // 6. Trigger background silent cloud data sync so data is refreshed immediately
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RemoteSyncManager.fetchFromCloud(applicationContext) { _, _ -> }
            } catch (e: Exception) {
                Log.e(TAG, "Failed background sync on FCM push", e)
            }
        }
    }

    companion object {
        private const val TAG = "EAP_FCM_Service"
        const val TOPIC_UPDATES = "eap_updates"
        const val TOPIC_STUDENTS = "eap_students"
        private const val RTDB_URL = "https://eap-tracker-default-rtdb.firebaseio.com"

        fun subscribeToTopics() {
            try {
                FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_UPDATES)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Subscribed to FCM topic: $TOPIC_UPDATES")
                        }
                    }
                FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_STUDENTS)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Subscribed to FCM topic: $TOPIC_STUDENTS")
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error subscribing to FCM topics", e)
            }
        }

        fun uploadFcmToken(context: Context, token: String) {
            if (token.isBlank()) return
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val studentKey = StudentTelemetryManager.getStudentKey()
                    if (studentKey == "student_unregistered") return@launch

                    val database = FirebaseDatabase.getInstance(RTDB_URL)
                    val studentRef = database.getReference("students").child(studentKey)

                    studentRef.child("fcm_token").setValue(token)
                    studentRef.child("profile_summary").child("fcm_token").setValue(token)
                    Log.d(TAG, "Uploaded student FCM token to RTDB for $studentKey")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to upload FCM token to RTDB", e)
                }
            }
        }
    }
}

