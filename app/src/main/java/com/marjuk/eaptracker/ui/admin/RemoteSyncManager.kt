package com.marjuk.eaptracker.ui.admin

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import com.marjuk.eaptracker.ui.NotificationHelper
import com.marjuk.eaptracker.ui.ProfileRepository
import com.marjuk.eaptracker.ui.NotificationHistoryRepository
import com.marjuk.eaptracker.ui.SyllabusRepository

object RemoteSyncManager {
    private const val PREFS_NAME = "eap_remote_sync_prefs"
    private const val KEY_ENDPOINT = "sync_api_endpoint"
    private const val KEY_API_KEY = "sync_web_api_key"
    private const val KEY_LAST_SYNC = "sync_last_timestamp"

    private var prefs: SharedPreferences? = null
    private var isInitialized = false

    val apiEndpointUrl = mutableStateOf("")
    val webApiKey = mutableStateOf("")
    val lastSyncTime = mutableStateOf("Never")
    val isSyncing = mutableStateOf(false)

    fun init(context: Context) {
        if (isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        NotificationHistoryRepository.init(context)
        loadSavedConfig()
        isInitialized = true
        startRealtimeListener(context)
    }

    private fun loadSavedConfig() {
        val defaultUrl = "https://eap-tracker-default-rtdb.firebaseio.com/eap_data.json"
        apiEndpointUrl.value = prefs?.getString(KEY_ENDPOINT, defaultUrl) ?: defaultUrl
        webApiKey.value = prefs?.getString(KEY_API_KEY, "") ?: ""
        lastSyncTime.value = prefs?.getString(KEY_LAST_SYNC, "Never") ?: "Never"
    }

    private var isRealtimeListening = false

    fun startRealtimeListener(context: Context) {
        if (isRealtimeListening) return
        try {
            val database = com.google.firebase.database.FirebaseDatabase.getInstance("https://eap-tracker-default-rtdb.firebaseio.com")
            val myRef = database.getReference("eap_data")
            myRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val value = snapshot.value ?: return
                    try {
                        val jsonStr = when (value) {
                            is Map<*, *> -> mapToJsonObject(value).toString()
                            is String -> value
                            else -> value.toString()
                        }
                        val ok = importMasterPayload(jsonStr, context.applicationContext)
                        if (ok) {
                            val dateFmt = SimpleDateFormat("dd-MMM HH:mm", Locale.getDefault())
                            val syncStr = dateFmt.format(Date())
                            lastSyncTime.value = syncStr
                            prefs?.edit()?.putString(KEY_LAST_SYNC, syncStr)?.apply()
                            com.marjuk.eaptracker.ui.BackupRepository.notifyDataChanged(
                                context = context.applicationContext,
                                delayMs = 60_000L
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    android.util.Log.w("RemoteSyncManager", "Firebase DB listener cancelled: ${error.message}")
                }
            })

            // 2. Standalone Direct Instant Push Notification Listener
            // NOTE: This is the ONLY source of system notifications.
            // The eap_data listener above only syncs data — it does NOT send notifications.
            // This prevents duplicate notifications.
            val broadcastRef = database.getReference("broadcast_notification")
            broadcastRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val value = snapshot.value as? Map<*, *> ?: return
                    try {
                        val timestamp = (value["timestamp"] as? Number)?.toLong() ?: 0L
                        val lastSeen = prefs?.getLong("last_seen_broadcast_time", 0L) ?: 0L
                        if (timestamp > 0L && timestamp != lastSeen) {
                            val title = (value["title"] as? String)?.ifBlank { "EAP Notice" } ?: "EAP Notice"
                            val message = value["message"] as? String ?: ""
                            val category = value["category"] as? String ?: "Notice"
                            val actionUrl = (value["actionUrl"] ?: value["url"] ?: value["link"]) as? String
                            val actionButtonText = (value["actionButtonText"] ?: value["actionText"] ?: value["buttonText"]) as? String
                            val actionRoute = (value["actionRoute"] ?: value["route"]) as? String

                            if (message.isNotBlank()) {
                                NotificationHistoryRepository.addNotification(
                                    title = title,
                                    message = message,
                                    category = category,
                                    timestamp = timestamp,
                                    actionRoute = actionRoute,
                                    actionUrl = actionUrl,
                                    actionButtonText = actionButtonText
                                )
                                NotificationHelper.showScheduleUpdateNotification(
                                    context = context.applicationContext,
                                    title = title,
                                    message = message,
                                    actionUrl = actionUrl,
                                    actionButtonText = actionButtonText,
                                    actionRoute = actionRoute
                                )
                            }
                            prefs?.edit()?.putLong("last_seen_broadcast_time", timestamp)?.apply()
                            // Also mark this configVersion as notification-already-sent
                            // so importMasterPayload won't double-fire
                            prefs?.edit()?.putLong("last_notified_config_version", timestamp)?.apply()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })

            // 3. Admin Target Exam Countdown Listener
            val examConfigRef = database.getReference("target_exam_config")
            examConfigRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    if (!snapshot.exists() || snapshot.value == null) {
                        ProfileRepository.resetCountdownToDefault()
                        return
                    }
                    val value = snapshot.value as? Map<*, *> ?: return
                    try {
                        val isCleared = (value["cleared"] as? Boolean) ?: false
                        if (isCleared) {
                            ProfileRepository.resetCountdownToDefault()
                            return
                        }
                        val examName = value["examName"] as? String
                        val examEpoch = (value["examEpoch"] as? Number)?.toLong()
                        if (!examName.isNullOrBlank() && examEpoch != null && examEpoch > 0L) {
                            ProfileRepository.updateCountdown(examName, examEpoch)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })

            // 4. Daily Motivational Quote Listener
            val quoteConfigRef = database.getReference("daily_quote_config")
            quoteConfigRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val value = snapshot.value as? Map<*, *> ?: return
                    try {
                        val quoteText = value["quoteText"] as? String
                        val author = value["author"] as? String ?: "Admission Mentor"
                        val timestamp = (value["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        if (!quoteText.isNullOrBlank()) {
                            val prevQuote = prefs?.getString("quote", null)
                            ProfileRepository.quote.value = quoteText
                            prefs?.edit()?.putString("quote", quoteText)?.apply()

                            if (prevQuote != quoteText) {
                                NotificationHistoryRepository.addNotification(
                                    title = "Daily Inspiration",
                                    message = "\u201c$quoteText\u201d — $author",
                                    category = "Motivation",
                                    timestamp = timestamp
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })

            isRealtimeListening = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun mapToJsonObject(map: Map<*, *>): JSONObject {
        val obj = JSONObject()
        for ((key, value) in map) {
            if (key == null) continue
            when (value) {
                is Map<*, *> -> obj.put(key.toString(), mapToJsonObject(value))
                is List<*> -> obj.put(key.toString(), listToJsonArray(value))
                else -> obj.put(key.toString(), value)
            }
        }
        return obj
    }

    private fun listToJsonArray(list: List<*>): org.json.JSONArray {
        val array = org.json.JSONArray()
        for (item in list) {
            when (item) {
                is Map<*, *> -> array.put(mapToJsonObject(item))
                is List<*> -> array.put(listToJsonArray(item))
                else -> array.put(item)
            }
        }
        return array
    }

    fun saveConfig(endpoint: String, apiKey: String) {
        apiEndpointUrl.value = endpoint
        webApiKey.value = apiKey
        prefs?.edit()
            ?.putString(KEY_ENDPOINT, endpoint)
            ?.putString(KEY_API_KEY, apiKey)
            ?.apply()
    }

    private const val KEY_LAST_CONFIG_VERSION = "sync_last_config_version"

    fun generateMasterPayload(): String {
        val root = JSONObject()
        root.put("configVersion", System.currentTimeMillis())
        val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        root.put("lastUpdated", dateFmt.format(Date()))
        root.put("offlineRoutine", DynamicRoutineRepository.serializeRoutines(DynamicRoutineRepository.offlineRoutines))
        root.put("onlineRoutine", DynamicRoutineRepository.serializeRoutines(DynamicRoutineRepository.onlineRoutines))
        root.put("offlineExams", DynamicExamRepository.serializeExams(DynamicExamRepository.offlineExamList))
        root.put("onlineExams", DynamicExamRepository.serializeExams(DynamicExamRepository.onlineExamList))
        return root.toString(2)
    }

    fun importMasterPayload(jsonStr: String, context: Context? = null): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            val configVersion = if (root.has("configVersion")) root.optLong("configVersion", 0L) else 0L
            val updateMessage = if (root.has("updateMessage")) root.optString("updateMessage", "") else ""
            val lastSavedVersion = prefs?.getLong(KEY_LAST_CONFIG_VERSION, 0L) ?: 0L

            var hasUpdates = false
            if (root.has("offlineRoutine")) {
                if (!DynamicRoutineRepository.isCustomRoutineActive("Offline")) {
                    DynamicRoutineRepository.importRoutinesFromJson("Offline", root.getJSONArray("offlineRoutine").toString())
                    hasUpdates = true
                }
            }
            if (root.has("onlineRoutine")) {
                if (!DynamicRoutineRepository.isCustomRoutineActive("Online")) {
                    DynamicRoutineRepository.importRoutinesFromJson("Online", root.getJSONArray("onlineRoutine").toString())
                    hasUpdates = true
                }
            }
            if (root.has("offlineExams")) {
                if (!DynamicExamRepository.isCustomExamActive("Offline")) {
                    DynamicExamRepository.importExamsFromJson("Offline", root.getJSONArray("offlineExams").toString())
                    hasUpdates = true
                }
            }
            if (root.has("onlineExams")) {
                if (!DynamicExamRepository.isCustomExamActive("Online")) {
                    DynamicExamRepository.importExamsFromJson("Online", root.getJSONArray("onlineExams").toString())
                    hasUpdates = true
                }
            }
            if (root.has("singleDayRoutine")) {
                val singleObj = root.optJSONObject("singleDayRoutine")
                if (singleObj != null) {
                    val type = singleObj.optString("type", "Offline")
                    if (!DynamicRoutineRepository.isCustomRoutineActive(type)) {
                        val itemObj = singleObj.optJSONObject("item")
                        if (itemObj != null) {
                            val item = DynamicRoutineRepository.parseSingleRoutineItem(itemObj.toString())
                            if (item != null) {
                                DynamicRoutineRepository.mergeSingleDay(type, item)
                                hasUpdates = true
                            }
                        }
                    }
                }
            }
            if (root.has("singleDayExam")) {
                val singleObj = root.optJSONObject("singleDayExam")
                if (singleObj != null) {
                    val type = singleObj.optString("type", "Offline")
                    if (!DynamicExamRepository.isCustomExamActive(type)) {
                        val itemObj = singleObj.optJSONObject("item")
                        if (itemObj != null) {
                            val parsedList = DynamicExamRepository.parseExamJson("[${itemObj}]")
                            if (parsedList.isNotEmpty()) {
                                DynamicExamRepository.mergeSingleExam(type, parsedList[0])
                                hasUpdates = true
                            }
                        }
                    }
                }
            }
            if (root.has("subjects")) {
                DynamicSyllabusRepository.importSubjectsFromJson(root.getJSONArray("subjects").toString(), context)
                hasUpdates = true
            }

            // Trigger System Notification if a new published version is detected
            // DEDUP GUARD: Skip if broadcast_notification already sent a notification for this version.
            // This prevents the duplicate notification issue (eap_data + broadcast_notification both firing).
            val lastNotifiedVersion = prefs?.getLong("last_notified_config_version", 0L) ?: 0L
            val broadcastAlreadyHandled = (configVersion > 0L && configVersion == lastNotifiedVersion)

            if (context != null && configVersion > 0L && configVersion > lastSavedVersion && hasUpdates && !broadcastAlreadyHandled) {
                val explicitTitle = root.optString("updateTitle", "")
                val explicitMsg = root.optString("updateMessage", "")
                val explicitCat = root.optString("updateCategory", "")
                val explicitRoute = root.optString("updateRoute", "")

                val smartTitle: String
                val smartMsg: String
                val smartCat: String
                val smartRoute: String

                if (explicitTitle.isNotBlank() && explicitMsg.isNotBlank()) {
                    smartTitle = explicitTitle
                    smartMsg = explicitMsg
                    smartCat = explicitCat.ifBlank { "Notice" }
                    smartRoute = explicitRoute.ifBlank { "home" }
                } else if (root.has("singleDayRoutine")) {
                    val singleObj = root.optJSONObject("singleDayRoutine")
                    val itemObj = singleObj?.optJSONObject("item")
                    val sub = itemObj?.optString("classSubject", "Class") ?: "Class"
                    val date = itemObj?.optString("date", "") ?: ""
                    val day = itemObj?.optString("day", "") ?: ""
                    val time = itemObj?.optString("classTime", "") ?: ""
                    val exam = itemObj?.optString("examDetails", "") ?: ""
                    smartTitle = "Class Rescheduled: $sub"
                    val timePart = if (time.isNotBlank()) " at $time" else ""
                    val examPart = if (exam.isNotBlank()) " • Exam: $exam" else ""
                    smartMsg = "$date ($day): $sub$timePart$examPart. Tap to view updated routine."
                    smartCat = "Schedule"
                    smartRoute = "routine"
                } else if (root.has("singleDayExam")) {
                    val singleObj = root.optJSONObject("singleDayExam")
                    val itemObj = singleObj?.optJSONObject("item")
                    val date = itemObj?.optString("date", "") ?: ""
                    val day = itemObj?.optString("day", "") ?: ""
                    val examsArr = itemObj?.optJSONArray("exams")
                    val examNames = if (examsArr != null && examsArr.length() > 0) {
                        (0 until examsArr.length()).map { examsArr.optString(it) }.joinToString(", ")
                    } else "Exam Test"
                    smartTitle = "Exam Schedule Update: $date"
                    smartMsg = "$date ($day): $examNames. Check upcoming test details."
                    smartCat = "Exam"
                    smartRoute = "exam"
                } else if (root.has("subjects")) {
                    smartTitle = "Syllabus Chapters Updated"
                    smartMsg = if (explicitMsg.isNotBlank()) explicitMsg else "New syllabus chapters and checklist items have been synchronized."
                    smartCat = "Syllabus"
                    smartRoute = "syllabus"
                } else if (root.has("offlineRoutine") || root.has("onlineRoutine")) {
                    val type = if (root.has("offlineRoutine") && root.has("onlineRoutine")) "All Class Routines" 
                              else if (root.has("onlineRoutine")) "Online Live Routine" 
                              else "Offline Class Routine"
                    smartTitle = "$type Updated"
                    smartMsg = if (explicitMsg.isNotBlank()) explicitMsg else "Latest class timetable has been updated. Tap to check your schedule."
                    smartCat = "Schedule"
                    smartRoute = "routine"
                } else if (root.has("offlineExams") || root.has("onlineExams")) {
                    val type = if (root.has("offlineExams") && root.has("onlineExams")) "All Exam Schedules"
                              else if (root.has("onlineExams")) "Online Exam Calendar"
                              else "Offline Exam Routine"
                    smartTitle = "$type Updated"
                    smartMsg = if (explicitMsg.isNotBlank()) explicitMsg else "Exam routine updated. Check your upcoming tests."
                    smartCat = "Exam"
                    smartRoute = "exam"
                } else {
                    smartTitle = "Schedule & Syllabus Synchronized"
                    smartMsg = if (explicitMsg.isNotBlank()) explicitMsg else "Latest admission schedule has been updated by your mentor."
                    smartCat = "Notice"
                    smartRoute = "home"
                }

                NotificationHelper.showScheduleUpdateNotification(
                    context = context,
                    title = smartTitle,
                    message = smartMsg,
                    actionRoute = smartRoute
                )
                NotificationHistoryRepository.addNotification(
                    title = smartTitle,
                    message = smartMsg,
                    category = smartCat,
                    actionRoute = smartRoute
                )
            }

            if (configVersion > 0L) {
                prefs?.edit()?.putLong(KEY_LAST_CONFIG_VERSION, configVersion)?.apply()
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun publishToCloud(context: Context, onResult: (Boolean, String) -> Unit) {
        val endpoint = apiEndpointUrl.value.trim()
        val apiKey = webApiKey.value.trim()

        if (endpoint.isBlank()) {
            onResult(false, "Please enter your Web API Endpoint URL")
            return
        }

        isSyncing.value = true
        withContext(Dispatchers.IO) {
            try {
                val payload = generateMasterPayload()
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = if (endpoint.contains("jsonbin.io", ignoreCase = true)) "PUT" else "POST"
                conn.setRequestProperty("Content-Type", "application/json; utf-8")
                conn.setRequestProperty("Accept", "application/json")
                
                if (apiKey.isNotBlank()) {
                    conn.setRequestProperty("X-Master-Key", apiKey)
                    conn.setRequestProperty("X-Access-Key", apiKey)
                    conn.setRequestProperty("X-Api-Key", apiKey)
                    conn.setRequestProperty("Authorization", "Bearer $apiKey")
                }
                conn.doOutput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000

                OutputStreamWriter(conn.outputStream).use { writer ->
                    writer.write(payload)
                    writer.flush()
                }

                val responseCode = conn.responseCode
                if (responseCode in 200..299) {
                    val dateFmt = SimpleDateFormat("dd-MMM HH:mm", Locale.getDefault())
                    val syncStr = dateFmt.format(Date())
                    withContext(Dispatchers.Main) {
                        lastSyncTime.value = syncStr
                        prefs?.edit()?.putString(KEY_LAST_SYNC, syncStr)?.apply()
                        isSyncing.value = false
                        onResult(true, "Published updates successfully! (HTTP $responseCode)")
                    }
                } else {
                    val errorStream = conn.errorStream ?: conn.inputStream
                    val response = BufferedReader(InputStreamReader(errorStream)).readText()
                    withContext(Dispatchers.Main) {
                        isSyncing.value = false
                        onResult(false, "Server returned HTTP $responseCode: $response")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isSyncing.value = false
                    onResult(false, "Connection error: ${e.localizedMessage}")
                }
            }
        }
    }

    suspend fun fetchFromCloud(context: Context, onResult: (Boolean, String) -> Unit) {
        val endpoint = apiEndpointUrl.value.trim()
        val apiKey = webApiKey.value.trim()

        if (endpoint.isBlank()) {
            onResult(false, "Please enter your Web API Endpoint URL")
            return
        }

        isSyncing.value = true
        withContext(Dispatchers.IO) {
            try {
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/json")
                
                if (apiKey.isNotBlank()) {
                    conn.setRequestProperty("X-Master-Key", apiKey)
                    conn.setRequestProperty("X-Access-Key", apiKey)
                    conn.setRequestProperty("X-Api-Key", apiKey)
                    conn.setRequestProperty("Authorization", "Bearer $apiKey")
                }
                conn.connectTimeout = 15000
                conn.readTimeout = 15000

                val responseCode = conn.responseCode
                if (responseCode in 200..299) {
                    val response = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                    // If JSONBin, it might wrap record in "record" object
                    val jsonToParse = try {
                        val obj = JSONObject(response)
                        if (obj.has("record")) obj.getJSONObject("record").toString() else response
                    } catch (e: Exception) {
                        response
                    }

                    val ok = importMasterPayload(jsonToParse, context)

                    // Also fetch and synchronize latest target exam countdown
                    try {
                        val baseEndpoint = endpoint.substringBefore("/eap_data.json").trimEnd('/')
                        val targetUrlStr = if (baseEndpoint.contains("firebaseio.com")) {
                            "$baseEndpoint/target_exam_config.json?auth=${apiKey.ifBlank { "yleDrwUbDfga8ueOKchQEZ47XCPlrzlRNiCF1NAw" }}"
                        } else {
                            "https://eap-tracker-default-rtdb.firebaseio.com/target_exam_config.json?auth=yleDrwUbDfga8ueOKchQEZ47XCPlrzlRNiCF1NAw"
                        }
                        val targetConn = URL(targetUrlStr).openConnection() as HttpURLConnection
                        targetConn.requestMethod = "GET"
                        targetConn.connectTimeout = 8000
                        targetConn.readTimeout = 8000
                        if (targetConn.responseCode in 200..299) {
                            val targetResp = BufferedReader(InputStreamReader(targetConn.inputStream)).readText()
                            if (targetResp.isNotBlank() && targetResp != "null") {
                                val targetObj = JSONObject(targetResp)
                                val examName = targetObj.optString("examName", "")
                                val examEpoch = targetObj.optLong("examEpoch", 0L)
                                if (examName.isNotBlank() && examEpoch > 0L) {
                                    withContext(Dispatchers.Main) {
                                        ProfileRepository.updateCountdown(examName, examEpoch)
                                    }
                                }
                            }
                        }
                        targetConn.disconnect()
                    } catch (_: Exception) {}

                    val dateFmt = SimpleDateFormat("dd-MMM HH:mm", Locale.getDefault())
                    val syncStr = dateFmt.format(Date())
                    withContext(Dispatchers.Main) {
                        if (ok) {
                            lastSyncTime.value = syncStr
                            prefs?.edit()?.putString(KEY_LAST_SYNC, syncStr)?.apply()
                            isSyncing.value = false
                            onResult(true, "Fetched & synchronized latest data!")
                        } else {
                            isSyncing.value = false
                            onResult(false, "Failed to parse remote payload format")
                        }
                    }
                } else {
                    val errorStream = conn.errorStream ?: conn.inputStream
                    val response = BufferedReader(InputStreamReader(errorStream)).readText()
                    withContext(Dispatchers.Main) {
                        isSyncing.value = false
                        onResult(false, "Server returned HTTP $responseCode: $response")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isSyncing.value = false
                    onResult(false, "Fetch error: ${e.localizedMessage}")
                }
            }
        }
    }
}
