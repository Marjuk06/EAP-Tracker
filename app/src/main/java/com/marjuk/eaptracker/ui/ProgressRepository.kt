package com.marjuk.eaptracker.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class SubjectExamStats(
    val subjectName: String,
    val examsCount: Int,
    val totalObtained: Float,
    val totalMax: Float,
    val averagePercentage: Float
)

data class Milestone(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean
)

object ProgressRepository {
    private const val PREFS_NAME = "eap_progress_prefs"
    private var prefs: SharedPreferences? = null

    val currentStreak = mutableStateOf(3)
    val longestStreak = mutableStateOf(7)
    val activeDays = mutableStateListOf(true, false, true, false, true, true, false) // Sat to Fri

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val p = prefs ?: return
        currentStreak.value = p.getInt("current_streak", 3)
        longestStreak.value = p.getInt("longest_streak", 7)
    }

    fun recordActivity() {
        val count = ExamMarksRepository.scoresState.size
        if (count > 0) {
            val newStreak = maxOf(currentStreak.value, minOf(count, 14))
            currentStreak.value = newStreak
            if (newStreak > longestStreak.value) {
                longestStreak.value = newStreak
                prefs?.edit()?.putInt("longest_streak", newStreak)?.apply()
            }
            prefs?.edit()?.putInt("current_streak", newStreak)?.apply()
        }
    }

    fun getOverallExamStats(): Triple<Int, Float, Float> {
        val scores = ExamMarksRepository.scoresState.values
        val count = scores.size
        var totalObtained = 0f
        var totalMax = 0f
        scores.forEach {
            totalObtained += it.totalObtained
            totalMax += it.totalMax
        }
        return Triple(count, totalObtained, totalMax)
    }

    fun getSubjectStats(subjectPrefix: String, displayName: String): SubjectExamStats {
        val matchingScores = ExamMarksRepository.scoresState.filterKeys { key ->
            key.contains(subjectPrefix, ignoreCase = true) ||
            (subjectPrefix == "P" && key.contains("Physics", ignoreCase = true)) ||
            (subjectPrefix == "C" && key.contains("Chemistry", ignoreCase = true)) ||
            (subjectPrefix == "M" && key.contains("Math", ignoreCase = true)) ||
            (subjectPrefix == "Bio" && key.contains("Biology", ignoreCase = true))
        }.values

        val count = matchingScores.size
        var obtained = 0f
        var max = 0f
        matchingScores.forEach {
            obtained += it.totalObtained
            max += it.totalMax
        }
        val avg = if (max > 0f) (obtained / max) * 100f else 0f
        return SubjectExamStats(displayName, count, obtained, max, avg)
    }

    fun getMilestones(): List<Milestone> {
        val (count, obtained, max) = getOverallExamStats()
        val overallAvg = if (max > 0f) (obtained / max) * 100f else 0f

        return listOf(
            Milestone(
                id = "first_exam",
                title = "First Step",
                description = "Completed your first exam",
                icon = "🎯",
                isUnlocked = count >= 1
            ),
            Milestone(
                id = "streak_master",
                title = "Consistency Hero",
                description = "Achieved a 3-day study streak",
                icon = "🔥",
                isUnlocked = currentStreak.value >= 3
            ),
            Milestone(
                id = "high_scorer",
                title = "Top Performer",
                description = "Scored 85%+ in overall exams",
                icon = "⭐",
                isUnlocked = count >= 1 && overallAvg >= 85f
            ),
            Milestone(
                id = "five_exams",
                title = "Dedicated Scholar",
                description = "Submitted 5 exam scores",
                icon = "🏆",
                isUnlocked = count >= 5
            ),
            Milestone(
                id = "ten_exams",
                title = "Exam Warrior",
                description = "Submitted 10 exam scores",
                icon = "👑",
                isUnlocked = count >= 10
            )
        )
    }
}
