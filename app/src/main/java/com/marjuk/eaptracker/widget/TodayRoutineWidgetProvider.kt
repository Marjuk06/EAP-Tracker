package com.marjuk.eaptracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.marjuk.eaptracker.MainActivity
import com.marjuk.eaptracker.R
import com.marjuk.eaptracker.ui.SettingsRepository
import com.marjuk.eaptracker.ui.admin.DynamicRoutineRepository
import com.marjuk.eaptracker.ui.offlineRoutine
import com.marjuk.eaptracker.ui.onlineRoutine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * TodayRoutineWidgetProvider
 * 4x2 Home Screen Widget with Next / Prev Day Routine browser and direct shortcuts.
 */
class TodayRoutineWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        when (intent.action) {
            ACTION_NEXT_DAY -> {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val prefs = context.getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE)
                    val currentOffset = prefs.getInt("offset_$appWidgetId", 0)
                    prefs.edit().putInt("offset_$appWidgetId", currentOffset + 1).apply()
                    updateWidget(context, appWidgetManager, appWidgetId)
                }
            }
            ACTION_PREV_DAY -> {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val prefs = context.getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE)
                    val currentOffset = prefs.getInt("offset_$appWidgetId", 0)
                    val newOffset = (currentOffset - 1).coerceAtLeast(0)
                    prefs.edit().putInt("offset_$appWidgetId", newOffset).apply()
                    updateWidget(context, appWidgetManager, appWidgetId)
                }
            }
            ACTION_UPDATE_ROUTINE_WIDGET -> {
                val thisWidget = ComponentName(context, TodayRoutineWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    companion object {
        const val PREFS_WIDGET = "prefs_eap_routine_widget"
        const val ACTION_UPDATE_ROUTINE_WIDGET = "com.marjuk.eaptracker.widget.ACTION_UPDATE_ROUTINE"
        const val ACTION_NEXT_DAY = "com.marjuk.eaptracker.widget.ACTION_NEXT_DAY"
        const val ACTION_PREV_DAY = "com.marjuk.eaptracker.widget.ACTION_PREV_DAY"

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, TodayRoutineWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_ROUTINE_WIDGET
            }
            context.sendBroadcast(intent)
        }

        private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            SettingsRepository.init(context)
            DynamicRoutineRepository.init(context)

            val mode = SettingsRepository.defaultRoutineMode.value
            val routineList = DynamicRoutineRepository.getRoutines(mode).ifEmpty {
                if (mode.equals("Online", ignoreCase = true)) onlineRoutine else offlineRoutine
            }

            val prefs = context.getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE)
            val dayOffset = prefs.getInt("offset_$appWidgetId", 0)

            val targetCalendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            val dayOfWeek = targetCalendar.get(Calendar.DAY_OF_WEEK)
            val dayName = when (dayOfWeek) {
                Calendar.SATURDAY -> "Sat"
                Calendar.SUNDAY -> "Sun"
                Calendar.MONDAY -> "Mon"
                Calendar.TUESDAY -> "Tue"
                Calendar.WEDNESDAY -> "Wed"
                Calendar.THURSDAY -> "Thu"
                Calendar.FRIDAY -> "Fri"
                else -> "Day"
            }

            val sdfStandard = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            val sdfShort = SimpleDateFormat("dd-MMM-yy", Locale.ENGLISH)
            val sdfDayMonth = SimpleDateFormat("dd MMM", Locale.ENGLISH)
            val targetDate = targetCalendar.time

            val dateStrStandard = sdfStandard.format(targetDate)
            val dateStrShort = sdfShort.format(targetDate)
            val dateStrDayMonth = sdfDayMonth.format(targetDate)

            val routineItem = routineList.find {
                it.date.equals(dateStrStandard, ignoreCase = true) ||
                it.date.equals(dateStrShort, ignoreCase = true) ||
                it.date.contains(dateStrDayMonth, ignoreCase = true)
            } ?: routineList.find { it.day.startsWith(dayName, ignoreCase = true) }
              ?: (if (routineList.isNotEmpty()) routineList[dayOffset % routineList.size] else null)

            val subject = routineItem?.classSubject ?: "Self Study & Concept Practice"
            val isOrientation = subject.contains("Orientation", ignoreCase = true)
            val exam = when (val details = routineItem?.examDetails) {
                is String -> details
                is List<*> -> details.filterIsInstance<String>().joinToString(" • ")
                else -> if (isOrientation) "No Exam (Orientation Day)" else "Daily Preparation Test"
            }
            val topics = routineItem?.topics?.joinToString(", ") ?: "Theory Revision & QB Practice"

            val displayBadge = when (dayOffset) {
                0 -> "Today • $dateStrDayMonth"
                1 -> "Tomorrow • $dateStrDayMonth"
                else -> "+${dayOffset}d • $dateStrDayMonth"
            }

            // Intents for Next / Prev Navigation
            val nextIntent = Intent(context, TodayRoutineWidgetProvider::class.java).apply {
                action = ACTION_NEXT_DAY
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val nextPendingIntent = PendingIntent.getBroadcast(
                context,
                5010 + appWidgetId,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_MUTABLE else 0)
            )

            val prevIntent = Intent(context, TodayRoutineWidgetProvider::class.java).apply {
                action = ACTION_PREV_DAY
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val prevPendingIntent = PendingIntent.getBroadcast(
                context,
                5020 + appWidgetId,
                prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_MUTABLE else 0)
            )

            // Intents for Open App / Routine
            val openRoutineIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "full_routine/${mode.lowercase()}")
            }
            val openRoutinePendingIntent = PendingIntent.getActivity(
                context,
                5001,
                openRoutineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                5002,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val views = RemoteViews(context.packageName, R.layout.widget_today_routine).apply {
                setTextViewText(R.id.widget_routine_date, displayBadge)
                setTextViewText(R.id.widget_routine_subject, "Class: $subject")
                setTextViewText(R.id.widget_routine_topics, "Topics: $topics")
                setTextViewText(R.id.widget_routine_exam, "Exam: $exam")

                setOnClickPendingIntent(R.id.widget_btn_next_day, nextPendingIntent)
                setOnClickPendingIntent(R.id.widget_btn_prev_day, prevPendingIntent)
                setOnClickPendingIntent(R.id.widget_btn_open_routine, openRoutinePendingIntent)
                setOnClickPendingIntent(R.id.widget_btn_open_app, openAppPendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
