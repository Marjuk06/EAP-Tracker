package com.marjuk.eaptracker.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.marjuk.eaptracker.LocalHazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

@Composable
fun ExamMarksDialog(
    date: String,
    day: String,
    examName: String,
    onDismiss: () -> Unit
) {
    val key = remember(date, examName) { ExamMarksRepository.makeKey(date, examName) }
    val existingScore = ExamMarksRepository.scoresState[key]
    val (defaultMaxMcq, defaultMaxWritten) = remember(examName) {
        ExamMarksRepository.parseExamStructure(examName)
    }

    val maxMcq = existingScore?.maxMcq?.takeIf { it > 0f } ?: defaultMaxMcq
    val maxWritten = existingScore?.maxWritten?.takeIf { it > 0f } ?: defaultMaxWritten

    var mcqScore by remember { mutableStateOf(existingScore?.mcqObtained ?: 0f) }
    var writtenScore by remember { mutableStateOf(existingScore?.writtenObtained ?: 0f) }

    var mcqText by remember { mutableStateOf(if (existingScore != null) formatScore(existingScore.mcqObtained) else "0") }
    var writtenText by remember { mutableStateOf(if (existingScore != null) formatScore(existingScore.writtenObtained) else "0") }

    val totalObtained = (if (maxMcq > 0) mcqScore else 0f) + (if (maxWritten > 0) writtenScore else 0f)
    val totalMax = (if (maxMcq > 0) maxMcq else 0f) + (if (maxWritten > 0) maxWritten else 0f)
    val percentage = if (totalMax > 0f) (totalObtained / totalMax) * 100f else 0f

    val progressAnim by animateFloatAsState(
        targetValue = if (totalMax > 0f) (totalObtained / totalMax).coerceIn(0f, 1f) else 0f,
        label = "progress"
    )

    val gradeColor by animateColorAsState(
        targetValue = when {
            percentage >= 80f -> Color(0xFF4CAF50) // Green
            percentage >= 60f -> Color(0xFF2196F3) // Blue
            percentage >= 40f -> Color(0xFFFF9800) // Orange
            else -> Color(0xFFF44336) // Red
        },
        label = "gradeColor"
    )

    val hazeState = LocalHazeState.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
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
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar with Close Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "$date • $day",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Exam Title
                Text(
                    text = examName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Score Visualizer Frosted Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = formatScore(totalObtained),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = " / ${formatScore(totalMax)}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = gradeColor.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, gradeColor.copy(alpha = 0.4f)),
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Text(
                                    text = "${percentage.toInt()}%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = gradeColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        SquigglyProgressIndicator(
                            progress = progressAnim,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp),
                            color = gradeColor,
                            trackColor = Color.White.copy(alpha = 0.15f),
                            strokeWidth = 3.5f,
                            waveAmplitudeDp = 1.6f,
                            waveLengthDp = 12f
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // MCQ Input Section (if applicable)
                if (maxMcq > 0f) {
                    MarksInputRow(
                        label = "MCQ Score",
                        maxMarks = maxMcq,
                        currentScore = mcqScore,
                        textValue = mcqText,
                        onScoreChanged = { newScore ->
                            mcqScore = newScore.coerceIn(0f, maxMcq)
                            mcqText = formatScore(mcqScore)
                        },
                        onTextChanged = { newText ->
                            mcqText = newText
                            val parsed = newText.toFloatOrNull()
                            if (parsed != null) {
                                mcqScore = parsed.coerceIn(0f, maxMcq)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Written Input Section (if applicable)
                if (maxWritten > 0f) {
                    MarksInputRow(
                        label = "Written Score",
                        maxMarks = maxWritten,
                        currentScore = writtenScore,
                        textValue = writtenText,
                        onScoreChanged = { newScore ->
                            writtenScore = newScore.coerceIn(0f, maxWritten)
                            writtenText = formatScore(writtenScore)
                        },
                        onTextChanged = { newText ->
                            writtenText = newText
                            val parsed = newText.toFloatOrNull()
                            if (parsed != null) {
                                writtenScore = parsed.coerceIn(0f, maxWritten)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Action Buttons
                val context = androidx.compose.ui.platform.LocalContext.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (existingScore != null) {
                        OutlinedButton(
                            onClick = {
                                HapticHelper.performHaptic(context, HapticType.STRONG)
                                ExamMarksRepository.clearScore(key)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                        }
                    }

                    Button(
                        onClick = {
                            HapticHelper.performHaptic(context, HapticType.SUCCESS)
                            ExamMarksRepository.saveScore(
                                key = key,
                                mcq = if (maxMcq > 0) mcqScore else 0f,
                                written = if (maxWritten > 0) writtenScore else 0f,
                                maxMcq = maxMcq,
                                maxWritten = maxWritten
                            )
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Marks",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarksInputRow(
    label: String,
    maxMarks: Float,
    currentScore: Float,
    textValue: String,
    onScoreChanged: (Float) -> Unit,
    onTextChanged: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label (Out of ${formatScore(maxMarks)})",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f)
                )
            )

            // Direct Type Input Field
            OutlinedTextField(
                value = textValue,
                onValueChange = onTextChanged,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .width(78.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color.White
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedContainerColor = Color.White.copy(alpha = 0.08f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Interactive Slider
        Slider(
            value = currentScore.coerceIn(0f, maxMarks),
            onValueChange = onScoreChanged,
            valueRange = 0f..maxMarks,
            steps = if (maxMarks <= 50f) (maxMarks * 2).toInt() - 1 else 0,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
            )
        )
    }
}

private fun formatScore(value: Float): String {
    return if (value % 1f == 0f) {
        value.toInt().toString()
    } else {
        String.format("%.1f", value)
    }
}
