package com.marjuk.eaptracker.ui

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.marjuk.eaptracker.MainActivity
import com.marjuk.eaptracker.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

object NotificationHelper {
    const val CHANNEL_QUOTES = "eap_quotes_channel"
    const val CHANNEL_ROUTINE = "eap_routine_channel"
    const val CHANNEL_PROGRESS = "eap_progress_channel"
    const val CHANNEL_COUNTDOWN = "eap_countdown_channel"
    const val CHANNEL_SCHEDULE_UPDATES = "eap_schedule_updates_channel"
    const val CHANNEL_BACKUP_PROGRESS = "eap_backup_progress_channel"
    const val CHANNEL_BACKUP_COMPLETE = "eap_backup_complete_channel"

    const val ACTION_QUOTE_REMINDER = "com.marjuk.eaptracker.ACTION_QUOTE_REMINDER"
    const val ACTION_NEXT_QUOTE = "com.marjuk.eaptracker.ACTION_NEXT_QUOTE"
    const val ACTION_COPY_QUOTE = "com.marjuk.eaptracker.ACTION_COPY_QUOTE"
    const val ACTION_MORNING_ROUTINE = "com.marjuk.eaptracker.ACTION_MORNING_ROUTINE"
    const val ACTION_EVENING_PROGRESS = "com.marjuk.eaptracker.ACTION_EVENING_PROGRESS"
    const val ACTION_COUNTDOWN_REMINDER = "com.marjuk.eaptracker.ACTION_COUNTDOWN_REMINDER"
    const val ACTION_SCHEDULE_SYNC_CHECK = "com.marjuk.eaptracker.ACTION_SCHEDULE_SYNC_CHECK"

    private const val REQ_QUOTE = 1001
    private const val REQ_MORNING = 1002
    private const val REQ_EVENING = 1003
    private const val REQ_COUNTDOWN = 1004
    private const val REQ_SCHEDULE_SYNC = 1005

    private const val NOTIF_QUOTE_ID = 2001
    private const val NOTIF_MORNING_ID = 2002
    private const val NOTIF_EVENING_ID = 2003
    private const val NOTIF_COUNTDOWN_ID = 2004
    private const val NOTIF_TEST_ID = 2005
    private const val NOTIF_SCHEDULE_UPDATE_ID = 2006
    private const val NOTIF_BACKUP_PROGRESS_ID = 3001
    private const val NOTIF_BACKUP_COMPLETE_ID = 3002

    fun init(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // 1. Motivational Quotes Channel
            val quotesChannel = NotificationChannel(
                CHANNEL_QUOTES,
                "Motivational Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Periodic focus and motivation boosts with interactive actions"
                enableVibration(true)
            }

            // 2. Daily Exam & Routine Channel
            val routineChannel = NotificationChannel(
                CHANNEL_ROUTINE,
                "Daily Routine & Exams",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Morning routine reminders, class topics, and scheduled exams"
                enableVibration(true)
            }

            // 3. Evening Progress Channel
            val progressChannel = NotificationChannel(
                CHANNEL_PROGRESS,
                "Evening Review & Marks",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Evening prompt to record exam marks and check off QB chapters"
                enableVibration(true)
            }

            // 4. Target Exam Countdown Channel
            val countdownChannel = NotificationChannel(
                CHANNEL_COUNTDOWN,
                "Admission Countdown",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily countdown alerts until your target admission exam"
                enableVibration(true)
            }

            // 5. Real-Time Schedule & Routine Updates Channel
            val updatesChannel = NotificationChannel(
                CHANNEL_SCHEDULE_UPDATES,
                "Schedule & Routine Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Instant notifications when new class routines, exam dates, or syllabus updates are published"
                enableVibration(true)
            }

            // 6. Cloud Backup Ongoing Progress Channel
            val backupProgressChannel = NotificationChannel(
                CHANNEL_BACKUP_PROGRESS,
                "Backup & Restore Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Live ongoing byte progress during cloud backup and restore"
                setShowBadge(false)
            }

            // 7. Cloud Backup Completion Channel
            val backupCompleteChannel = NotificationChannel(
                CHANNEL_BACKUP_COMPLETE,
                "Backup & Restore Confirmations",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Status confirmation when cloud backup or restore finishes"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(quotesChannel)
            notificationManager.createNotificationChannel(routineChannel)
            notificationManager.createNotificationChannel(progressChannel)
            notificationManager.createNotificationChannel(countdownChannel)
            notificationManager.createNotificationChannel(updatesChannel)
            notificationManager.createNotificationChannel(backupProgressChannel)
            notificationManager.createNotificationChannel(backupCompleteChannel)
        }

        rescheduleAll(context)
    }

