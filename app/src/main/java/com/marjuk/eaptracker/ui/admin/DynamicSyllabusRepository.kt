package com.marjuk.eaptracker.ui.admin

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Speed
import com.marjuk.eaptracker.ui.Chapter
import com.marjuk.eaptracker.ui.Paper
import com.marjuk.eaptracker.ui.Subject
import com.marjuk.eaptracker.ui.SyllabusRepository

object DynamicSyllabusRepository {

    fun addSubject(name: String, iconName: String = "Book") {
        val icon = when (iconName.lowercase()) {
            "physics", "speed" -> Icons.Default.Speed
            "chemistry", "science" -> Icons.Default.Science
            "math", "calculate" -> Icons.Default.Calculate
            "biology", "spa" -> Icons.Default.Spa
            else -> Icons.Default.Book
        }
        val defaultPapers = listOf(
            Paper("1st Paper", emptyList()),
            Paper("2nd Paper", emptyList())
        )
        val newSub = Subject(name, icon, defaultPapers)
        SyllabusRepository.subjectsState.add(newSub)
    }

    fun deleteSubject(subjectName: String) {
        val idx = SyllabusRepository.subjectsState.indexOfFirst { it.name.equals(subjectName, ignoreCase = true) }
        if (idx != -1) {
            SyllabusRepository.subjectsState.removeAt(idx)
        }
    }

    fun addChapter(subjectName: String, paperName: String, chapterName: String) {
        val subIndex = SyllabusRepository.subjectsState.indexOfFirst { it.name.equals(subjectName, ignoreCase = true) }
        if (subIndex == -1) return
        val sub = SyllabusRepository.subjectsState[subIndex]

        val papIndex = sub.papers.indexOfFirst { it.name.equals(paperName, ignoreCase = true) }
        if (papIndex == -1) return
        val pap = sub.papers[papIndex]

        val updatedChapters = pap.chapters.toMutableList().apply {
            add(Chapter(chapterName))
        }

        val updatedPapers = sub.papers.mapIndexed { idx, p ->
            if (idx == papIndex) p.copy(chapters = updatedChapters) else p
        }

        SyllabusRepository.subjectsState[subIndex] = sub.copy(papers = updatedPapers)
    }

    fun deleteChapter(subjectName: String, paperName: String, chapterIndex: Int) {
        val subIndex = SyllabusRepository.subjectsState.indexOfFirst { it.name.equals(subjectName, ignoreCase = true) }
        if (subIndex == -1) return
        val sub = SyllabusRepository.subjectsState[subIndex]

        val papIndex = sub.papers.indexOfFirst { it.name.equals(paperName, ignoreCase = true) }
        if (papIndex == -1) return
        val pap = sub.papers[papIndex]

        if (chapterIndex !in pap.chapters.indices) return
        val updatedChapters = pap.chapters.toMutableList().apply {
            removeAt(chapterIndex)
        }

        val updatedPapers = sub.papers.mapIndexed { idx, p ->
            if (idx == papIndex) p.copy(chapters = updatedChapters) else p
        }

        SyllabusRepository.subjectsState[subIndex] = sub.copy(papers = updatedPapers)
    }

    fun importSubjectsFromJson(jsonStr: String, context: Context? = null): Boolean {
        return try {
            val arr = org.json.JSONArray(jsonStr)
            if (arr.length() == 0) return false
            val parsedList = mutableListOf<Subject>()
            for (i in 0 until arr.length()) {
                val subObj = arr.getJSONObject(i)
                val subName = subObj.optString("name", "Subject $i")
                val icon = when {
                    subName.contains("Physics", ignoreCase = true) -> Icons.Default.Speed
                    subName.contains("Chemistry", ignoreCase = true) -> Icons.Default.Science
                    subName.contains("Math", ignoreCase = true) -> Icons.Default.Calculate
                    subName.contains("Biology", ignoreCase = true) -> Icons.Default.Spa
                    else -> Icons.Default.Book
                }
                val papersArr = subObj.optJSONArray("papers") ?: org.json.JSONArray()
                val parsedPapers = mutableListOf<Paper>()
                for (p in 0 until papersArr.length()) {
                    val paperObj = papersArr.getJSONObject(p)
                    val paperName = paperObj.optString("name", "Paper ${p + 1}")
                    val chaptersArr = paperObj.optJSONArray("chapters") ?: org.json.JSONArray()
                    val parsedChapters = mutableListOf<Chapter>()
                    for (c in 0 until chaptersArr.length()) {
                        val chapObj = chaptersArr.getJSONObject(c)
                        val chapName = chapObj.optString("name", "")
                        if (chapName.isNotBlank()) {
                            parsedChapters.add(Chapter(chapName))
                        }
                    }
                    parsedPapers.add(Paper(paperName, parsedChapters))
                }
                parsedList.add(Subject(subName, icon, parsedPapers))
            }
            if (parsedList.isNotEmpty()) {
                SyllabusRepository.updateAllSubjects(parsedList)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
