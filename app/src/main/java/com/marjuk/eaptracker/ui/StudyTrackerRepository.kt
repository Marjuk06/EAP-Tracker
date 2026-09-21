package com.marjuk.eaptracker.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DayStudyStat(
    val dayName: String,
    val dateStr: String,
    val studyHours: Float,
    val goalHours: Float,
    val isToday: Boolean
)

data class CalendarDayItem(
    val dayNumber: Int,
    val month: Int,
    val year: Int,
    val isCurrentMonth: Boolean,
    val studyMinutes: Int,
    val isToday: Boolean
) {
    val studyHours: Float get() = studyMinutes / 60f
    val formattedDuration: String get() {
        if (studyMinutes <= 0) return ""
        val h = studyMinutes / 60
        val m = studyMinutes % 60
        return if (h > 0 && m > 0) "${h}:${"%02d".format(m)}"
        else if (h > 0) "${h}:00"
        else "0:${"%02d".format(m)}"
    }
    val heatmapLevel: Int get() {
        val h = studyHours
        return when {
            h <= 0f -> 0
            h < 4f -> 1
            h < 7f -> 2
            h < 10f -> 3
            h < 12f -> 4
            else -> 5
        }
    }
}

data class MonthSummaryStat(
    val monthIndex: Int, // 0..11
    val monthName: String, // "Jan", "Feb"
    val totalMinutes: Int,
    val totalHours: Float,
    val dailyAvgHours: Float,
    val daysLogged: Int
)

object StudyTrackerRepository {
    private var prefs: SharedPreferences? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null

    private var appContext: Context? = null

    // Reactive States
    val todayStudyMinutes = mutableIntStateOf(0)
    val isTimerRunning = mutableStateOf(false)
    val sessionElapsedSeconds = mutableIntStateOf(0)
    val activeSubject = mutableStateOf("Self Study & Problem Solving")

    fun init(context: Context) {
        appContext = context.applicationContext
        if (prefs == null) {
            prefs = context.getSharedPreferences("eap_study_tracker_prefs", Context.MODE_PRIVATE)
            cleanupOldSampleDataIfPresent()
            loadTodayStudyMinutes()
        }
    }

    private fun cleanupOldSampleDataIfPresent() {
        val p = prefs ?: return
        if (p.getBoolean("has_seeded_sample_stats", false)) {
            val editor = p.edit()
            val allKeys = p.all.keys
            for (k in allKeys) {
                if (k.startsWith("study_mins_") && k != "study_mins_${getTodayDateKey()}") {
                    editor.remove(k)
                }
            }
            editor.remove("has_seeded_sample_stats")
            editor.apply()
        }
    }

    private fun getTodayDateKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun makeDateKey(year: Int, month: Int, day: Int): String {
        return "%04d-%02d-%02d".format(year, month, day)
    }

    fun loadTodayStudyMinutes() {
        val key = "study_mins_${getTodayDateKey()}"
        todayStudyMinutes.intValue = prefs?.getInt(key, 0) ?: 0
    }

    fun getStudyMinutesForDate(year: Int, month: Int, day: Int): Int {
        val key = "study_mins_${makeDateKey(year, month, day)}"
        return prefs?.getInt(key, 0) ?: 0
    }

    fun saveStudyMinutesForDate(year: Int, month: Int, day: Int, minutes: Int) {
        val key = "study_mins_${makeDateKey(year, month, day)}"
        prefs?.edit()?.putInt(key, minutes.coerceAtLeast(0))?.apply()
        if (makeDateKey(year, month, day) == getTodayDateKey()) {
            todayStudyMinutes.intValue = minutes.coerceAtLeast(0)
        }
    }

    fun addStudyMinutes(minutes: Int) {
        val current = todayStudyMinutes.intValue
        val updated = (current + minutes).coerceAtLeast(0)
        todayStudyMinutes.intValue = updated
        val key = "study_mins_${getTodayDateKey()}"
        prefs?.edit()?.putInt(key, updated)?.apply()
        appContext?.let { com.marjuk.eaptracker.widget.StudyGoalWidgetProvider.updateAllWidgets(it) }
    }

    fun setTodayStudyMinutes(minutes: Int) {
        val updated = minutes.coerceAtLeast(0)
        todayStudyMinutes.intValue = updated
        val key = "study_mins_${getTodayDateKey()}"
        prefs?.edit()?.putInt(key, updated)?.apply()
        appContext?.let { com.marjuk.eaptracker.widget.StudyGoalWidgetProvider.updateAllWidgets(it) }
    }

    fun startFocusTimer(subject: String = "Self Study & Problem Solving", context: Context? = null) {
        val ctx = context ?: appContext
        activeSubject.value = subject
        if (isTimerRunning.value) return
        isTimerRunning.value = true
        com.marjuk.eaptracker.ui.admin.StudentTelemetryManager.updateLiveStudyStatus(isStudying = true, activeSubject = subject)
        if (ctx != null) {
            com.marjuk.eaptracker.service.FocusTimerForegroundService.startService(ctx, subject, sessionElapsedSeconds.intValue)
        }
        timerJob = scope.launch {
            while (isTimerRunning.value) {
                delay(1000)
                sessionElapsedSeconds.intValue += 1
            }
        }
    }

