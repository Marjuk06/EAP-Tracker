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
import com.marjuk.eaptracker.ui.ExamMarksRepository
import com.marjuk.eaptracker.ui.NavigationItem
import com.marjuk.eaptracker.ui.SyllabusRepository

/**
 * SyllabusProgressWidgetProvider
 * 4x2 Home Screen Widget showing syllabus chapter completion, average exam marks, and quick navigation.
 */
class SyllabusProgressWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_PROGRESS_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, SyllabusProgressWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    companion object {
        const val ACTION_UPDATE_PROGRESS_WIDGET = "com.marjuk.eaptracker.widget.ACTION_UPDATE_PROGRESS"

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, SyllabusProgressWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_PROGRESS_WIDGET
            }
            context.sendBroadcast(intent)
        }

        private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            SyllabusRepository.init(context)
            ExamMarksRepository.init(context)

            val (completedChapters, totalChapters) = SyllabusRepository.getTotalChaptersStats()
            val (phyDone, phyTotal) = SyllabusRepository.getSubjectProgress("Physics")
            val (chemDone, chemTotal) = SyllabusRepository.getSubjectProgress("Chemistry")
            val (mathDone, mathTotal) = SyllabusRepository.getSubjectProgress("Math")

            val overallPct = if (totalChapters > 0) ((completedChapters.toFloat() / totalChapters) * 100).toInt() else 0

            // Exam Performance Stats
            val scores = ExamMarksRepository.scoresState.values.filter { it.totalMax > 0 }
            val examCount = scores.size
            val avgMarksPct = if (examCount > 0) {
                val sumPct = scores.sumOf { it.percentage.toDouble() }
                (sumPct / examCount).toInt()
            } else 0

            val subjectsSummary = "Phy: $phyDone/$phyTotal • Chem: $chemDone/$chemTotal • Math: $mathDone/$mathTotal"
            val examSummary = if (examCount > 0) {
                "Avg Exam Score: $avgMarksPct% • $examCount Exams Taken"
            } else {
                "Exam Performance: No Exam Marks Logged"
            }

            // Navigation Intents
            val openSyllabusIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", NavigationItem.Syllabus.route)
            }
            val openSyllabusPendingIntent = PendingIntent.getActivity(
                context,
                5201,
                openSyllabusIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val openExamsIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", NavigationItem.Exams.route)
            }
            val openExamsPendingIntent = PendingIntent.getActivity(
                context,
                5202,
                openExamsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val openProgressIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", NavigationItem.Progress.route)
            }
            val openProgressPendingIntent = PendingIntent.getActivity(
                context,
                5203,
                openProgressIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val views = RemoteViews(context.packageName, R.layout.widget_syllabus_progress).apply {
                setTextViewText(R.id.widget_progress_overall, "$overallPct% Done")
                setTextViewText(R.id.widget_progress_subjects, subjectsSummary)
                setTextViewText(R.id.widget_progress_exams, examSummary)

                setOnClickPendingIntent(R.id.widget_btn_open_syllabus, openSyllabusPendingIntent)
                setOnClickPendingIntent(R.id.widget_btn_open_exams, openExamsPendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
