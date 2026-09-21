package com.marjuk.eaptracker.ui.admin

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import com.marjuk.eaptracker.ui.ExamItem
import com.marjuk.eaptracker.ui.offlineExams
import com.marjuk.eaptracker.ui.onlineExams
import org.json.JSONArray
import org.json.JSONObject

object DynamicExamRepository {
    private const val PREFS_NAME = "eap_dynamic_exam_prefs"
    private const val KEY_OFFLINE = "custom_offline_exams"
    private const val KEY_ONLINE = "custom_online_exams"
    private const val KEY_IS_CUSTOM_OFFLINE_EXAM = "is_custom_offline_exam_active"
    private const val KEY_IS_CUSTOM_ONLINE_EXAM = "is_custom_online_exam_active"

    private var prefs: SharedPreferences? = null
    private var isInitialized = false

    val offlineExamList = mutableStateListOf<ExamItem>()
    val onlineExamList = mutableStateListOf<ExamItem>()

    fun init(context: Context) {
        if (isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadExams()
        isInitialized = true
    }

    fun isCustomExamActive(type: String): Boolean {
        val isOnline = type.equals("Online", ignoreCase = true)
        val key = if (isOnline) KEY_IS_CUSTOM_ONLINE_EXAM else KEY_IS_CUSTOM_OFFLINE_EXAM
        return prefs?.getBoolean(key, false) ?: false
    }

    fun setCustomExams(type: String, list: List<ExamItem>) {
        if (list.isEmpty()) return
        val isOnline = type.equals("Online", ignoreCase = true)
        val targetList = if (isOnline) onlineExamList else offlineExamList
        val key = if (isOnline) KEY_ONLINE else KEY_OFFLINE
        val activeKey = if (isOnline) KEY_IS_CUSTOM_ONLINE_EXAM else KEY_IS_CUSTOM_OFFLINE_EXAM

        targetList.clear()
        targetList.addAll(list)

        val jsonArray = serializeExams(targetList)
        prefs?.edit()
            ?.putString(key, jsonArray.toString())
            ?.putBoolean(activeKey, true)
            ?.apply()
    }

    fun clearCustomExam(type: String) {
        val isOnline = type.equals("Online", ignoreCase = true)
        val activeKey = if (isOnline) KEY_IS_CUSTOM_ONLINE_EXAM else KEY_IS_CUSTOM_OFFLINE_EXAM
        prefs?.edit()?.remove(activeKey)?.apply()
        resetToDefault(type)
    }

    private fun loadExams() {
        val p = prefs ?: return
        val offlineJson = p.getString(KEY_OFFLINE, null)
        val onlineJson = p.getString(KEY_ONLINE, null)

        offlineExamList.clear()
        if (!offlineJson.isNullOrBlank()) {
            val parsed = parseExamJson(offlineJson)
            if (parsed.isNotEmpty()) {
                offlineExamList.addAll(parsed)
            } else {
                offlineExamList.addAll(offlineExams)
            }
        } else {
            offlineExamList.addAll(offlineExams)
        }

        onlineExamList.clear()
        if (!onlineJson.isNullOrBlank()) {
            val parsed = parseExamJson(onlineJson)
            if (parsed.isNotEmpty()) {
                onlineExamList.addAll(parsed)
            } else {
                onlineExamList.addAll(onlineExams)
            }
        } else {
            onlineExamList.addAll(onlineExams)
        }
    }

    fun getExams(type: String): List<ExamItem> {
        return if (type.equals("Online", ignoreCase = true)) onlineExamList else offlineExamList
    }

    fun addExamItem(type: String, item: ExamItem) {
        val list = if (type.equals("Online", ignoreCase = true)) onlineExamList else offlineExamList
        list.add(item)
        saveExams(type)
    }

    fun updateExamItem(type: String, index: Int, item: ExamItem) {
        val list = if (type.equals("Online", ignoreCase = true)) onlineExamList else offlineExamList
        if (index in list.indices) {
            list[index] = item
            saveExams(type)
        }
    }

    fun mergeSingleExam(type: String, item: ExamItem) {
        val list = if (type.equals("Online", ignoreCase = true)) onlineExamList else offlineExamList
        val idx = list.indexOfFirst { it.date.trim().equals(item.date.trim(), ignoreCase = true) }
        if (idx >= 0) {
            list[idx] = item
        } else {
            list.add(item)
        }
        saveExams(type)
    }

    fun deleteExamItem(type: String, index: Int) {
        val list = if (type.equals("Online", ignoreCase = true)) onlineExamList else offlineExamList
        if (index in list.indices) {
            list.removeAt(index)
            saveExams(type)
        }
    }

    fun resetToDefault(type: String) {
        if (type.equals("Online", ignoreCase = true)) {
            onlineExamList.clear()
            onlineExamList.addAll(onlineExams)
            prefs?.edit()?.remove(KEY_ONLINE)?.remove(KEY_IS_CUSTOM_ONLINE_EXAM)?.apply()
        } else {
            offlineExamList.clear()
            offlineExamList.addAll(offlineExams)
            prefs?.edit()?.remove(KEY_OFFLINE)?.remove(KEY_IS_CUSTOM_OFFLINE_EXAM)?.apply()
        }
    }

    fun saveExams(type: String) {
        val p = prefs ?: return
        val isOnline = type.equals("Online", ignoreCase = true)
        val list = if (isOnline) onlineExamList else offlineExamList
        val key = if (isOnline) KEY_ONLINE else KEY_OFFLINE

        val jsonArray = serializeExams(list)
        p.edit().putString(key, jsonArray.toString()).apply()
    }

    fun serializeExams(list: List<ExamItem>): JSONArray {
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("date", item.date)
            obj.put("day", item.day)
            
            val examsArray = JSONArray()
            item.exams.forEach { examsArray.put(it) }
            obj.put("exams", examsArray)

            val syllabusArray = JSONArray()
            item.syllabus?.forEach { syllabusArray.put(it) }
            obj.put("syllabus", syllabusArray)

            array.put(obj)
        }
        return array
    }

    fun parseExamJson(jsonStr: String): List<ExamItem> {
        val list = mutableListOf<ExamItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val date = obj.optString("date", "")
                val day = obj.optString("day", "")
                
                val examsList = mutableListOf<String>()
                val examsArray = obj.optJSONArray("exams")
                if (examsArray != null) {
                    for (j in 0 until examsArray.length()) {
                        examsList.add(examsArray.optString(j))
                    }
                }

                val syllabusList = mutableListOf<String>()
                val syllabusArray = obj.optJSONArray("syllabus")
                if (syllabusArray != null) {
                    for (j in 0 until syllabusArray.length()) {
                        syllabusList.add(syllabusArray.optString(j))
                    }
                }

                list.add(ExamItem(date, day, examsList, syllabusList))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun importExamsFromJson(type: String, jsonStr: String): Boolean {
        val parsed = parseExamJson(jsonStr)
        if (parsed.isEmpty()) return false
        
        val list = if (type.equals("Online", ignoreCase = true)) onlineExamList else offlineExamList
        if (parsed.size == 1) {
            // Single exam date update
            mergeSingleExam(type, parsed[0])
        } else {
            // Full exam schedule replacement
            list.clear()
            list.addAll(parsed)
            saveExams(type)
        }
        return true
    }
}
