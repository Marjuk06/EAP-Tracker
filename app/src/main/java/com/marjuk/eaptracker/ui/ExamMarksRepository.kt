package com.marjuk.eaptracker.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

data class ExamScore(
    val mcqObtained: Float = 0f,
    val writtenObtained: Float = 0f,
    val maxMcq: Float = 0f,
    val maxWritten: Float = 0f
) {
    val totalObtained: Float get() = mcqObtained + writtenObtained
    val totalMax: Float get() = maxMcq + maxWritten
    val percentage: Float get() = if (totalMax > 0f) (totalObtained / totalMax) * 100f else 0f
}

object ExamMarksRepository {
    private const val PREFS_NAME = "eap_exam_marks_prefs"
    private var prefs: SharedPreferences? = null
    val scoresState = mutableStateMapOf<String, ExamScore>()
    private var appContext: Context? = null
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        appContext = context.applicationContext
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadAll()
        isInitialized = true
    }

    private fun loadAll() {
        val p = prefs ?: return
        p.all.forEach { (key, value) ->
            if (value is String) {
                // format: mcqObtained:writtenObtained:maxMcq:maxWritten
                val parts = value.split(":")
                if (parts.size >= 4) {
                    val mcq = parts[0].toFloatOrNull() ?: 0f
                    val wri = parts[1].toFloatOrNull() ?: 0f
                    val maxM = parts[2].toFloatOrNull() ?: 0f
                    val maxW = parts[3].toFloatOrNull() ?: 0f
                    scoresState[key] = ExamScore(mcq, wri, maxM, maxW)
                }
            }
        }
    }

    fun getScore(key: String): ExamScore? = scoresState[key]

    fun saveScore(key: String, mcq: Float, written: Float, maxMcq: Float, maxWritten: Float) {
        val score = ExamScore(mcq, written, maxMcq, maxWritten)
        scoresState[key] = score
        prefs?.edit()?.putString(key, "$mcq:$written:$maxMcq:$maxWritten")?.apply()
        appContext?.let { 
            com.marjuk.eaptracker.widget.SyllabusProgressWidgetProvider.updateAllWidgets(it)
            BackupRepository.notifyDataChanged(it) 
        }
    }

    fun clearScore(key: String) {
        scoresState.remove(key)
        prefs?.edit()?.remove(key)?.apply()
        appContext?.let { 
            com.marjuk.eaptracker.widget.SyllabusProgressWidgetProvider.updateAllWidgets(it)
            BackupRepository.notifyDataChanged(it) 
        }
    }

    fun parseExamStructure(examText: String): Pair<Float, Float> {
        // Returns Pair(maxMcq, maxWritten)
        var maxMcq = 0f
        var maxWritten = 0f

        val mcqMatch = Regex("""MCQ\s*\((\d+)\)""", RegexOption.IGNORE_CASE).find(examText)
        if (mcqMatch != null) {
            maxMcq = mcqMatch.groupValues[1].toFloatOrNull() ?: 0f
        }

        val wriMatch = Regex("""(?:Written|Wri\.?)\s*\((\d+)\)""", RegexOption.IGNORE_CASE).find(examText)
        if (wriMatch != null) {
            maxWritten = wriMatch.groupValues[1].toFloatOrNull() ?: 0f
        }

        // If neither was matched (e.g. Weekly Exam or special test)
        if (maxMcq == 0f && maxWritten == 0f) {
            if (examText.contains("Weekly", ignoreCase = true) || examText.contains("Live", ignoreCase = true)) {
                maxMcq = 100f // default standard weekly exam
            } else if (examText.contains("600")) {
                maxWritten = 600f
            } else {
                maxMcq = 30f
            }
        }

        return Pair(maxMcq, maxWritten)
    }

    fun makeKey(date: String, examName: String): String {
        return "${date}_${examName.replace(" ", "_")}"
    }

    fun parseKey(key: String): Pair<String, String> {
        val idx = key.indexOf('_')
        if (idx != -1) {
            val date = key.substring(0, idx)
            val name = key.substring(idx + 1).replace("_", " ")
            return Pair(date, name)
        }
        return Pair("", key)
    }
}
