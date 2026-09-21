package com.marjuk.eaptracker.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class TargetExam(
    val id: String,
    val examName: String,
    val targetEpoch: Long,
    val institution: String = ""
)

object ProfileRepository {
    private const val PREFS_NAME = "eap_profile_prefs"
    private const val KEY_TARGET_EXAMS = "target_exams_json"
    private var prefs: SharedPreferences? = null
    private var isInitialized = false

    // Reactive State Holders (Empty defaults for new user onboarding)
    val name = mutableStateOf("")
    val targetInstitution = mutableStateOf("")
    val college = mutableStateOf("")
    val hscBatch = mutableStateOf("HSC '25")
    val rollNo = mutableStateOf("")
    val avatar = mutableStateOf("👨‍🎓")
    val profilePhotoUri = mutableStateOf<String?>(null)
    val googlePhotoUrl = mutableStateOf<String?>(null)
    val quote = mutableStateOf("“Discipline and relentless consistency turn engineering dreams into reality.”")
    
    val targetExamName = mutableStateOf("BUET Admission Test")
    val targetExamDateEpoch = mutableLongStateOf(getDefaultTargetDateEpoch())
    val dailyStudyHoursGoal = mutableIntStateOf(8)
    val preferredTrack = mutableStateOf("Offline + Online")

    val targetExamsList = androidx.compose.runtime.mutableStateListOf<TargetExam>()

    // Default Avatar options
    val avatarOptions = listOf("👨‍🎓", "👩‍🎓", "🚀", "⚛️", "💻", "🎯", "🧠", "⚡", "🏆", "🔬", "📐", "🦾")

    fun getInitials(): String {
        val parts = name.value.trim().split("\\s+".toRegex())
        return when {
            parts.size >= 2 -> "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
            parts.size == 1 && parts[0].isNotEmpty() -> parts[0].take(2).uppercase()
            else -> "ST"
        }
    }

    // Preset Target Institutions
    val institutionPresets = listOf(
        "BUET CSE",
        "BUET EEE",
        "BUET ME",
        "BUET Civil",
        "CKRUET (CUET/KUET/RUET)",
        "IUT CSE / SWE",
        "DU A-Unit (CSE/Applied Physics)",
        "MIST CSE",
        "BUTEX",
        "Medical / AFMC"
    )

    private fun getDefaultTargetDateEpoch(): Long {
        return try {
            val target = LocalDateTime.now().plusMonths(3).withHour(10).withMinute(0).withSecond(0)
            target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis() + 90L * 24 * 60 * 60 * 1000
        }
    }

    private fun getInitialTargetExams(): List<TargetExam> {
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60 * 60 * 1000
        return listOf(
            TargetExam("buet_preli", "BUET Preliminary Admission Test", now + 90L * dayMs, "BUET"),
            TargetExam("buet_written", "BUET Final Written Test", now + 111L * dayMs, "BUET"),
            TargetExam("ckruet_test", "CKRUET Combined Admission Test", now + 125L * dayMs, "CKRUET"),
            TargetExam("du_a_unit", "DU A-Unit Admission Exam", now + 146L * dayMs, "Dhaka University"),
            TargetExam("iut_test", "IUT Admission Test", now + 160L * dayMs, "IUT"),
            TargetExam("mist_test", "MIST Admission Test", now + 174L * dayMs, "MIST")
        )
    }

