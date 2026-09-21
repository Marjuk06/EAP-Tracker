package com.marjuk.eaptracker.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CastForEducation
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Speed
import androidx.compose.runtime.mutableStateListOf

object SyllabusRepository {
    private const val PREFS_NAME = "eap_syllabus_prefs"
    private var prefs: SharedPreferences? = null
    private var isInitialized = false

    val subjectsState = mutableStateListOf<Subject>()

    private var appContext: Context? = null

    fun init(context: Context) {
        if (isInitialized) return
        appContext = context.applicationContext
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadSubjects()
        isInitialized = true
    }

    private fun getInitialSubjects(): List<Subject> {
        return listOf(
            Subject(
                "Physics", Icons.Default.Speed, listOf(
                    Paper(
                        "1st Paper", listOf(
                            "ভৌত জগৎ ও পরিমাপ", "ভেক্টর", "গতিবিদ্যা", "নিউটনীয় বলবিদ্যা",
                            "কাজ, শক্তি ও ক্ষমতা", "মহাকর্ষ ও অভিকর্ষ", "পদার্থের গাঠনিক ধর্ম",
                            "পর্যায়বৃত্ত গতি", "তরঙ্গ", "আদর্শ গ্যাস ও গ্যাসের গতি তত্ত্ব"
                        ).map { Chapter(it) }
                    ),
                    Paper(
                        "2nd Paper", listOf(
                            "তাপগতিবিদ্যা", "স্থির তড়িৎ", "চল তড়িৎ",
                            "তড়িৎ প্রবাহের চৌম্বক ক্রিয়া ও চৌম্বকত্ব", "তড়িৎচৌম্বক আবেশ ও পরিবর্তী প্রবাহ",
                            "জ্যামিতিক আলোকবিজ্ঞান", "ভৌত আলোকবিজ্ঞান", "আধুনিক পদার্থবিজ্ঞানের সূচনা",
                            "পরমাণুর মডেল ও নিউক্লিয়ার পদার্থবিজ্ঞান", "সেমিকন্ডাক্টর ও ইলেকট্রনিক্স", "জ্যোতির্বিজ্ঞান"
                        ).map { Chapter(it) }
                    )
                )
            ),
            Subject(
                "Chemistry", Icons.Default.Science, listOf(
                    Paper(
                        "1st Paper", listOf(
                            "ল্যাবরেটরির নিরাপদ ব্যবহার", "গুণগত রসায়ন",
                            "মৌলের পর্যায়বৃত্ত ধর্ম ও রাসায়নিক বন্ধন", "রাসায়নিক পরিবর্তন", "কর্মমুখী রসায়ন"
                        ).map { Chapter(it) }
                    ),
                    Paper(
                        "2nd Paper", listOf(
                            "পরিবেশ রসায়ন", "জৈব রসায়ন", "পরিমাণগত রসায়ন", "তড়িৎ রসায়ন", "অর্থনৈতিক রসায়ন"
                        ).map { Chapter(it) }
                    )
                )
            ),
            Subject(
                "Higher Math", Icons.Default.Calculate, listOf(
                    Paper(
                        "1st Paper", listOf(
                            "ম্যাট্রিক্স ও নির্ণায়ক", "ভেক্টর", "সরলরেখা", "বৃত্ত",
                            "বিন্যাস ও সমাবেশ", "ত্রিকোণমিতিক অনুপাত", "সংযুক্ত কোণের ত্রিকোণমিতিক অনুপাত",
                            "ফাংশন ও ফাংশনের লেখচিত্র", "অন্তরীকরণ", "যোগজীকরণ"
                        ).map { Chapter(it) }
                    ),
                    Paper(
                        "2nd Paper", listOf(
                            "বাস্তব সংখ্যা ও অসমতা", "যোগাশ্রয়ী প্রোগ্রাম", "জটিল সংখ্যা",
                            "বহুপদী ও বহুপদী সমীকরণ", "দ্বিপদী বিস্তৃতি", "কণিক",
                            "বিপরীত ত্রিকোণমিতিক ফাংশন ও সমীকরণ", "স্থিতিবিদ্যা", "সমতলে বস্তুকণার গতি",
                            "বিস্তার পরিমাপ ও সম্ভাবনা"
                        ).map { Chapter(it) }
                    )
                )
            ),
            Subject(
                "Biology", Icons.Default.Spa, listOf(
                    Paper(
                        "1st Paper", listOf(
                            "কোষ ও এর গঠন", "কোষ বিভাজন", "অণুজীব", "নগ্নবীজী ও আবৃতবীজী",
                            "টিস্যু ও টিস্যুতন্ত্র", "উদ্ভিদ শারীরতত্ত্ব", "জীবপ্রযুক্তি"
                        ).map { Chapter(it) }
                    ),
                    Paper(
                        "2nd Paper", listOf(
                            "প্রাণীর বিভিন্নতা ও শ্রেণিবিন্যাস", "প্রাণীর পরিচিতি (হাইড্রা, ঘাসফড়িং, রুই)",
                            "পরিপাক ও শোষণ", "রক্ত ও সংবহন", "শ্বসন ও শ্বাসক্রিয়া", "চলন ও অঙ্গচালনা", "জিনতত্ত্ব ও বিবর্তন"
                        ).map { Chapter(it) }
                    )
                )
            )
        )
    }