    fun isQuietSleepTime(): Boolean {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val start = SettingsRepository.sleepStartHour.intValue
        val end = SettingsRepository.sleepEndHour.intValue

        return if (start > end) {
            currentHour >= start || currentHour < end
        } else {
            currentHour in start until end
        }
    }

    private fun getNavPendingIntent(context: Context, destination: String, reqCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", destination)
        }
        return PendingIntent.getActivity(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )
    }

    private fun getActionPendingIntent(context: Context, action: String, extraQuote: String?, reqCode: Int): PendingIntent {
        val intent = Intent(context, QuoteAlarmReceiver::class.java).apply {
            this.action = action
            if (extraQuote != null) putExtra("quote_text", extraQuote)
        }
        return PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )
    }

    // 1. Show Motivational Quote Notification (with [🎲 Next Quote] and [📋 Copy Quote] actions)
    fun showQuoteNotification(context: Context, quoteItem: QuoteItem) {
        if (!SettingsRepository.notificationsMaster.value || !SettingsRepository.quoteNotificationsEnabled.value) return
        if (isQuietSleepTime()) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val nextQuoteIntent = getActionPendingIntent(context, ACTION_NEXT_QUOTE, null, 3001)
        val copyQuoteIntent = getActionPendingIntent(context, ACTION_COPY_QUOTE, quoteItem.quote, 3002)
        val openAppIntent = getNavPendingIntent(context, "profile", 3003)

        val title = "${quoteItem.category.uppercase()} BOOST"
        val bigText = "“${quoteItem.quote}”\n\n${quoteItem.category} Momentum"

        val notification = NotificationCompat.Builder(context, CHANNEL_QUOTES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("“${quoteItem.quote}”")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bigText)
                    .setSummaryText("Focus Boost")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openAppIntent)
            .addAction(R.drawable.ic_notification, "Next Quote", nextQuoteIntent)
            .addAction(R.drawable.ic_notification, "Copy Quote", copyQuoteIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIF_QUOTE_ID, notification)
    }

    // 2. Show Morning Routine & Exam Alert (with [View Routine] and [Today's Exam] actions)
    fun showMorningRoutineNotification(context: Context) {
        if (!SettingsRepository.notificationsMaster.value || !SettingsRepository.dailyRoutineAlert.value) return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val mode = SettingsRepository.defaultRoutineMode.value
        val routineList = com.marjuk.eaptracker.ui.admin.DynamicRoutineRepository.getRoutines(mode).ifEmpty {
            if (mode.equals("Online", ignoreCase = true)) onlineRoutine else offlineRoutine
        }

        val todayCalendar = Calendar.getInstance()
        val dayOfWeek = todayCalendar.get(Calendar.DAY_OF_WEEK)
        val dayName = when (dayOfWeek) {
            Calendar.SATURDAY -> "Sat"
            Calendar.SUNDAY -> "Sun"
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            else -> "Today"
        }

        val sdfStandard = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val sdfShort = SimpleDateFormat("dd-MMM-yy", Locale.ENGLISH)
        val sdfDayMonth = SimpleDateFormat("dd MMM", Locale.ENGLISH)
        val dateNow = Date()

        val dateStrStandard = sdfStandard.format(dateNow)
        val dateStrShort = sdfShort.format(dateNow)
        val dateStrDayMonth = sdfDayMonth.format(dateNow)

        val todayItem = routineList.find { 
            it.date.equals(dateStrStandard, ignoreCase = true) ||
            it.date.equals(dateStrShort, ignoreCase = true) ||
            it.date.contains(dateStrDayMonth, ignoreCase = true)
        } ?: routineList.find { it.day.startsWith(dayName, ignoreCase = true) } 
          ?: routineList.firstOrNull()

        val subject = todayItem?.classSubject ?: "Self Study & Concept Practice"
        val isOrientation = subject.contains("Orientation", ignoreCase = true)
        val exam = when (val details = todayItem?.examDetails) {
            is String -> details
            is List<*> -> details.filterIsInstance<String>().joinToString(" • ")
            else -> if (isOrientation) "No Exam (Course Orientation Day)" else "Daily Preparation Test"
        }
        val topics = todayItem?.topics?.joinToString(", ") ?: "Theory Revision & Question Bank (QB) Practice"

        val title = if (isOrientation) "Special Event: $subject" else "Today's Schedule • $mode"
        val bodyText = if (isOrientation) {
            "$subject\nTopics: $topics\nWelcome to your Engineering Admission Journey!"
        } else {
            "Class: $subject\nExam: $exam\nTopics: $topics"
        }

        val viewRoutineIntent = getNavPendingIntent(context, "routine", 3011)
        val viewExamsIntent = getNavPendingIntent(context, "exams", 3012)

        val notification = NotificationCompat.Builder(context, CHANNEL_ROUTINE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("Class: $subject | Exam: $exam")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bodyText)
                    .setSummaryText("$mode Routine • $dayName")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(viewRoutineIntent)
            .addAction(R.drawable.ic_notification, "View Routine", viewRoutineIntent)
            .addAction(R.drawable.ic_notification, "Today's Exam", viewExamsIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIF_MORNING_ID, notification)
    }

    /**
     * Check if user opened or installed the app during the day (e.g. 4 PM)
     * and deliver today's schedule immediately if not already delivered today.
     */
    fun checkAndDeliverTodaysScheduleOnFirstOpen(context: Context) {
        if (!SettingsRepository.isSetupCompleted.value) return
        try {
            val prefs = context.getSharedPreferences("notification_delivery_prefs", Context.MODE_PRIVATE)
            val todayDateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val lastDelivered = prefs.getString("last_delivered_schedule_date", null)

            if (lastDelivered != todayDateKey) {
                showMorningRoutineNotification(context)
                prefs.edit().putString("last_delivered_schedule_date", todayDateKey).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 3. Show Evening Progress & Streak Wrap-up (with [Input Exam Score] and [Check Off Chapters] actions)
    fun showEveningProgressNotification(context: Context) {
        if (!SettingsRepository.notificationsMaster.value || !SettingsRepository.eveningProgressAlert.value) return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val (completedChapters, totalChapters) = SyllabusRepository.getTotalChaptersStats()
        val syllabusPct = if (totalChapters > 0) (completedChapters * 100 / totalChapters) else 0
        val (examsCount, totalObtained, totalMax) = ProgressRepository.getOverallExamStats()
        val examAvg = if (totalMax > 0f) (totalObtained * 100f / totalMax).toInt() else 0
        val streak = ProgressRepository.currentStreak.value

        StudyTrackerRepository.init(context)
        val todayMins = StudyTrackerRepository.todayStudyMinutes.intValue
        val goalHours = ProfileRepository.dailyStudyHoursGoal.intValue
        val studyHoursStr = String.format("%.1f", todayMins / 60f)
        val studySummary = "Today's Study: ${studyHoursStr} / ${goalHours}.0 hrs"

        val title = "$streak-Day Study Streak • Evening Review"
        val bodyText = "Syllabus: $syllabusPct% Complete ($completedChapters/$totalChapters Chapters)\n" +
                "Exams: $examsCount Logged | Avg: $examAvg%\n" +
                "$studySummary\n\n" +
                "Did you complete today's Question Bank (QB) target? Don't break the chain!"

        val inputScoreIntent = getNavPendingIntent(context, "exams", 3021)
        val checkChaptersIntent = getNavPendingIntent(context, "syllabus", 3022)

        val notification = NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("Syllabus: $syllabusPct% • $examsCount Exams Logged")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bodyText)
                    .setSummaryText("Daily Wrap-up • Keep the chain!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(inputScoreIntent)
            .addAction(R.drawable.ic_notification, "Input Exam Score", inputScoreIntent)
            .addAction(R.drawable.ic_notification, "Check Off Chapters", checkChaptersIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIF_EVENING_ID, notification)
    }

    // 4. Show Target Exam Countdown Banner (with [Open Tracker] and [Copy Motto] actions)
    fun showCountdownNotification(context: Context) {
        if (!SettingsRepository.notificationsMaster.value || !SettingsRepository.countdownAlertEnabled.value) return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val targetName = ProfileRepository.targetExamName.value
        val quote = ProfileRepository.quote.value
        val targetEpoch = ProfileRepository.targetExamDateEpoch.longValue
        val remainingMillis = maxOf(0L, targetEpoch - System.currentTimeMillis())
        val days = remainingMillis / (1000 * 60 * 60 * 24)

        val title = "$days Days Remaining • $targetName"
        val bodyText = if (quote.isNotBlank()) {
            "“$quote”\n\nStay consistent! Every problem solved brings you one step closer to your goal."
        } else {
            "Stay consistent! Every problem solved brings you one step closer to your goal."
        }

        val openAppIntent = getNavPendingIntent(context, "home", 3031)
        val copyMottoIntent = getActionPendingIntent(context, ACTION_COPY_QUOTE, quote, 3032)

        val notification = NotificationCompat.Builder(context, CHANNEL_COUNTDOWN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("$days days remaining until $targetName")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bodyText)
                    .setSummaryText("$days Days Remaining")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openAppIntent)
            .addAction(R.drawable.ic_notification, "Open Tracker", openAppIntent)
            .apply {
                if (quote.isNotBlank()) {
                    addAction(R.drawable.ic_notification, "Copy Motto", copyMottoIntent)
                }
            }
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIF_COUNTDOWN_ID, notification)
    }

    // 5. Test Live Notifications
    fun showTestNotification(context: Context, type: String = "quote") {
        when (type) {
            "routine" -> showMorningRoutineNotification(context)
            "evening" -> showEveningProgressNotification(context)
            "countdown" -> showCountdownNotification(context)
            "all" -> {
                showMorningRoutineNotification(context)
                showEveningProgressNotification(context)
                showCountdownNotification(context)
                val quote = QuotesRepository.getRandomQuote()
                showQuoteNotification(context, quote)
            }
            else -> {
                val quote = QuotesRepository.getRandomQuote()
                showQuoteNotification(context, quote)
            }
        }
    }

    // 5. Show Schedule Update / Broadcast Notification (with custom URL links & routes)
    fun showScheduleUpdateNotification(
        context: Context,
        title: String,
        message: String,
        actionUrl: String? = null,
        actionButtonText: String? = null,
        actionRoute: String? = null
    ) {
        init(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val cleanTitle = title.replace(Regex("[🔥🚀💡⚡✨📢🎯🌟📖]"), "").trim().ifBlank { "EAP Notice" }
        val cleanMessage = message.replace(Regex("[🔥🚀💡⚡✨📢🎯🌟📖]"), "").trim()

        val openAppIntent = if (!actionRoute.isNullOrBlank()) {
            getNavPendingIntent(context, actionRoute, 3041)
        } else {
            getNavPendingIntent(context, "home", 3041)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_SCHEDULE_UPDATES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(cleanTitle)
            .setContentText(cleanMessage)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(cleanMessage)
                    .setSummaryText("EAP Tracker Alert")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)

        if (!actionUrl.isNullOrBlank()) {
            try {
                val linkIntent = Intent(Intent.ACTION_VIEW, Uri.parse(actionUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val linkPendingIntent = PendingIntent.getActivity(
                    context,
                    (System.currentTimeMillis() % 10000).toInt() + 5000,
                    linkIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                )
                builder.setContentIntent(linkPendingIntent)
                builder.addAction(
                    R.drawable.ic_notification,
                    actionButtonText?.ifBlank { "Open Link" } ?: "Open Link",
                    linkPendingIntent
                )
            } catch (_: Exception) {
                builder.setContentIntent(openAppIntent)
            }
        } else {
            builder.setContentIntent(openAppIntent)
            val openRoutineIntent = getNavPendingIntent(context, "full_routine_offline", 3042)
            val openExamsIntent = getNavPendingIntent(context, "full_exams_offline", 3043)
            builder.addAction(R.drawable.ic_notification, "View Routine", openRoutineIntent)
            builder.addAction(R.drawable.ic_notification, "View Exams", openExamsIntent)
        }
        val notifId = kotlin.math.abs(cleanTitle.hashCode() xor cleanMessage.hashCode())
        if (notifId == 0) {
            notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
        } else {
            notificationManager.notify(notifId, builder.build())
        }
    }

    fun showPersonalNoticeNotification(
        context: Context,
        title: String,
        message: String,
        actionUrl: String? = null,
        buttonText: String? = null
    ) {
        showScheduleUpdateNotification(
            context = context,
            title = title,
            message = message,
            actionUrl = actionUrl,
            actionButtonText = buttonText
        )
    }

    // Schedule All Alarms
    fun rescheduleAll(context: Context) {
        if (!SettingsRepository.isSetupCompleted.value) return
        val master = SettingsRepository.notificationsMaster.value
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // 1. Repeating Quotes Booster
        cancelAlarm(context, alarmManager, REQ_QUOTE, ACTION_QUOTE_REMINDER)
        if (master && SettingsRepository.quoteNotificationsEnabled.value) {
            scheduleRepeatingQuotes(context, alarmManager)
        }

        // 2. Morning Routine Alert (e.g. 08:00)
        cancelAlarm(context, alarmManager, REQ_MORNING, ACTION_MORNING_ROUTINE)
        if (master && SettingsRepository.dailyRoutineAlert.value) {
            scheduleDailyAlarm(context, alarmManager, REQ_MORNING, ACTION_MORNING_ROUTINE, SettingsRepository.dailyRoutineTime.value)
        }

        // 3. Evening Progress Reminder (e.g. 21:30)
        cancelAlarm(context, alarmManager, REQ_EVENING, ACTION_EVENING_PROGRESS)
        if (master && SettingsRepository.eveningProgressAlert.value) {
            scheduleDailyAlarm(context, alarmManager, REQ_EVENING, ACTION_EVENING_PROGRESS, SettingsRepository.eveningProgressTime.value)
        }

        // 4. Target Exam Countdown (e.g. 10:00)
        cancelAlarm(context, alarmManager, REQ_COUNTDOWN, ACTION_COUNTDOWN_REMINDER)
        if (master && SettingsRepository.countdownAlertEnabled.value) {
            scheduleDailyAlarm(context, alarmManager, REQ_COUNTDOWN, ACTION_COUNTDOWN_REMINDER, SettingsRepository.countdownAlertTime.value)
        }

        // 5. Periodic Background Cloud Sync (WorkManager + AlarmManager fallback)
        cancelAlarm(context, alarmManager, REQ_SCHEDULE_SYNC, ACTION_SCHEDULE_SYNC_CHECK)
        scheduleRepeatingSync(context, alarmManager)
        schedulePeriodicWork(context)
    }

    /**
     * Enqueue Android WorkManager periodic background work (every 15 minutes)
     * to poll mentor personal notices, broadcasts, and academic schedule updates even when app is killed.
     */
    fun schedulePeriodicWork(context: Context) {
        try {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()

            val syncWorkRequest = androidx.work.PeriodicWorkRequestBuilder<com.marjuk.eaptracker.service.ScheduleSyncWorker>(
                15, java.util.concurrent.TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "EAPScheduleSyncWorker",
                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                syncWorkRequest
            )
        } catch (_: Exception) {}
    }

    fun scheduleRepeatingSync(context: Context, alarmManager: AlarmManager) {
        val intent = Intent(context, QuoteAlarmReceiver::class.java).apply {
            action = ACTION_SCHEDULE_SYNC_CHECK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQ_SCHEDULE_SYNC,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        val intervalMillis = 20 * 60 * 1000L // 20 minutes active background cycle
        val triggerAtMillis = System.currentTimeMillis() + intervalMillis
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: Exception) {}
    }

    private fun scheduleRepeatingQuotes(context: Context, alarmManager: AlarmManager) {
        if (!SettingsRepository.notificationsMaster.value || !SettingsRepository.quoteNotificationsEnabled.value) return

        val intent = Intent(context, QuoteAlarmReceiver::class.java).apply {
            action = ACTION_QUOTE_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQ_QUOTE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val intervalHours = SettingsRepository.quoteIntervalHours.intValue.coerceIn(1, 12)
        val intervalMillis = intervalHours * 60 * 60 * 1000L
        val triggerAtMillis = System.currentTimeMillis() + intervalMillis

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: Exception) {
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } catch (_: Exception) {}
        }
    }

    private fun scheduleDailyAlarm(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int,
        actionName: String,
        timeStr: String
    ) {
        val intent = Intent(context, QuoteAlarmReceiver::class.java).apply {
            action = actionName
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val parts = timeStr.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val showIntent = Intent(context, com.marjuk.eaptracker.MainActivity::class.java)
                val showPendingIntent = PendingIntent.getActivity(
                    context,
                    requestCode + 5000,
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                )
                val alarmClockInfo = AlarmManager.AlarmClockInfo(calendar.timeInMillis, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (_: Exception) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (_: Exception) {}
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 KB"
        val kb = bytes / 1024.0
        return if (kb < 1024.0) {
            String.format(java.util.Locale.US, "%.1f KB", kb)
        } else {
            String.format(java.util.Locale.US, "%.2f MB", kb / 1024.0)
        }
    }

    fun showBackupProgressNotification(
        context: Context,
        isRestore: Boolean,
        currentBytes: Long,
        totalBytes: Long,
        statusText: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val title = if (isRestore) "Restoring Study Data" else "Backing Up Study Data"
        val percent = if (totalBytes > 0L) ((currentBytes.toFloat() / totalBytes) * 100).toInt().coerceIn(0, 100) else 0
        val isIndeterminate = totalBytes <= 0L || currentBytes == 0L

        val subText = if (totalBytes > 0L) {
            "${formatBytes(currentBytes)} / ${formatBytes(totalBytes)} ($percent%)"
        } else {
            statusText
        }

        val openAppIntent = getNavPendingIntent(context, "profile", 3004)

        val notif = NotificationCompat.Builder(context, CHANNEL_BACKUP_PROGRESS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(subText)
            .setSubText("Google Drive")
            .setProgress(100, percent, isIndeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(NOTIF_BACKUP_PROGRESS_ID, notif)
    }

    fun showBackupCompleteNotification(
        context: Context,
        isRestore: Boolean,
        isSuccess: Boolean,
        sizeBytes: Long,
        message: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        notificationManager.cancel(NOTIF_BACKUP_PROGRESS_ID)

        val title = if (isRestore) {
            if (isSuccess) "Restore Complete" else "Restore Failed"
        } else {
            if (isSuccess) "Backup Complete" else "Backup Failed"
        }

        val bodyText = if (isSuccess) {
            if (sizeBytes > 0L) "$message (${formatBytes(sizeBytes)})" else message
        } else {
            message
        }

        val openAppIntent = getNavPendingIntent(context, "profile", 3005)

        val notif = NotificationCompat.Builder(context, CHANNEL_BACKUP_COMPLETE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setSubText("Google Drive")
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(openAppIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIF_BACKUP_COMPLETE_ID, notif)
    }

    fun dismissBackupProgressNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        notificationManager.cancel(NOTIF_BACKUP_PROGRESS_ID)
    }

    private fun cancelAlarm(context: Context, alarmManager: AlarmManager, requestCode: Int, actionName: String) {
        val intent = Intent(context, QuoteAlarmReceiver::class.java).apply {
            action = actionName
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        alarmManager.cancel(pendingIntent)
    }
}

class QuoteAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "EAPTracker:QuoteAlarmReceiverWakeLock"
        )
        wakeLock?.acquire(30_000L) // 30 seconds max

        try {
            NotificationHelper.init(context)
            SettingsRepository.init(context)
            ProfileRepository.init(context)
            ExamMarksRepository.init(context)
            SyllabusRepository.init(context)

            if (!SettingsRepository.isSetupCompleted.value) return

            when (intent?.action) {
                Intent.ACTION_BOOT_COMPLETED,
                "android.intent.action.MY_PACKAGE_REPLACED",
                "android.intent.action.TIME_SET",
                "android.intent.action.TIMEZONE_CHANGED" -> {
                    NotificationHelper.rescheduleAll(context)
                    try {
                        val oneTimeWork = androidx.work.OneTimeWorkRequestBuilder<com.marjuk.eaptracker.service.ScheduleSyncWorker>()
                            .build()
                        androidx.work.WorkManager.getInstance(context).enqueue(oneTimeWork)
                    } catch (_: Exception) {}
                }
                Intent.ACTION_USER_PRESENT,
                Intent.ACTION_POWER_CONNECTED,
                "android.net.conn.CONNECTIVITY_CHANGE" -> {
                    // Instantly sync latest notifications on device unlock, network reconnect, or charger connect
                    try {
                        val oneTimeWork = androidx.work.OneTimeWorkRequestBuilder<com.marjuk.eaptracker.service.ScheduleSyncWorker>()
                            .build()
                        androidx.work.WorkManager.getInstance(context).enqueue(oneTimeWork)
                    } catch (_: Exception) {}
                }
                NotificationHelper.ACTION_NEXT_QUOTE -> {
                    val newQuote = QuotesRepository.getRandomQuote()
                    NotificationHelper.showQuoteNotification(context, newQuote)
                    HapticHelper.performHaptic(context, HapticType.SELECTION)
                    Toast.makeText(context, "New quote loaded", Toast.LENGTH_SHORT).show()
                }
                NotificationHelper.ACTION_COPY_QUOTE -> {
                    val textToCopy = intent.getStringExtra("quote_text") ?: QuotesRepository.currentQuote.value.quote
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = ClipData.newPlainText("EAP Quote", textToCopy)
                    clipboard?.setPrimaryClip(clip)
                    HapticHelper.performHaptic(context, HapticType.SUCCESS)
                    Toast.makeText(context, "Quote copied to clipboard", Toast.LENGTH_SHORT).show()
                }
                NotificationHelper.ACTION_QUOTE_REMINDER -> {
                    val quote = QuotesRepository.getRandomQuote()
                    NotificationHelper.showQuoteNotification(context, quote)
                    NotificationHelper.rescheduleAll(context)
                }
                NotificationHelper.ACTION_MORNING_ROUTINE -> {
                    NotificationHelper.showMorningRoutineNotification(context)
                    NotificationHelper.rescheduleAll(context)
                }
                NotificationHelper.ACTION_EVENING_PROGRESS -> {
                    NotificationHelper.showEveningProgressNotification(context)
                    NotificationHelper.rescheduleAll(context)
                }
                NotificationHelper.ACTION_COUNTDOWN_REMINDER -> {
                    NotificationHelper.showCountdownNotification(context)
                    NotificationHelper.rescheduleAll(context)
                }
                NotificationHelper.ACTION_SCHEDULE_SYNC_CHECK -> {
                    com.marjuk.eaptracker.ui.admin.RemoteSyncManager.init(context)
                    // Trigger immediate one-time background worker for deep sync
                    try {
                        val oneTimeWork = androidx.work.OneTimeWorkRequestBuilder<com.marjuk.eaptracker.service.ScheduleSyncWorker>()
                            .build()
                        androidx.work.WorkManager.getInstance(context).enqueue(oneTimeWork)
                    } catch (_: Exception) {}

                    // Re-arm AlarmManager for next 20-minute cycle
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                    if (alarmManager != null) {
                        NotificationHelper.scheduleRepeatingSync(context, alarmManager)
                    }
                }
                else -> {
                    val quote = QuotesRepository.getRandomQuote()
                    NotificationHelper.showQuoteNotification(context, quote)
                }
            }
        } finally {
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock.release()
                }
            } catch (_: Exception) {}
        }
    }
}