    fun init(context: Context) {
        if (isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadProfile()
        isInitialized = true
    }

    private fun loadProfile() {
        val p = prefs ?: return
        name.value = p.getString("user_name", "") ?: ""
        targetInstitution.value = p.getString("target_institution", "") ?: ""
        college.value = p.getString("college_name", "") ?: ""
        hscBatch.value = p.getString("hsc_batch", "HSC '25") ?: "HSC '25"
        rollNo.value = p.getString("roll_no", "") ?: ""
        avatar.value = p.getString("avatar", "👨‍🎓") ?: "👨‍🎓"
        profilePhotoUri.value = p.getString("profile_photo_uri", null)
        googlePhotoUrl.value = p.getString("google_photo_url", null)
        quote.value = p.getString("quote", "“Discipline and relentless consistency turn engineering dreams into reality.”") 
            ?: "“Discipline and relentless consistency turn engineering dreams into reality.”"
        
        targetExamName.value = p.getString("target_exam_name", "BUET Admission Test") ?: "BUET Admission Test"
        targetExamDateEpoch.longValue = p.getLong("target_exam_date_epoch", getDefaultTargetDateEpoch())
        dailyStudyHoursGoal.intValue = p.getInt("daily_study_hours_goal", 8)
        preferredTrack.value = p.getString("preferred_track", "Offline + Online") ?: "Offline + Online"

        loadTargetExams()
    }

    private fun loadTargetExams() {
        val p = prefs ?: return
        val json = p.getString(KEY_TARGET_EXAMS, null)
        targetExamsList.clear()
        if (!json.isNullOrBlank()) {
            try {
                val arr = org.json.JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    targetExamsList.add(
                        TargetExam(
                            id = obj.optString("id", "exam_$i"),
                            examName = obj.optString("examName", "Target Exam"),
                            targetEpoch = obj.optLong("targetEpoch", System.currentTimeMillis() + 90L * 86400000L),
                            institution = obj.optString("institution", "")
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                targetExamsList.addAll(getInitialTargetExams())
            }
        } else {
            targetExamsList.addAll(getInitialTargetExams())
        }
    }

    fun saveTargetExams() {
        val p = prefs ?: return
        val arr = org.json.JSONArray()
        targetExamsList.forEach { exam ->
            val obj = org.json.JSONObject().apply {
                put("id", exam.id)
                put("examName", exam.examName)
                put("targetEpoch", exam.targetEpoch)
                put("institution", exam.institution)
            }
            arr.put(obj)
        }
        p.edit().putString(KEY_TARGET_EXAMS, arr.toString()).apply()
    }

    fun addTargetExam(examName: String, targetEpoch: Long, institution: String = "") {
        val exam = TargetExam(
            id = "target_${System.currentTimeMillis()}_${(100..999).random()}",
            examName = examName.trim(),
            targetEpoch = targetEpoch,
            institution = institution.trim()
        )
        targetExamsList.add(exam)
        saveTargetExams()
    }

    fun updateTargetExam(id: String, examName: String, targetEpoch: Long, institution: String = "") {
        val index = targetExamsList.indexOfFirst { it.id == id }
        if (index >= 0) {
            targetExamsList[index] = TargetExam(id, examName.trim(), targetEpoch, institution.trim())
            saveTargetExams()
        }
    }

    fun deleteTargetExam(id: String) {
        targetExamsList.removeAll { it.id == id }
        saveTargetExams()
    }

    fun generatePermanentStudentId(batch: String): String {
        val cleanBatch = if (batch.contains("25")) "25" else if (batch.contains("26")) "26" else "26"
        val randomNum = (1000..9999).random()
        return "EAP-$cleanBatch-$randomNum"
    }

    fun ensurePermanentStudentId(batch: String = hscBatch.value): String {
        val current = rollNo.value.trim()
        if (current.isNotBlank() && current != "241001" && current.startsWith("EAP-")) {
            return current
        }
        val newId = generatePermanentStudentId(batch)
        rollNo.value = newId
        prefs?.edit()?.putString("roll_no", newId)?.apply()
        return newId
    }

    fun updateProfile(
        newName: String,
        newTarget: String,
        newCollege: String,
        newBatch: String,
        newRoll: String,
        newAvatar: String,
        newQuote: String
    ) {
        name.value = newName.trim()
        targetInstitution.value = newTarget.trim()
        college.value = newCollege.trim()
        hscBatch.value = newBatch.trim()
        if (rollNo.value.isBlank() || !rollNo.value.startsWith("EAP-")) {
            rollNo.value = ensurePermanentStudentId(newBatch)
        }
        avatar.value = newAvatar.trim()
        quote.value = newQuote.trim()

        prefs?.edit()
            ?.putString("user_name", name.value)
            ?.putString("target_institution", targetInstitution.value)
            ?.putString("college_name", college.value)
            ?.putString("hsc_batch", hscBatch.value)
            ?.putString("roll_no", rollNo.value)
            ?.putString("avatar", avatar.value)
            ?.putString("quote", quote.value)
            ?.apply()

        com.marjuk.eaptracker.ui.admin.StudentTelemetryManager.syncTelemetryToCloud()
    }

    fun updateCountdown(newExamName: String, newDateEpoch: Long) {
        val cleanName = newExamName.trim()
        targetExamName.value = cleanName
        targetExamDateEpoch.longValue = newDateEpoch

        val existingIndex = targetExamsList.indexOfFirst { it.examName.equals(cleanName, ignoreCase = true) }
        if (existingIndex >= 0) {
            val item = targetExamsList[existingIndex]
            targetExamsList[existingIndex] = item.copy(targetEpoch = newDateEpoch)
        } else {
            targetExamsList.add(0, TargetExam(id = "exam_${System.currentTimeMillis()}", examName = cleanName, targetEpoch = newDateEpoch, institution = ""))
        }
        saveTargetExams()

        prefs?.edit()
            ?.putString("target_exam_name", cleanName)
            ?.putLong("target_exam_date_epoch", newDateEpoch)
            ?.apply()

        com.marjuk.eaptracker.ui.admin.StudentTelemetryManager.syncTelemetryToCloud()
    }

    fun resetCountdownToDefault() {
        val defaultEpoch = getDefaultTargetDateEpoch()
        targetExamName.value = "BUET Admission Test"
        targetExamDateEpoch.longValue = defaultEpoch
        prefs?.edit()
            ?.putString("target_exam_name", "BUET Admission Test")
            ?.putLong("target_exam_date_epoch", defaultEpoch)
            ?.apply()
        com.marjuk.eaptracker.ui.admin.StudentTelemetryManager.syncTelemetryToCloud()
    }

    fun updateDailyGoal(hours: Int) {
        dailyStudyHoursGoal.intValue = hours
        prefs?.edit()?.putInt("daily_study_hours_goal", hours)?.apply()
        com.marjuk.eaptracker.ui.admin.StudentTelemetryManager.syncTelemetryToCloud()
    }

    fun updatePreferredTrack(track: String) {
        preferredTrack.value = track
        prefs?.edit()?.putString("preferred_track", track)?.apply()
        com.marjuk.eaptracker.ui.admin.StudentTelemetryManager.syncTelemetryToCloud()
    }

    fun updateGooglePhotoUrl(url: String?) {
        googlePhotoUrl.value = url
        prefs?.edit()?.putString("google_photo_url", url)?.apply()
        if (profilePhotoUri.value.isNullOrBlank() && !url.isNullOrBlank()) {
            profilePhotoUri.value = url
            prefs?.edit()?.putString("profile_photo_uri", url)?.apply()
        }
        com.marjuk.eaptracker.ui.admin.StudentTelemetryManager.syncTelemetryToCloud()
    }

    fun setProfilePhoto(uriString: String?) {
        profilePhotoUri.value = uriString
        if (uriString == null) {
            prefs?.edit()?.remove("profile_photo_uri")?.apply()
        } else {
            prefs?.edit()?.putString("profile_photo_uri", uriString)?.apply()
        }
        com.marjuk.eaptracker.ui.admin.StudentTelemetryManager.syncTelemetryToCloud()
    }

    fun saveCroppedAvatar(context: Context, bitmap: android.graphics.Bitmap): String? {
        return try {
            val file = java.io.File(context.applicationContext.filesDir, "profile_avatar.jpg")
            file.outputStream().use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, out)
            }
            val uriStr = file.absolutePath
            setProfilePhoto(uriStr)
            uriStr
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun grabGoogleProfilePhoto(context: Context, highRes: Boolean = true): String? {
        return try {
            val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
            val photoUri = account?.photoUrl ?: return null
            val raw = photoUri.toString()
            val finalUrl = if (highRes) {
                raw.replace("/s96-c/", "/s400-c/").replace("=s96-c", "=s400-c")
            } else {
                raw
            }
            updateGooglePhotoUrl(finalUrl)
            setProfilePhoto(finalUrl)
            finalUrl
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveCustomAvatarFromUri(context: Context, sourceUri: android.net.Uri): String? {
        return try {
            val file = java.io.File(context.applicationContext.filesDir, "profile_avatar.jpg")
            context.applicationContext.contentResolver.openInputStream(sourceUri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            val uriStr = file.absolutePath
            setProfilePhoto(uriStr)
            uriStr
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Exports all user profile, syllabus checkmarks, and exam marks to a single JSON string.
     */
    fun exportAllDataAsJson(context: Context): String {
        val root = JSONObject()

        // 1. Profile Data
        val profileJson = JSONObject().apply {
            put("name", name.value)
            put("targetInstitution", targetInstitution.value)
            put("college", college.value)
            put("hscBatch", hscBatch.value)
            put("rollNo", rollNo.value)
            put("avatar", avatar.value)
            put("quote", quote.value)
            put("targetExamName", targetExamName.value)
            put("targetExamDateEpoch", targetExamDateEpoch.longValue)
            put("dailyStudyHoursGoal", dailyStudyHoursGoal.intValue)
            put("preferredTrack", preferredTrack.value)
            put("currentStreak", ProgressRepository.currentStreak.value)
            put("longestStreak", ProgressRepository.longestStreak.value)
        }
        root.put("profile", profileJson)

        // 2. Exam Scores
        val examPrefs = context.applicationContext.getSharedPreferences("eap_exam_marks_prefs", Context.MODE_PRIVATE)
        val examsJson = JSONObject()
        examPrefs.all.forEach { (k, v) ->
            if (v != null) {
                examsJson.put(k, v.toString())
            }
        }
        root.put("examScores", examsJson)

        // 3. Syllabus Checked Sections
        val syllabusPrefs = context.applicationContext.getSharedPreferences("eap_syllabus_prefs", Context.MODE_PRIVATE)
        val syllabusJson = JSONObject()
        syllabusPrefs.all.forEach { (k, v) ->
            if (v is Boolean) {
                syllabusJson.put(k, v)
            }
        }
        root.put("syllabus", syllabusJson)

        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        return root.toString(2)
    }

    /**
     * Restores application state from a valid JSON backup string.
     */
    fun importAllDataFromJson(context: Context, jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)

            // 1. Restore Profile
            if (root.has("profile")) {
                val p = root.getJSONObject("profile")
                val importedName = p.optString("name", name.value)
                val importedTarget = p.optString("targetInstitution", targetInstitution.value)
                val importedCollege = p.optString("college", college.value)
                val importedBatch = p.optString("hscBatch", hscBatch.value)
                val importedRoll = p.optString("rollNo", rollNo.value)
                val importedAvatar = p.optString("avatar", avatar.value)
                val importedQuote = p.optString("quote", quote.value)

                updateProfile(
                    importedName,
                    importedTarget,
                    importedCollege,
                    importedBatch,
                    importedRoll,
                    importedAvatar,
                    importedQuote
                )

                val importedExamName = p.optString("targetExamName", targetExamName.value)
                val importedExamDate = p.optLong("targetExamDateEpoch", targetExamDateEpoch.longValue)
                updateCountdown(importedExamName, importedExamDate)

                val importedGoal = p.optInt("dailyStudyHoursGoal", dailyStudyHoursGoal.intValue)
                updateDailyGoal(importedGoal)

                val importedTrack = p.optString("preferredTrack", preferredTrack.value)
                updatePreferredTrack(importedTrack)
            }

            // 2. Restore Exams
            if (root.has("examScores")) {
                val examPrefs = context.applicationContext.getSharedPreferences("eap_exam_marks_prefs", Context.MODE_PRIVATE)
                val editor = examPrefs.edit().clear()
                val examsJson = root.getJSONObject("examScores")
                val keys = examsJson.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    editor.putString(k, examsJson.getString(k))
                }
                editor.apply()

                // Reload runtime state
                ExamMarksRepository.scoresState.clear()
                examPrefs.all.forEach { (key, value) ->
                    if (value is String) {
                        val parts = value.split(":")
                        if (parts.size >= 4) {
                            val mcq = parts[0].toFloatOrNull() ?: 0f
                            val wri = parts[1].toFloatOrNull() ?: 0f
                            val maxM = parts[2].toFloatOrNull() ?: 0f
                            val maxW = parts[3].toFloatOrNull() ?: 0f
                            ExamMarksRepository.scoresState[key] = ExamScore(mcq, wri, maxM, maxW)
                        }
                    }
                }
            }

            // 3. Restore Syllabus
            if (root.has("syllabus")) {
                val syllabusPrefs = context.applicationContext.getSharedPreferences("eap_syllabus_prefs", Context.MODE_PRIVATE)
                val editor = syllabusPrefs.edit().clear()
                val syllabusJson = root.getJSONObject("syllabus")
                val keys = syllabusJson.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    editor.putBoolean(k, syllabusJson.getBoolean(k))
                }
                editor.apply()

                // Reload Syllabus state
                SyllabusRepository.init(context)
                // Trigger reload by re-reading values
                val currentSubs = SyllabusRepository.subjectsState.toList()
                val reloaded = currentSubs.map { sub ->
                    val updatedPapers = sub.papers.map { pap ->
                        val updatedChapters = pap.chapters.map { chap ->
                            val updatedSections = chap.sections.map { sec ->
                                val key = "sec_${sub.name}_${pap.name}_${chap.name}_${sec.name}".replace(" ", "_")
                                val isDone = syllabusPrefs.getBoolean(key, false)
                                sec.copy(isCompleted = isDone)
                            }
                            chap.copy(sections = updatedSections)
                        }
                        pap.copy(chapters = updatedChapters)
                    }
                    sub.copy(papers = updatedPapers)
                }
                SyllabusRepository.subjectsState.clear()
                SyllabusRepository.subjectsState.addAll(reloaded)
            }

            true
        } catch (_: Exception) {
            false
        }
    }

    fun resetSyllabus(context: Context) {
        val syllabusPrefs = context.applicationContext.getSharedPreferences("eap_syllabus_prefs", Context.MODE_PRIVATE)
        syllabusPrefs.edit().clear().apply()

        val currentSubs = SyllabusRepository.subjectsState.toList()
        val cleared = currentSubs.map { sub ->
            val updatedPapers = sub.papers.map { pap ->
                val updatedChapters = pap.chapters.map { chap ->
                    val updatedSections = chap.sections.map { sec ->
                        sec.copy(isCompleted = false)
                    }
                    chap.copy(sections = updatedSections)
                }
                pap.copy(chapters = updatedChapters)
            }
            sub.copy(papers = updatedPapers)
        }
        SyllabusRepository.subjectsState.clear()
        SyllabusRepository.subjectsState.addAll(cleared)
    }

    fun resetExamMarks(context: Context) {
        val examPrefs = context.applicationContext.getSharedPreferences("eap_exam_marks_prefs", Context.MODE_PRIVATE)
        examPrefs.edit().clear().apply()
        ExamMarksRepository.scoresState.clear()
    }
}