    private fun loadSubjects() {
        val base = getInitialSubjects()
        val p = prefs
        val loaded = base.map { sub ->
            val updatedPapers = sub.papers.map { pap ->
                val updatedChapters = pap.chapters.map { chap ->
                    val updatedSections = chap.sections.map { sec ->
                        val key = makeSectionKey(sub.name, pap.name, chap.name, sec.name)
                        val isDone = (p?.getBoolean(key, false) == true) || 
                            (sec.name == "QB" && p?.getBoolean(makeSectionKey(sub.name, pap.name, chap.name, "Bank"), false) == true)
                        sec.copy(isCompleted = isDone)
                    }
                    chap.copy(sections = updatedSections)
                }
                pap.copy(chapters = updatedChapters)
            }
            sub.copy(papers = updatedPapers)
        }
        subjectsState.clear()
        subjectsState.addAll(loaded)
    }

    fun updateAllSubjects(newSubjects: List<Subject>) {
        val p = prefs
        val loaded = newSubjects.map { sub ->
            val updatedPapers = sub.papers.map { pap ->
                val updatedChapters = pap.chapters.map { chap ->
                    val updatedSections = chap.sections.map { sec ->
                        val key = makeSectionKey(sub.name, pap.name, chap.name, sec.name)
                        val isDone = (p?.getBoolean(key, false) == true) || 
                            (sec.name == "QB" && p?.getBoolean(makeSectionKey(sub.name, pap.name, chap.name, "Bank"), false) == true)
                        sec.copy(isCompleted = isDone)
                    }
                    chap.copy(sections = updatedSections)
                }
                pap.copy(chapters = updatedChapters)
            }
            sub.copy(papers = updatedPapers)
        }
        subjectsState.clear()
        subjectsState.addAll(loaded)
    }

    fun toggleSection(subjectName: String, paperName: String, chapterIndex: Int, sectionIndex: Int) {
        val subIndex = subjectsState.indexOfFirst { it.name == subjectName }
        if (subIndex == -1) return
        val sub = subjectsState[subIndex]

        val papIndex = sub.papers.indexOfFirst { it.name == paperName }
        if (papIndex == -1) return
        val pap = sub.papers[papIndex]

        if (chapterIndex !in pap.chapters.indices) return
        val chap = pap.chapters[chapterIndex]

        if (sectionIndex !in chap.sections.indices) return
        val sec = chap.sections[sectionIndex]

        val newCompleted = !sec.isCompleted
        val key = makeSectionKey(sub.name, pap.name, chap.name, sec.name)
        prefs?.edit()?.putBoolean(key, newCompleted)?.apply()

        val newSections = chap.sections.mapIndexed { idx, s ->
            if (idx == sectionIndex) s.copy(isCompleted = newCompleted) else s
        }
        val newChapters = pap.chapters.mapIndexed { idx, c ->
            if (idx == chapterIndex) c.copy(sections = newSections) else c
        }
        val newPapers = sub.papers.mapIndexed { idx, p ->
            if (idx == papIndex) p.copy(chapters = newChapters) else p
        }
        val newSub = sub.copy(papers = newPapers)

        subjectsState[subIndex] = newSub

        // Record active streak if completing a section
        if (newCompleted) {
            ProgressRepository.recordActivity()
        }
        appContext?.let { com.marjuk.eaptracker.widget.SyllabusProgressWidgetProvider.updateAllWidgets(it) }
        appContext?.let { BackupRepository.notifyDataChanged(it) }
    }

    fun getTotalChaptersStats(): Pair<Int, Int> {
        val total = subjectsState.sumOf { it.totalChapters }
        val completed = subjectsState.sumOf { it.completedChapters }
        return Pair(completed, total)
    }

    fun getSubjectProgress(subjectName: String): Pair<Int, Int> {
        val sub = subjectsState.find { it.name.contains(subjectName, ignoreCase = true) }
        return Pair(sub?.completedChapters ?: 0, sub?.totalChapters ?: 0)
    }

    fun getSubjectIcon(name: String): androidx.compose.ui.graphics.vector.ImageVector {
        val lower = name.lowercase()
        return when {
            "phys" in lower -> Icons.Default.Speed
            "chem" in lower -> Icons.Default.Science
            "math" in lower -> Icons.Default.Calculate
            "bio" in lower -> Icons.Default.Spa
            "ict" in lower -> Icons.Default.CastForEducation
            else -> Icons.AutoMirrored.Filled.MenuBook
        }
    }

    fun parseSyllabusJson(jsonStr: String): List<Subject> {
        val list = mutableListOf<Subject>()
        try {
            val jsonArray = org.json.JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val subObj = jsonArray.getJSONObject(i)
                val subName = subObj.optString("name", "Subject ${i + 1}")
                val icon = getSubjectIcon(subName)
                val papersList = mutableListOf<Paper>()
                val papersArr = subObj.optJSONArray("papers")
                if (papersArr != null) {
                    for (p in 0 until papersArr.length()) {
                        val papObj = papersArr.getJSONObject(p)
                        val papName = papObj.optString("name", "Paper ${p + 1}")
                        val chapsList = mutableListOf<Chapter>()
                        val chapsArr = papObj.optJSONArray("chapters")
                        if (chapsArr != null) {
                            for (c in 0 until chapsArr.length()) {
                                val cItem = chapsArr.get(c)
                                val cName = if (cItem is org.json.JSONObject) cItem.optString("name", "Chapter ${c + 1}") else cItem.toString()
                                chapsList.add(Chapter(cName))
                            }
                        }
                        papersList.add(Paper(papName, chapsList))
                    }
                }
                list.add(Subject(subName, icon, papersList))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun resetToDefaultSyllabus() {
        loadSubjects()
    }

    private fun makeSectionKey(sub: String, pap: String, chap: String, sec: String): String {
        return "sec_${sub}_${pap}_${chap}_${sec}".replace(" ", "_")
    }
}
