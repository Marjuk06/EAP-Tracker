package com.marjuk.eaptracker.ui.admin

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import com.marjuk.eaptracker.ui.RoutineItem
import com.marjuk.eaptracker.ui.offlineRoutine
import com.marjuk.eaptracker.ui.onlineRoutine
import org.json.JSONArray
import org.json.JSONObject

object DynamicRoutineRepository {
    private const val PREFS_NAME = "eap_dynamic_routine_prefs"
    private const val KEY_OFFLINE = "custom_offline_routine"
    private const val KEY_ONLINE = "custom_online_routine"
    private const val KEY_IS_CUSTOM_OFFLINE = "is_custom_offline_routine_active"
    private const val KEY_IS_CUSTOM_ONLINE = "is_custom_online_routine_active"

    private var prefs: SharedPreferences? = null
    private var isInitialized = false

    val offlineRoutines = mutableStateListOf<RoutineItem>()
    val onlineRoutines = mutableStateListOf<RoutineItem>()

    fun init(context: Context) {
        if (isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadRoutines()
        isInitialized = true
    }

    fun isCustomRoutineActive(type: String): Boolean {
        val isOnline = type.equals("Online", ignoreCase = true)
        val key = if (isOnline) KEY_IS_CUSTOM_ONLINE else KEY_IS_CUSTOM_OFFLINE
        return prefs?.getBoolean(key, false) ?: false
    }

    fun setCustomRoutine(type: String, list: List<RoutineItem>) {
        if (list.isEmpty()) return
        val isOnline = type.equals("Online", ignoreCase = true)
        val targetList = if (isOnline) onlineRoutines else offlineRoutines
        val key = if (isOnline) KEY_ONLINE else KEY_OFFLINE
        val activeKey = if (isOnline) KEY_IS_CUSTOM_ONLINE else KEY_IS_CUSTOM_OFFLINE

        targetList.clear()
        targetList.addAll(list)

        val jsonArray = serializeRoutines(targetList)
        prefs?.edit()
            ?.putString(key, jsonArray.toString())
            ?.putBoolean(activeKey, true)
            ?.apply()
    }

    fun clearCustomRoutine(type: String) {
        val isOnline = type.equals("Online", ignoreCase = true)
        val activeKey = if (isOnline) KEY_IS_CUSTOM_ONLINE else KEY_IS_CUSTOM_OFFLINE
        prefs?.edit()?.remove(activeKey)?.apply()
        resetToDefault(type)
    }

    private fun loadRoutines() {
        val p = prefs ?: return
        val offlineJson = p.getString(KEY_OFFLINE, null)
        val onlineJson = p.getString(KEY_ONLINE, null)

        offlineRoutines.clear()
        if (!offlineJson.isNullOrBlank()) {
            val parsed = parseRoutineJson(offlineJson)
            if (parsed.isNotEmpty()) {
                offlineRoutines.addAll(parsed)
            } else {
                offlineRoutines.addAll(offlineRoutine)
            }
        } else {
            offlineRoutines.addAll(offlineRoutine)
        }

        onlineRoutines.clear()
        if (!onlineJson.isNullOrBlank()) {
            val parsed = parseRoutineJson(onlineJson)
            if (parsed.isNotEmpty()) {
                onlineRoutines.addAll(parsed)
            } else {
                onlineRoutines.addAll(onlineRoutine)
            }
        } else {
            onlineRoutines.addAll(onlineRoutine)
        }
    }

    fun getRoutines(type: String): List<RoutineItem> {
        return if (type.equals("Online", ignoreCase = true)) onlineRoutines else offlineRoutines
    }

    fun addRoutineItem(type: String, item: RoutineItem) {
        val list = if (type.equals("Online", ignoreCase = true)) onlineRoutines else offlineRoutines
        list.add(item)
        saveRoutines(type)
    }

    fun updateRoutineItem(type: String, index: Int, item: RoutineItem) {
        val list = if (type.equals("Online", ignoreCase = true)) onlineRoutines else offlineRoutines
        if (index in list.indices) {
            list[index] = item
            saveRoutines(type)
        }
    }

    fun mergeSingleDay(type: String, item: RoutineItem) {
        val list = if (type.equals("Online", ignoreCase = true)) onlineRoutines else offlineRoutines
        val idx = list.indexOfFirst { it.date.trim().equals(item.date.trim(), ignoreCase = true) }
        if (idx >= 0) {
            list[idx] = item
        } else {
            list.add(item)
        }
        saveRoutines(type)
    }

    fun deleteRoutineItem(type: String, index: Int) {
        val list = if (type.equals("Online", ignoreCase = true)) onlineRoutines else offlineRoutines
        if (index in list.indices) {
            list.removeAt(index)
            saveRoutines(type)
        }
    }

    fun resetToDefault(type: String) {
        if (type.equals("Online", ignoreCase = true)) {
            onlineRoutines.clear()
            onlineRoutines.addAll(onlineRoutine)
            prefs?.edit()?.remove(KEY_ONLINE)?.remove(KEY_IS_CUSTOM_ONLINE)?.apply()
        } else {
            offlineRoutines.clear()
            offlineRoutines.addAll(offlineRoutine)
            prefs?.edit()?.remove(KEY_OFFLINE)?.remove(KEY_IS_CUSTOM_OFFLINE)?.apply()
        }
    }

    fun saveRoutines(type: String) {
        val p = prefs ?: return
        val isOnline = type.equals("Online", ignoreCase = true)
        val list = if (isOnline) onlineRoutines else offlineRoutines
        val key = if (isOnline) KEY_ONLINE else KEY_OFFLINE

        val jsonArray = serializeRoutines(list)
        p.edit().putString(key, jsonArray.toString()).apply()
    }

    fun serializeRoutines(list: List<RoutineItem>): JSONArray {
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("date", item.date)
            obj.put("day", item.day)
            obj.put("classSubject", item.classSubject ?: JSONObject.NULL)
            
            when (val ex = item.examDetails) {
                is String -> obj.put("examDetails", ex)
                is List<*> -> {
                    val exArr = JSONArray()
                    ex.forEach { if (it != null) exArr.put(it.toString()) }
                    obj.put("examDetails", exArr)
                }
                else -> obj.put("examDetails", JSONObject.NULL)
            }

            val topicsArray = JSONArray()
            item.topics?.forEach { topicsArray.put(it) }
            obj.put("topics", topicsArray)
            array.put(obj)
        }
        return array
    }

