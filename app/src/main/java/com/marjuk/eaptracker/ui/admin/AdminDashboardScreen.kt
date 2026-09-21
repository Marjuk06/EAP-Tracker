package com.marjuk.eaptracker.ui.admin

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marjuk.eaptracker.LocalHazeState
import com.marjuk.eaptracker.ui.*
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.launch

@Composable
fun AdminDashboardScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val hazeState = LocalHazeState.current

    var activeTab by remember { mutableStateOf("Routines") }
    var routineMode by remember { mutableStateOf("Offline") }
    var examMode by remember { mutableStateOf("Offline") }

    // Dialog States
    var routineItemToEdit by remember { mutableStateOf<Pair<Int, RoutineItem>?>(null) }
    var isAddingRoutineDay by remember { mutableStateOf(false) }

    var examItemToEdit by remember { mutableStateOf<Pair<Int, ExamItem>?>(null) }
    var isAddingExamDay by remember { mutableStateOf(false) }

    var isAddingSubject by remember { mutableStateOf(false) }
    var newSubjectName by remember { mutableStateOf("") }

    var chapterAddTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var newChapterName by remember { mutableStateOf("") }

    // API Config State
    var endpointInput by remember { mutableStateOf(RemoteSyncManager.apiEndpointUrl.value) }
    var apiKeyInput by remember { mutableStateOf(RemoteSyncManager.webApiKey.value) }

    val tabs = listOf("Routines", "Exams", "Syllabus", "Cloud")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "ADMIN CONSOLE",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "Dynamic Schedule & Syllabus Management",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Sync Status Indicator Pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CloudSync,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = RemoteSyncManager.lastSyncTime.value,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        // Top Navigation Connected Morphing Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            tabs.forEach { tab ->
                val isSelected = activeTab == tab
                val weight by animateFloatAsState(if (isSelected) 1.4f else 1f, label = "tabWeight")
                val cornerRadius by animateDpAsState(if (isSelected) 24.dp else 8.dp, label = "tabCorners")
                val bgColor by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    label = "tabBg"
                )
                val contentColor by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else Color.White.copy(alpha = 0.7f),
                    label = "tabContent"
                )

                Box(
                    modifier = Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(bgColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { activeTab = tab },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = contentColor,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Contents
        when (activeTab) {
            // TAB 1: ROUTINES MANAGER
            "Routines" -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Offline", "Online").forEach { mode ->
                                val sel = routineMode == mode
                                FilterChip(
                                    selected = sel,
                                    onClick = { routineMode = mode },
                                    label = { Text("$mode Routine") },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }

                        Button(
                            onClick = { isAddingRoutineDay = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Day", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val routines = if (routineMode == "Offline") DynamicRoutineRepository.offlineRoutines
                    else DynamicRoutineRepository.onlineRoutines

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        itemsIndexed(routines) { index, item ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0xFF1D1B20).copy(alpha = 0.7f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${item.date} (${item.day})",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )

                                        item.classSubject?.let {
                                            Text(
                                                text = "Class: $it",
                                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.SemiBold)
                                            )
                                        }

                                        item.examDetails?.let { ex ->
                                            val exText = when (ex) {
                                                is String -> ex
                                                is List<*> -> ex.joinToString(", ")
                                                else -> ex.toString()
                                            }
                                            Text(
                                                text = "Exam: $exText",
                                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.secondary)
                                            )
                                        }

                                        val topicsList = item.topics
                                        if (topicsList != null && topicsList.isNotEmpty()) {
                                            Text(
                                                text = "Topics (${topicsList.size} parts): ${topicsList.firstOrNull()?.take(40)}...",
                                                style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.6f))
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = { routineItemToEdit = Pair(index, item) }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                                        }
                                        IconButton(onClick = {
                                            DynamicRoutineRepository.deleteRoutineItem(routineMode, index)
                                            Toast.makeText(context, "Deleted day", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF5350))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Reset Option
                    TextButton(
                        onClick = {
                            DynamicRoutineRepository.resetToDefault(routineMode)
                            Toast.makeText(context, "Reset $routineMode routine to defaults", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset $routineMode to Factory Defaults", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                }
            }

            // TAB 2: EXAMS MANAGER
            "Exams" -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Offline", "Online").forEach { mode ->
                                val sel = examMode == mode
                                FilterChip(
                                    selected = sel,
                                    onClick = { examMode = mode },
                                    label = { Text("$mode Exams") },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }

                        Button(
                            onClick = { isAddingExamDay = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Exam", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val exams = if (examMode == "Offline") DynamicExamRepository.offlineExamList
                    else DynamicExamRepository.onlineExamList

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        itemsIndexed(exams) { index, item ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0xFF1D1B20).copy(alpha = 0.7f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${item.date} (${item.day})",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )

                                        if (item.exams.isNotEmpty()) {
                                            item.exams.forEach { examName ->
                                                Text(
                                                    text = "• $examName",
                                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.SemiBold)
                                                )
                                            }
                                        }

                                        val syl = item.syllabus
                                        if (syl != null && syl.isNotEmpty()) {
                                            Text(
                                                text = "Syllabus: ${syl.firstOrNull()?.take(40)}...",
                                                style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.6f))
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = { examItemToEdit = Pair(index, item) }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                                        }
                                        IconButton(onClick = {
                                            DynamicExamRepository.deleteExamItem(examMode, index)
                                            Toast.makeText(context, "Deleted exam", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF5350))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            DynamicExamRepository.resetToDefault(examMode)
                            Toast.makeText(context, "Reset $examMode exams to defaults", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset $examMode to Factory Defaults", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                }
            }

            // TAB 3: SYLLABUS MANAGER
            "Syllabus" -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Subjects & Chapters (${SyllabusRepository.subjectsState.size})",
                            style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )

                        Button(
                            onClick = { isAddingSubject = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Subject", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        itemsIndexed(SyllabusRepository.subjectsState) { subIdx, subject ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF1D1B20).copy(alpha = 0.7f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(subject.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = subject.name,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = Color.White)
                                            )
                                        }

                                        Text(
                                            text = "${subject.completedChapters}/${subject.totalChapters} Chaps",
                                            style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary)
                                        )
                                    }

                                    // Papers & Chapters
                                    subject.papers.forEach { paper ->
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color.White.copy(alpha = 0.04f),
                                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "${paper.name} (${paper.chapters.size} Chapters)",
                                                        style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                                    )

                                                    IconButton(
                                                        onClick = { chapterAddTarget = Pair(subject.name, paper.name) },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Add Chapter", tint = MaterialTheme.colorScheme.primary)
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                paper.chapters.forEachIndexed { chapIdx, chapter ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 3.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "${chapIdx + 1}. ${chapter.name}",
                                                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.85f)),
                                                            modifier = Modifier.weight(1f)
                                                        )

                                                        IconButton(
                                                            onClick = {
                                                                DynamicSyllabusRepository.deleteChapter(subject.name, paper.name, chapIdx)
                                                            },
                                                            modifier = Modifier.size(20.dp)
                                                        ) {
                                                            Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color(0xFFEF5350), modifier = Modifier.size(14.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TAB 4: CLOUD SYNC & WEB API HUB
            "Cloud" -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF1D1B20).copy(alpha = 0.75f))
                            .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(22.dp))
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Web API & Cloud Configuration",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                )
                            }

                            Text(
                                text = "Connect to your Web API endpoint (e.g. custom REST API or JSONBin) using your Web API Key to broadcast routine, exam, and syllabus updates directly to all student apps.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.7f))
                            )

                            OutlinedTextField(
                                value = endpointInput,
                                onValueChange = { endpointInput = it },
                                label = { Text("Web API Endpoint URL") },
                                placeholder = { Text("https://api.jsonbin.io/v3/b/YOUR_BIN_ID") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                )
                            )

                            OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = { apiKeyInput = it },
                                label = { Text("Web API Key / Secret Token") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                )
                            )

                            Button(
                                onClick = {
                                    RemoteSyncManager.saveConfig(endpointInput.trim(), apiKeyInput.trim())
                                    Toast.makeText(context, "API credentials saved", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.align(Alignment.End),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Save Credentials")
                            }
                        }
                    }

                    // Publish & Fetch Action Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF1D1B20).copy(alpha = 0.75f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Live Broadcast Actions",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            RemoteSyncManager.publishToCloud(context) { success, message ->
                                                Toast.makeText(context, message, if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    enabled = !RemoteSyncManager.isSyncing.value,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    if (RemoteSyncManager.isSyncing.value) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Publishing...")
                                    } else {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Publish to Cloud", fontWeight = FontWeight.Bold)
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            RemoteSyncManager.fetchFromCloud(context) { success, message ->
                                                Toast.makeText(context, message, if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    enabled = !RemoteSyncManager.isSyncing.value,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Fetch Cloud Data")
                                }
                            }
                        }
                    }

                    // Master Export / Copy Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF1D1B20).copy(alpha = 0.75f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Master JSON Payload Export",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                            )

                            Button(
                                onClick = {
                                    val masterJson = RemoteSyncManager.generateMasterPayload()
                                    clipboardManager.setText(AnnotatedString(masterJson))
                                    Toast.makeText(context, "Copied master payload JSON to clipboard!", Toast.LENGTH_LONG).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Copy Master Payload JSON")
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (isAddingRoutineDay) {
        RoutineEditorDialog(
            initialItem = null,
            onSave = { newItem ->
                DynamicRoutineRepository.addRoutineItem(routineMode, newItem)
                isAddingRoutineDay = false
                Toast.makeText(context, "Added new routine day", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { isAddingRoutineDay = false }
        )
    }

    routineItemToEdit?.let { (idx, item) ->
        RoutineEditorDialog(
            initialItem = item,
            onSave = { updated ->
                DynamicRoutineRepository.updateRoutineItem(routineMode, idx, updated)
                routineItemToEdit = null
                Toast.makeText(context, "Updated routine day", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { routineItemToEdit = null }
        )
    }

    if (isAddingExamDay) {
        ExamEditorDialog(
            initialItem = null,
            onSave = { newItem ->
                DynamicExamRepository.addExamItem(examMode, newItem)
                isAddingExamDay = false
                Toast.makeText(context, "Added new exam schedule", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { isAddingExamDay = false }
        )
    }

    examItemToEdit?.let { (idx, item) ->
        ExamEditorDialog(
            initialItem = item,
            onSave = { updated ->
                DynamicExamRepository.updateExamItem(examMode, idx, updated)
                examItemToEdit = null
                Toast.makeText(context, "Updated exam schedule", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { examItemToEdit = null }
        )
    }

    // Add Subject Dialog
    if (isAddingSubject) {
        AlertDialog(
            onDismissRequest = { isAddingSubject = false },
            title = { Text("Add New Subject") },
            text = {
                OutlinedTextField(
                    value = newSubjectName,
                    onValueChange = { newSubjectName = it },
                    label = { Text("Subject Name (e.g. ICT)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newSubjectName.isNotBlank()) {
                        DynamicSyllabusRepository.addSubject(newSubjectName.trim())
                        newSubjectName = ""
                        isAddingSubject = false
                        Toast.makeText(context, "Subject added", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddingSubject = false }) { Text("Cancel") }
            }
        )
    }

    // Add Chapter Dialog
    chapterAddTarget?.let { (subName, papName) ->
        AlertDialog(
            onDismissRequest = { chapterAddTarget = null },
            title = { Text("Add Chapter to $subName ($papName)") },
            text = {
                OutlinedTextField(
                    value = newChapterName,
                    onValueChange = { newChapterName = it },
                    label = { Text("Chapter Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newChapterName.isNotBlank()) {
                        DynamicSyllabusRepository.addChapter(subName, papName, newChapterName.trim())
                        newChapterName = ""
                        chapterAddTarget = null
                        Toast.makeText(context, "Chapter added", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { chapterAddTarget = null }) { Text("Cancel") }
            }
        )
    }
}
