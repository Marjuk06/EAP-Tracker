package com.marjuk.eaptracker.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.marjuk.eaptracker.LocalHazeState
import com.marjuk.eaptracker.ui.RoutineItem
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

@Composable
fun RoutineEditorDialog(
    initialItem: RoutineItem?,
    onSave: (RoutineItem) -> Unit,
    onDismiss: () -> Unit
) {
    var date by remember { mutableStateOf(initialItem?.date ?: "") }
    var day by remember { mutableStateOf(initialItem?.day ?: "Saturday") }
    var classSubject by remember { mutableStateOf(initialItem?.classSubject ?: "") }
    
    val initialExamStr = when (val ex = initialItem?.examDetails) {
        is String -> ex
        is List<*> -> ex.joinToString(", ")
        else -> ""
    }
    var examDetails by remember { mutableStateOf(initialExamStr) }

    val topics = remember {
        mutableStateListOf<String>().apply {
            val initialTopics = initialItem?.topics
            if (initialTopics != null && initialTopics.isNotEmpty()) {
                addAll(initialTopics)
            } else {
                add("")
            }
        }
    }

    val hazeState = LocalHazeState.current

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp))
                .hazeChild(
                    state = hazeState,
                    shape = RoundedCornerShape(28.dp),
                    style = HazeStyle(
                        backgroundColor = MaterialTheme.colorScheme.background,
                        tint = HazeTint(Color.Black.copy(alpha = 0.35f)),
                        blurRadius = 24.dp
                    )
                )
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    RoundedCornerShape(28.dp)
                )
                .background(Color(0xFF1D1B20).copy(alpha = 0.85f))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialItem != null) "EDIT ROUTINE DAY" else "ADD ROUTINE DAY",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Date & Day
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date (e.g. 22-Aug-26)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        )
                    )

                    OutlinedTextField(
                        value = day,
                        onValueChange = { day = it },
                        label = { Text("Day Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }

                // Class Subject
                OutlinedTextField(
                    value = classSubject,
                    onValueChange = { classSubject = it },
                    label = { Text("Class Subject (e.g. Physics (P-01))") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    )
                )

                // Exam Details
                OutlinedTextField(
                    value = examDetails,
                    onValueChange = { examDetails = it },
                    label = { Text("Exam Name (e.g. P-01 MCQ + Written)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    )
                )

                // Topics Dynamic List
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Topics / Syllabus Parts",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        TextButton(
                            onClick = { topics.add("") },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Part")
                        }
                    }

                    topics.forEachIndexed { index, topic ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val partLabel = "Part-${String.format("%02d", index + 1)}"
                            OutlinedTextField(
                                value = topic,
                                onValueChange = { topics[index] = it },
                                label = { Text(partLabel) },
                                modifier = Modifier.weight(1f),
                                maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                )
                            )

                            if (topics.size > 1) {
                                IconButton(
                                    onClick = { topics.removeAt(index) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        tint = Color(0xFFEF5350),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val cleanTopics = topics.map { it.trim() }.filter { it.isNotBlank() }
                            val subVal = classSubject.trim().ifBlank { null }
                            val exVal: Any? = examDetails.trim().ifBlank { null }
                            val item = RoutineItem(
                                date = date.trim(),
                                day = day.trim(),
                                classSubject = subVal,
                                examDetails = exVal,
                                topics = cleanTopics.ifEmpty { null }
                            )
                            onSave(item)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Save Day", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