    fun parseRoutineJson(jsonStr: String): List<RoutineItem> {
        val list = mutableListOf<RoutineItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val date = obj.optString("date", "")
                val day = obj.optString("day", "")
                val classSubject = if (obj.isNull("classSubject")) null else obj.optString("classSubject")
                
                val examDetails: Any? = if (obj.isNull("examDetails")) {
                    null
                } else {
                    val raw = obj.get("examDetails")
                    if (raw is JSONArray) {
                        val strList = mutableListOf<String>()
                        for (k in 0 until raw.length()) {
                            strList.add(raw.optString(k))
                        }
                        strList
                    } else {
                        raw.toString()
                    }
                }
                
                val topicsList = mutableListOf<String>()
                val topicsArray = obj.optJSONArray("topics")
                if (topicsArray != null) {
                    for (j in 0 until topicsArray.length()) {
                        topicsList.add(topicsArray.optString(j))
                    }
                }
                list.add(RoutineItem(date, day, classSubject, examDetails, topicsList))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun parseSingleRoutineItem(jsonStr: String): RoutineItem? {
        return try {
            val obj = JSONObject(jsonStr)
            val date = obj.optString("date", "")
            val day = obj.optString("day", "")
            val classSubject = if (obj.isNull("classSubject")) null else obj.optString("classSubject")
            val examDetails = if (obj.isNull("examDetails")) null else obj.opt("examDetails")
            val topicsList = mutableListOf<String>()
            val topicsArray = obj.optJSONArray("topics")
            if (topicsArray != null) {
                for (j in 0 until topicsArray.length()) {
                    topicsList.add(topicsArray.optString(j))
                }
            }
            RoutineItem(date, day, classSubject, examDetails, topicsList)
        } catch (e: Exception) {
            null
        }
    }

    fun importRoutinesFromJson(type: String, jsonStr: String): Boolean {
        val parsed = parseRoutineJson(jsonStr)
        if (parsed.isEmpty()) return false
        
        val list = if (type.equals("Online", ignoreCase = true)) onlineRoutines else offlineRoutines
        if (parsed.size == 1) {
            // Single day update
            mergeSingleDay(type, parsed[0])
        } else {
            // Full routine replacement
            list.clear()
            list.addAll(parsed)
            saveRoutines(type)
        }
        return true
    }
}
