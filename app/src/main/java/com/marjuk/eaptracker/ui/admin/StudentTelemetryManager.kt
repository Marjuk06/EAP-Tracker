package com.marjuk.eaptracker.ui.admin

import android.content.Context
import android.os.Build
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.marjuk.eaptracker.ui.BackupRepository
import com.marjuk.eaptracker.ui.NotificationHelper
import com.marjuk.eaptracker.ui.NotificationHistoryRepository
import com.marjuk.eaptracker.ui.ProfileRepository
import com.marjuk.eaptracker.ui.ProgressRepository
import com.marjuk.eaptracker.ui.StudyTrackerRepository
import com.marjuk.eaptracker.ui.SyllabusRepository
import kotlinx.coroutines.launch

object StudentTelemetryManager {

    private const val RTDB_URL = "https://eap-tracker-default-rtdb.firebaseio.com"
    private var isListening = false

    fun getStudentKey(): String {
        val roll = ProfileRepository.rollNo.value.trim()
        if (roll.isNotBlank()) {
            return roll.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        }
        val email = BackupRepository.connectedAccountEmail.value?.trim()
        if (!email.isNullOrBlank()) {
            return email.replace(".", "_").replace("@", "_at_")
        }
        return "student_unregistered"
    }

    /**
     * Push current student telemetry snapshot to Firebase Realtime Database.
     * Admin can view all student metrics in real time on the Admin Panel.
     * Strictly gated to only push after setup wizard is completed.
     */
    fun syncTelemetryToCloud(context: Context? = null) {
        if (!com.marjuk.eaptracker.ui.SettingsRepository.isSetupCompleted.value) {
            return
        }
        try {
            val studentKey = getStudentKey()
            if (studentKey == "student_unregistered") return
            val database = FirebaseDatabase.getInstance(RTDB_URL)
            val studentRef = database.getReference("students").child(studentKey)

            val (completedChapters, totalChapters) = SyllabusRepository.getTotalChaptersStats()
            val syllabusPct = if (totalChapters > 0) ((completedChapters.toFloat() / totalChapters) * 100f).toInt() else 0

            val (totalExams, totalObtained, totalMax) = ProgressRepository.getOverallExamStats()
            val examAvgPct = if (totalMax > 0f) ((totalObtained / totalMax) * 100f).toInt() else 0

            val todayMins = StudyTrackerRepository.todayStudyMinutes.intValue
            val todayHours = (todayMins / 60f)

            val photo = ProfileRepository.profilePhotoUri.value ?: ProfileRepository.googlePhotoUrl.value ?: ""

            val telemetryData = mapOf(
                "studentId" to ProfileRepository.rollNo.value.ifBlank { studentKey },
                "name" to ProfileRepository.name.value.ifBlank { "Aspirant" },
                "college" to ProfileRepository.college.value,
                "batch" to ProfileRepository.hscBatch.value,
                "targetInstitution" to ProfileRepository.targetInstitution.value,
                "email" to (BackupRepository.connectedAccountEmail.value ?: ""),
                "photoUrl" to photo,
                "dailyGoalHours" to ProfileRepository.dailyStudyHoursGoal.intValue,
                "todayStudyHours" to todayHours,
                "completedChapters" to completedChapters,
                "totalChapters" to totalChapters,
                "syllabusPct" to syllabusPct,
                "totalExamsLogged" to totalExams,
                "examAveragePct" to examAvgPct,
                "preferredTrack" to ProfileRepository.preferredTrack.value,
                "isStudying" to StudyTrackerRepository.isTimerRunning.value,
                "lastActiveEpoch" to System.currentTimeMillis(),
                "deviceModel" to (Build.MODEL ?: "Android"),
                "appVersion" to "1.0.0"
            )

            studentRef.child("profile_summary").updateChildren(telemetryData)

            // Also push via REST with secret key for guaranteed delivery
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val urlStr = "$RTDB_URL/students/$studentKey/profile_summary.json?auth=yleDrwUbDfga8ueOKchQEZ47XCPlrzlRNiCF1NAw"
                    val url = java.net.URL(urlStr)
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "PATCH"
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    conn.doOutput = true
                    val jsonObj = org.json.JSONObject(telemetryData)
                    conn.outputStream.use { os ->
                        os.write(jsonObj.toString().toByteArray(Charsets.UTF_8))
                    }
                    val code = conn.responseCode
                    conn.disconnect()
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Update live study timer status in real time to the cloud so Admin can see if student is currently studying.
     */
    fun updateLiveStudyStatus(isStudying: Boolean, activeSubject: String = "Focus Session") {
        try {
            val studentKey = getStudentKey()
            val database = FirebaseDatabase.getInstance(RTDB_URL)
            val studentRef = database.getReference("students").child(studentKey)

            val liveData = mapOf(
                "isStudying" to isStudying,
                "activeSubject" to activeSubject,
                "sessionStartEpoch" to if (isStudying) System.currentTimeMillis() else 0L,
                "lastActiveEpoch" to System.currentTimeMillis()
            )

            studentRef.child("profile_summary").updateChildren(liveData)

            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val urlStr = "$RTDB_URL/students/$studentKey/profile_summary.json?auth=yleDrwUbDfga8ueOKchQEZ47XCPlrzlRNiCF1NAw"
                    val url = java.net.URL(urlStr)
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "PATCH"
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    conn.doOutput = true
                    val jsonObj = org.json.JSONObject(liveData)
                    conn.outputStream.use { os ->
                        os.write(jsonObj.toString().toByteArray(Charsets.UTF_8))
                    }
                    val code = conn.responseCode
                    conn.disconnect()
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Start listening for individual customized routine/exams or personal notices targeted to this student.
     */
    fun startIndividualStudentListener(context: Context) {
        if (isListening) return
        isListening = true

        try {
            val studentKey = getStudentKey()
            val database = FirebaseDatabase.getInstance(RTDB_URL)
            val studentRef = database.getReference("students").child(studentKey)

            // Listen for personal direct notices from Admin
            studentRef.child("personal_notice").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val data = snapshot.value as? Map<*, *> ?: return
                    try {
                        val timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L
                        val prefs = context.getSharedPreferences("student_telemetry_prefs", Context.MODE_PRIVATE)
                        val lastSeen = prefs.getLong("last_seen_personal_notice", 0L)

                        if (timestamp > 0L && timestamp != lastSeen) {
                            val title = (data["title"] as? String)?.ifBlank { "Personal Mentor Notice" } ?: "Personal Mentor Notice"
                            val message = data["message"] as? String ?: ""
                            val actionUrl = (data["actionUrl"] ?: data["url"]) as? String
                            val actionButtonText = (data["actionButtonText"] ?: data["buttonText"]) as? String

                            if (message.isNotBlank()) {
                                NotificationHistoryRepository.addNotification(
                                    title = title,
                                    message = message,
                                    category = "Personal",
                                    timestamp = timestamp,
                                    actionUrl = actionUrl,
                                    actionButtonText = actionButtonText
                                )
                                NotificationHelper.showScheduleUpdateNotification(
                                    context = context.applicationContext,
                                    title = title,
                                    message = message,
                                    actionUrl = actionUrl,
                                    actionButtonText = actionButtonText
                                )
                            }
                            prefs.edit().putLong("last_seen_personal_notice", timestamp).apply()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

            // Listen for custom individual routine assigned by Admin
            studentRef.child("custom_routine").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val value = snapshot.value
                    if (value == null) {
                        // Admin reset or removed custom routine -> clear custom mode
                        DynamicRoutineRepository.clearCustomRoutine("Offline")
                        DynamicRoutineRepository.clearCustomRoutine("Online")
                        return
                    }

                    val data = value as? Map<*, *> ?: return
                    try {
                        val updatedEpoch = (data["updatedEpoch"] as? Number)?.toLong() ?: 0L
                        val prefs = context.getSharedPreferences("student_telemetry_prefs", Context.MODE_PRIVATE)
                        val lastSeenRoutine = prefs.getLong("last_seen_custom_routine", 0L)
                        var appliedCount = 0

                        // 1. Explicit offlineRoutine list
                        val offlineList = data["offlineRoutine"] as? List<*>
                        if (offlineList != null && offlineList.isNotEmpty()) {
                            val jsonArray = org.json.JSONArray()
                            offlineList.forEach { item ->
                                if (item is Map<*, *>) jsonArray.put(org.json.JSONObject(item))
                            }
                            val parsed = DynamicRoutineRepository.parseRoutineJson(jsonArray.toString())
                            if (parsed.isNotEmpty()) {
                                DynamicRoutineRepository.setCustomRoutine("Offline", parsed)
                                appliedCount += parsed.size
                            }
                        }

                        // 2. Explicit onlineRoutine list
                        val onlineList = data["onlineRoutine"] as? List<*>
                        if (onlineList != null && onlineList.isNotEmpty()) {
                            val jsonArray = org.json.JSONArray()
                            onlineList.forEach { item ->
                                if (item is Map<*, *>) jsonArray.put(org.json.JSONObject(item))
                            }
                            val parsed = DynamicRoutineRepository.parseRoutineJson(jsonArray.toString())
                            if (parsed.isNotEmpty()) {
                                DynamicRoutineRepository.setCustomRoutine("Online", parsed)
                                appliedCount += parsed.size
                            }
                        }

                        // 3. Fallback generic routineDays list
                        val routineDaysList = data["routineDays"] as? List<*>
                        if (routineDaysList != null && routineDaysList.isNotEmpty() && offlineList == null && onlineList == null) {
                            val jsonArray = org.json.JSONArray()
                            routineDaysList.forEach { item ->
                                if (item is Map<*, *>) jsonArray.put(org.json.JSONObject(item))
                            }
                            val targetMode = (data["mode"] as? String)?.ifBlank { null } ?: "Offline"
                            val parsed = DynamicRoutineRepository.parseRoutineJson(jsonArray.toString())
                            if (parsed.isNotEmpty()) {
                                DynamicRoutineRepository.setCustomRoutine(targetMode, parsed)
                                appliedCount += parsed.size
                            }
                        }

                        if (appliedCount > 0 && updatedEpoch > 0L && updatedEpoch != lastSeenRoutine) {
                            NotificationHelper.showScheduleUpdateNotification(
                                context = context.applicationContext,
                                title = "🌟 Custom Routine Assigned",
                                message = "Your mentor has updated your personalized study schedule with $appliedCount custom days!"
                            )
                            prefs.edit().putLong("last_seen_custom_routine", updatedEpoch).apply()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

            // Listen for custom individual exams assigned by Admin
            studentRef.child("custom_exams").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val value = snapshot.value
                    if (value == null) {
                        // Admin reset or removed custom exams -> clear custom mode
                        DynamicExamRepository.clearCustomExam("Offline")
                        DynamicExamRepository.clearCustomExam("Online")
                        return
                    }

                    val data = value as? Map<*, *> ?: return
                    try {
                        val updatedEpoch = (data["updatedEpoch"] as? Number)?.toLong() ?: 0L
                        val prefs = context.getSharedPreferences("student_telemetry_prefs", Context.MODE_PRIVATE)
                        val lastSeenExams = prefs.getLong("last_seen_custom_exams", 0L)
                        var appliedCount = 0

                        // 1. Explicit offlineExams list
                        val offlineList = data["offlineExams"] as? List<*>
                        if (offlineList != null && offlineList.isNotEmpty()) {
                            val jsonArray = org.json.JSONArray()
                            offlineList.forEach { item ->
                                if (item is Map<*, *>) jsonArray.put(org.json.JSONObject(item))
                            }
                            val parsed = DynamicExamRepository.parseExamJson(jsonArray.toString())
                            if (parsed.isNotEmpty()) {
                                DynamicExamRepository.setCustomExams("Offline", parsed)
                                appliedCount += parsed.size
                            }
                        }

                        // 2. Explicit onlineExams list
                        val onlineList = data["onlineExams"] as? List<*>
                        if (onlineList != null && onlineList.isNotEmpty()) {
                            val jsonArray = org.json.JSONArray()
                            onlineList.forEach { item ->
                                if (item is Map<*, *>) jsonArray.put(org.json.JSONObject(item))
                            }
                            val parsed = DynamicExamRepository.parseExamJson(jsonArray.toString())
                            if (parsed.isNotEmpty()) {
                                DynamicExamRepository.setCustomExams("Online", parsed)
                                appliedCount += parsed.size
                            }
                        }

                        // 3. Fallback generic examList
                        val examList = data["examList"] as? List<*>
                        if (examList != null && examList.isNotEmpty() && offlineList == null && onlineList == null) {
                            val jsonArray = org.json.JSONArray()
                            examList.forEach { item ->
                                if (item is Map<*, *>) jsonArray.put(org.json.JSONObject(item))
                            }
                            val targetMode = (data["mode"] as? String)?.ifBlank { null } ?: "Offline"
                            val parsed = DynamicExamRepository.parseExamJson(jsonArray.toString())
                            if (parsed.isNotEmpty()) {
                                DynamicExamRepository.setCustomExams(targetMode, parsed)
                                appliedCount += parsed.size
                            }
                        }

                        if (appliedCount > 0 && updatedEpoch > 0L && updatedEpoch != lastSeenExams) {
                            NotificationHelper.showScheduleUpdateNotification(
                                context = context.applicationContext,
                                title = "📝 Custom Exam Schedule Assigned",
                                message = "Your mentor has updated your personalized exam schedule with $appliedCount tests!"
                            )
                            prefs.edit().putLong("last_seen_custom_exams", updatedEpoch).apply()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

            // Listen for custom individual syllabus (e.g. Biology disabled, custom subjects added) assigned by Admin
            studentRef.child("custom_syllabus").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val value = snapshot.value
                    if (value == null) {
                        // Reset to global master syllabus
                        SyllabusRepository.resetToDefaultSyllabus()
                        return
                    }

                    val data = value as? Map<*, *> ?: return
                    try {
                        val updatedEpoch = (data["updatedEpoch"] as? Number)?.toLong() ?: 0L
                        val prefs = context.getSharedPreferences("student_telemetry_prefs", Context.MODE_PRIVATE)
                        val lastSeenSyllabus = prefs.getLong("last_seen_custom_syllabus", 0L)

                        val subjectsList = data["subjects"] as? List<*>
                        if (subjectsList != null && subjectsList.isNotEmpty()) {
                            val jsonArray = org.json.JSONArray()
                            subjectsList.forEach { item ->
                                when (item) {
                                    is Map<*, *> -> jsonArray.put(org.json.JSONObject(item))
                                    is String -> jsonArray.put(item)
                                }
                            }
                            val parsed = SyllabusRepository.parseSyllabusJson(jsonArray.toString())
                            if (parsed.isNotEmpty()) {
                                SyllabusRepository.updateAllSubjects(parsed)

                                if (updatedEpoch > 0L && updatedEpoch != lastSeenSyllabus) {
                                    NotificationHelper.showScheduleUpdateNotification(
                                        context = context.applicationContext,
                                        title = "📚 Custom Syllabus Assigned",
                                        message = "Your personalized subject syllabus has been updated by your mentor!"
                                    )
                                    prefs.edit().putLong("last_seen_custom_syllabus", updatedEpoch).apply()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

            // Listen for targeted beta / test app update pushed specifically to this student
            studentRef.child("app_update").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val value = snapshot.value ?: return
                    val data = value as? Map<*, *> ?: return
                    try {
                        val versionCode = (data["versionCode"] as? Number)?.toInt() ?: 0
                        val versionName = (data["versionName"] as? String) ?: ""
                        val apkUrl = (data["apkUrl"] as? String) ?: ""
                        val title = (data["title"] as? String) ?: "🚀 Test Release Available ($versionName)"
                        val changelog = (data["changelog"] as? String) ?: ""
                        val isForceUpdate = (data["isForceUpdate"] as? Boolean) ?: false
                        val currentVersionCode = AppUpdateManager.getCurrentVersionCode(context)

                        if (versionCode > currentVersionCode && apkUrl.isNotBlank()) {
                            val info = AppUpdateInfo(
                                versionCode = versionCode,
                                versionName = versionName,
                                apkUrl = apkUrl,
                                title = title,
                                changelog = changelog,
                                isForceUpdate = isForceUpdate
                            )
                            AppUpdateManager.availableUpdate.value = info
                            AppUpdateManager.isUpdateDialogOpen.value = true

                            NotificationHelper.showScheduleUpdateNotification(
                                context = context.applicationContext,
                                title = title,
                                message = "A test update ($versionName) has been assigned to you. Tap to download!"
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