    fun pauseFocusTimer(context: Context? = null) {
        val ctx = context ?: appContext
        isTimerRunning.value = false
        timerJob?.cancel()
        timerJob = null
        com.marjuk.eaptracker.ui.admin.StudentTelemetryManager.updateLiveStudyStatus(isStudying = false)
        if (ctx != null) {
            com.marjuk.eaptracker.service.FocusTimerForegroundService.stopService(ctx)
        }
    }

    fun logAndSaveSession(context: Context? = null): Int {
        val seconds = sessionElapsedSeconds.intValue
        val minutesToAdd = (seconds / 60)
        if (minutesToAdd > 0) {
            addStudyMinutes(minutesToAdd)
        }
        pauseFocusTimer(context)
        sessionElapsedSeconds.intValue = 0
        com.marjuk.eaptracker.ui.admin.StudentTelemetryManager.syncTelemetryToCloud()
        return minutesToAdd
    }

    fun resetSessionTimer(context: Context? = null) {
        pauseFocusTimer(context)
        sessionElapsedSeconds.intValue = 0
        com.marjuk.eaptracker.ui.admin.StudentTelemetryManager.updateLiveStudyStatus(isStudying = false)
    }

    fun getWeeklyStudyStats(): List<DayStudyStat> {
        val stats = mutableListOf<DayStudyStat>()
        val cal = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val goalHours = ProfileRepository.dailyStudyHoursGoal.intValue.toFloat()

        val todayKey = keyFormat.format(Date())

        for (i in 6 downTo 0) {
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val key = "study_mins_${keyFormat.format(c.time)}"
            val mins = prefs?.getInt(key, 0) ?: 0
            val hours = mins / 60f
            val dayName = dayFormat.format(c.time)
            val dateStr = SimpleDateFormat("dd MMM", Locale.getDefault()).format(c.time)
            val isToday = keyFormat.format(c.time) == todayKey

            stats.add(
                DayStudyStat(
                    dayName = dayName,
                    dateStr = dateStr,
                    studyHours = hours,
                    goalHours = goalHours,
                    isToday = isToday
                )
            )
        }
        return stats
    }

    fun getCalendarMonthGrid(year: Int, month: Int): List<CalendarDayItem> {
        val items = mutableListOf<CalendarDayItem>()
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1) // 0-based month
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, ... 7 = Saturday
        // Convert to Saturday = 0, Sunday = 1, Monday = 2, Tuesday = 3, Wednesday = 4, Thursday = 5, Friday = 6
        val saturdayBasedFirstDay = when (firstDayOfWeek) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }

        val todayCal = Calendar.getInstance()
        val isCurrentYearAndMonth = todayCal.get(Calendar.YEAR) == year && (todayCal.get(Calendar.MONTH) + 1) == month
        val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

        // Previous month padding days
        val prevCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            add(Calendar.MONTH, -1)
        }
        val daysInPrevMonth = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val prevMonthVal = prevCal.get(Calendar.MONTH) + 1
        val prevYearVal = prevCal.get(Calendar.YEAR)

        for (i in 0 until saturdayBasedFirstDay) {
            val d = daysInPrevMonth - saturdayBasedFirstDay + 1 + i
            val mins = getStudyMinutesForDate(prevYearVal, prevMonthVal, d)
            items.add(
                CalendarDayItem(
                    dayNumber = d,
                    month = prevMonthVal,
                    year = prevYearVal,
                    isCurrentMonth = false,
                    studyMinutes = mins,
                    isToday = false
                )
            )
        }

        // Current month days
        for (day in 1..daysInMonth) {
            val mins = getStudyMinutesForDate(year, month, day)
            val isToday = isCurrentYearAndMonth && day == todayDay
            items.add(
                CalendarDayItem(
                    dayNumber = day,
                    month = month,
                    year = year,
                    isCurrentMonth = true,
                    studyMinutes = mins,
                    isToday = isToday
                )
            )
        }

        // Trailing next month padding days to complete full weeks
        val remainder = items.size % 7
        if (remainder != 0) {
            val needed = 7 - remainder
            val nextCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                add(Calendar.MONTH, 1)
            }
            val nextMonthVal = nextCal.get(Calendar.MONTH) + 1
            val nextYearVal = nextCal.get(Calendar.YEAR)

            for (day in 1..needed) {
                val mins = getStudyMinutesForDate(nextYearVal, nextMonthVal, day)
                items.add(
                    CalendarDayItem(
                        dayNumber = day,
                        month = nextMonthVal,
                        year = nextYearVal,
                        isCurrentMonth = false,
                        studyMinutes = mins,
                        isToday = false
                    )
                )
            }
        }

        return items
    }

    fun getYearlyMonthSummaries(year: Int): List<MonthSummaryStat> {
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val result = mutableListOf<MonthSummaryStat>()

        for (m in 1..12) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, m - 1)
            }
            val daysCount = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            var totalMins = 0
            var daysLogged = 0

            for (d in 1..daysCount) {
                val mins = getStudyMinutesForDate(year, m, d)
                if (mins > 0) {
                    totalMins += mins
                    daysLogged++
                }
            }

            val hours = totalMins / 60f
            val avg = if (daysLogged > 0) hours / daysLogged else 0f

            result.add(
                MonthSummaryStat(
                    monthIndex = m - 1,
                    monthName = monthNames[m - 1],
                    totalMinutes = totalMins,
                    totalHours = hours,
                    dailyAvgHours = avg,
                    daysLogged = daysLogged
                )
            )
        }
        return result
    }
}
