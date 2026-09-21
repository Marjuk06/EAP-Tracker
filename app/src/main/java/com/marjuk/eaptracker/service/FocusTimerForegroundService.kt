package com.marjuk.eaptracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.marjuk.eaptracker.MainActivity
import com.marjuk.eaptracker.R
import com.marjuk.eaptracker.ui.StudyTrackerRepository

/**
 * FocusTimerForegroundService
 * Powers the Dedicated Large-Format Live Chronometer Notification Card.
 * Non-dismissible while active and accurately synced with SystemClock.elapsedRealtime().
 */
class FocusTimerForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        val subject = intent?.getStringExtra(EXTRA_SUBJECT) ?: "Self Study & Problem Solving"

        when (action) {
            ACTION_START -> {
                createNotificationChannel()
                val elapsedSeconds = intent?.getIntExtra(EXTRA_ELAPSED_SECONDS, 0) ?: 0
                val notification = buildLiveNotification(subject, elapsedSeconds)
                startForeground(NOTIFICATION_ID, notification)
            }
            ACTION_PAUSE -> {
                StudyTrackerRepository.pauseFocusTimer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_FINISH -> {
                StudyTrackerRepository.logAndSaveSession()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Live Focus Timer",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Ongoing study focus session with large live chronometer"
                setShowBadge(true)
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildLiveNotification(subject: String, elapsedSeconds: Int): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "fullscreen_focus")
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            REQ_OPEN_APP,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        // Action: Pause
        val pauseIntent = Intent(this, FocusTimerForegroundService::class.java).apply {
            action = ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            this,
            REQ_PAUSE,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        // Action: Finish & Save
        val finishIntent = Intent(this, FocusTimerForegroundService::class.java).apply {
            action = ACTION_FINISH
        }
        val finishPendingIntent = PendingIntent.getService(
            this,
            REQ_FINISH,
            finishIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        // Precise Chronometer Base (SystemClock.elapsedRealtime)
        val baseElapsedRealtime = SystemClock.elapsedRealtime() - (elapsedSeconds * 1000L)

        // Custom Compact RemoteViews
        val smallView = RemoteViews(packageName, R.layout.notification_focus_timer_small).apply {
            setTextViewText(R.id.notif_title, "Study Focus")
            setTextViewText(R.id.notif_subject, subject)
            setChronometer(R.id.notif_chronometer_small, baseElapsedRealtime, null, true)
        }

        // Custom Expanded RemoteViews with BIG Center Chronometer
        val largeView = RemoteViews(packageName, R.layout.notification_focus_timer_large).apply {
            setTextViewText(R.id.notif_large_title, "Study Focus Session")
            setTextViewText(R.id.notif_large_subject, subject)
            setChronometer(R.id.notif_chronometer_large, baseElapsedRealtime, null, true)
            setOnClickPendingIntent(R.id.btn_notif_pause, pausePendingIntent)
            setOnClickPendingIntent(R.id.btn_notif_finish, finishPendingIntent)
            setOnClickPendingIntent(R.id.btn_notif_open, openPendingIntent)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Study Focus: $subject")
            .setContentText("Focus session in progress")
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openPendingIntent)
            .setCustomContentView(smallView)
            .setCustomBigContentView(largeView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .build()

        notification.flags = notification.flags or Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
        return notification
    }

    companion object {
        const val CHANNEL_ID = "eap_live_focus_channel"
        const val NOTIFICATION_ID = 4001

        const val ACTION_START = "com.marjuk.eaptracker.action.START_FOCUS"
        const val ACTION_PAUSE = "com.marjuk.eaptracker.action.PAUSE_FOCUS"
        const val ACTION_FINISH = "com.marjuk.eaptracker.action.FINISH_FOCUS"
        const val ACTION_STOP = "com.marjuk.eaptracker.action.STOP_FOCUS"

        const val EXTRA_SUBJECT = "extra_subject"
        const val EXTRA_ELAPSED_SECONDS = "extra_elapsed_seconds"

        fun startService(context: Context, subject: String, elapsedSeconds: Int = 0) {
            val intent = Intent(context, FocusTimerForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SUBJECT, subject)
                putExtra(EXTRA_ELAPSED_SECONDS, elapsedSeconds)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FocusTimerForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private const val REQ_OPEN_APP = 4101
        private const val REQ_PAUSE = 4102
        private const val REQ_FINISH = 4103
    }
}
