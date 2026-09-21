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
import com.marjuk.eaptracker.ui.ProfileRepository
import com.marjuk.eaptracker.ui.StudyTrackerRepository

/**
 * StudyGoalWidgetProvider
 * 2x2 Home Screen Widget showing daily study hours, completion percentage, and 1-click focus launcher.
 */
class StudyGoalWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_GOAL_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, StudyGoalWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    companion object {
        const val ACTION_UPDATE_GOAL_WIDGET = "com.marjuk.eaptracker.widget.ACTION_UPDATE_GOAL"

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, StudyGoalWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_GOAL_WIDGET
            }
            context.sendBroadcast(intent)
        }

        private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            ProfileRepository.init(context)
            StudyTrackerRepository.init(context)

            val todayMins = StudyTrackerRepository.todayStudyMinutes.intValue
            val todayHours = todayMins / 60f
            val goalHours = ProfileRepository.dailyStudyHoursGoal.intValue.coerceAtLeast(1)
            val pct = ((todayHours / goalHours) * 100).toInt().coerceIn(0, 100)

            val hoursText = "${String.format(java.util.Locale.US, "%.1f", todayHours)} / ${goalHours}h"
            val pctText = "$pct%"

            // Intent to open focus timer directly
            val openFocusIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "fullscreen_focus")
            }
            val openFocusPendingIntent = PendingIntent.getActivity(
                context,
                5101,
                openFocusIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                5102,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val views = RemoteViews(context.packageName, R.layout.widget_study_goal).apply {
                setTextViewText(R.id.widget_goal_hours, hoursText)
                setTextViewText(R.id.widget_goal_percent, pctText)
                setTextViewText(R.id.widget_goal_streak, if (pct >= 100) "Goal Achieved Today!" else "Daily Study Goal")

                setOnClickPendingIntent(R.id.widget_btn_start_focus, openFocusPendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
