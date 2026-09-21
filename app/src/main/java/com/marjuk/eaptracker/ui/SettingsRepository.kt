package com.marjuk.eaptracker.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

object SettingsRepository {
    private const val PREFS_NAME = "eap_settings_prefs"
    private var prefs: SharedPreferences? = null
    private var isInitialized = false

    // Notifications Settings
    val notificationsMaster = mutableStateOf(true)
    val quoteNotificationsEnabled = mutableStateOf(true)
    val quoteIntervalHours = mutableIntStateOf(3)
    val sleepStartHour = mutableIntStateOf(23) // 11:00 PM
    val sleepEndHour = mutableIntStateOf(7)    // 07:00 AM
    val dailyRoutineAlert = mutableStateOf(true)
    val dailyRoutineTime = mutableStateOf("08:00")
    val eveningProgressAlert = mutableStateOf(true)
    val eveningProgressTime = mutableStateOf("21:30")
    val countdownAlertEnabled = mutableStateOf(true)
    val countdownAlertTime = mutableStateOf("10:00")

    // Haptics Settings
    val hapticEnabled = mutableStateOf(true)
    val hapticIntensity = mutableStateOf("Medium") // "Light", "Medium", "Strong"

    // Routine & Study Settings
    val defaultRoutineMode = mutableStateOf("Offline") // "Offline", "Online"
    val autoScrollToToday = mutableStateOf(true)
    val alwaysShowTopics = mutableStateOf(false)

    // Performance Target & Display Settings
    val benchmarkPassPercentage = mutableIntStateOf(85)
    val keepScreenOn = mutableStateOf(false)
    val glassBlurEnabled = mutableStateOf(true)

    // Setup Wizard Completion Flag
    val isSetupCompleted = mutableStateOf(true) // Default to true if already configured, loaded from prefs

    fun init(context: Context) {
        if (isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadSettings()
        isInitialized = true
    }

    private fun loadSettings() {
        val p = prefs ?: return
        isSetupCompleted.value = p.getBoolean("is_setup_completed", false)
        notificationsMaster.value = p.getBoolean("notif_master", true)
        quoteNotificationsEnabled.value = p.getBoolean("quote_notif_enabled", true)
        quoteIntervalHours.intValue = p.getInt("quote_interval_hours", 3)
        sleepStartHour.intValue = p.getInt("sleep_start_hour", 23)
        sleepEndHour.intValue = p.getInt("sleep_end_hour", 7)
        dailyRoutineAlert.value = p.getBoolean("daily_routine_alert", true)
        dailyRoutineTime.value = p.getString("daily_routine_time", "08:00") ?: "08:00"
        eveningProgressAlert.value = p.getBoolean("evening_progress_alert", true)
        eveningProgressTime.value = p.getString("evening_progress_time", "21:30") ?: "21:30"
        countdownAlertEnabled.value = p.getBoolean("countdown_alert_enabled", true)
        countdownAlertTime.value = p.getString("countdown_alert_time", "10:00") ?: "10:00"

        hapticEnabled.value = p.getBoolean("haptic_enabled", true)
        hapticIntensity.value = p.getString("haptic_intensity", "Medium") ?: "Medium"

        defaultRoutineMode.value = p.getString("default_routine_mode", "Offline") ?: "Offline"
        autoScrollToToday.value = p.getBoolean("auto_scroll_today", true)
        alwaysShowTopics.value = p.getBoolean("always_show_topics", false)

        benchmarkPassPercentage.intValue = p.getInt("benchmark_pass_pct", 85)
        keepScreenOn.value = p.getBoolean("keep_screen_on", false)
        glassBlurEnabled.value = p.getBoolean("glass_blur_enabled", true)
    }

    fun setSetupCompleted(completed: Boolean) {
        isSetupCompleted.value = completed
        prefs?.edit()?.putBoolean("is_setup_completed", completed)?.apply()
    }

    fun setNotificationMaster(enabled: Boolean, context: Context? = null) {
        notificationsMaster.value = enabled
        prefs?.edit()?.putBoolean("notif_master", enabled)?.apply()
        if (context != null) {
            NotificationHelper.rescheduleAll(context)
        }
    }

    fun setQuoteNotifications(enabled: Boolean, context: Context? = null) {
        quoteNotificationsEnabled.value = enabled
        prefs?.edit()?.putBoolean("quote_notif_enabled", enabled)?.apply()
        if (context != null) {
            NotificationHelper.rescheduleAll(context)
        }
    }

    fun setQuoteInterval(hours: Int, context: Context? = null) {
        quoteIntervalHours.intValue = hours
        prefs?.edit()?.putInt("quote_interval_hours", hours)?.apply()
        if (context != null) {
            NotificationHelper.rescheduleAll(context)
        }
    }

    fun setSleepWindow(startHour: Int, endHour: Int) {
        sleepStartHour.intValue = startHour
        sleepEndHour.intValue = endHour
        prefs?.edit()
            ?.putInt("sleep_start_hour", startHour)
            ?.putInt("sleep_end_hour", endHour)
            ?.apply()
    }

    fun setDailyRoutineAlert(enabled: Boolean, time: String, context: Context? = null) {
        dailyRoutineAlert.value = enabled
        dailyRoutineTime.value = time
        prefs?.edit()
            ?.putBoolean("daily_routine_alert", enabled)
            ?.putString("daily_routine_time", time)
            ?.apply()
        if (context != null) {
            NotificationHelper.rescheduleAll(context)
        }
    }

    fun setEveningProgressAlert(enabled: Boolean, time: String, context: Context? = null) {
        eveningProgressAlert.value = enabled
        eveningProgressTime.value = time
        prefs?.edit()
            ?.putBoolean("evening_progress_alert", enabled)
            ?.putString("evening_progress_time", time)
            ?.apply()
        if (context != null) {
            NotificationHelper.rescheduleAll(context)
        }
    }

    fun setCountdownAlert(enabled: Boolean, time: String, context: Context? = null) {
        countdownAlertEnabled.value = enabled
        countdownAlertTime.value = time
        prefs?.edit()
            ?.putBoolean("countdown_alert_enabled", enabled)
            ?.putString("countdown_alert_time", time)
            ?.apply()
        if (context != null) {
            NotificationHelper.rescheduleAll(context)
        }
    }

    fun setHapticEnabled(enabled: Boolean) {
        hapticEnabled.value = enabled
        prefs?.edit()?.putBoolean("haptic_enabled", enabled)?.apply()
    }

    fun setHapticIntensity(intensity: String) {
        hapticIntensity.value = intensity
        prefs?.edit()?.putString("haptic_intensity", intensity)?.apply()
    }

    fun setDefaultRoutineMode(mode: String) {
        defaultRoutineMode.value = mode
        prefs?.edit()?.putString("default_routine_mode", mode)?.apply()
    }

    fun setAutoScrollToToday(enabled: Boolean) {
        autoScrollToToday.value = enabled
        prefs?.edit()?.putBoolean("auto_scroll_today", enabled)?.apply()
    }

    fun setAlwaysShowTopics(enabled: Boolean) {
        alwaysShowTopics.value = enabled
        prefs?.edit()?.putBoolean("always_show_topics", enabled)?.apply()
    }

    fun setBenchmarkPassPercentage(pct: Int) {
        benchmarkPassPercentage.intValue = pct
        prefs?.edit()?.putInt("benchmark_pass_pct", pct)?.apply()
    }

    fun setKeepScreenOn(enabled: Boolean) {
        keepScreenOn.value = enabled
        prefs?.edit()?.putBoolean("keep_screen_on", enabled)?.apply()
    }

    fun setGlassBlurEnabled(enabled: Boolean) {
        glassBlurEnabled.value = enabled
        prefs?.edit()?.putBoolean("glass_blur_enabled", enabled)?.apply()
    }
}
