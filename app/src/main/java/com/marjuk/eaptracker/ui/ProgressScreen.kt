package com.marjuk.eaptracker.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marjuk.eaptracker.LocalHazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import com.marjuk.eaptracker.ui.components.adaptiveGlass

@Composable
fun ProgressScreen(
    onNavigateToStudyStats: () -> Unit = {}
) {
    val hazeState = LocalHazeState.current

    // Gather live syllabus metrics from SyllabusRepository
    val (completedChaptersTotal, totalChaptersTotal) = SyllabusRepository.getTotalChaptersStats()
    val syllabusPercentage = if (totalChaptersTotal > 0) (completedChaptersTotal.toFloat() / totalChaptersTotal) * 100f else 0f

    val (physicsCompleted, physicsTotal) = SyllabusRepository.getSubjectProgress("Physics")
    val (chemCompleted, chemTotal) = SyllabusRepository.getSubjectProgress("Chemistry")
    val (mathCompleted, mathTotal) = SyllabusRepository.getSubjectProgress("Higher Math")
    val (bioCompleted, bioTotal) = SyllabusRepository.getSubjectProgress("Biology")

    // Gather live exam metrics from repositories
    val (totalExamsCount, totalObtainedMarks, totalMaxMarks) = ProgressRepository.getOverallExamStats()
    val overallExamAvg = if (totalMaxMarks > 0f) (totalObtainedMarks / totalMaxMarks) * 100f else 0f

    val physicsStats = ProgressRepository.getSubjectStats("P", "Physics")
    val chemistryStats = ProgressRepository.getSubjectStats("C", "Chemistry")
    val mathStats = ProgressRepository.getSubjectStats("M", "Higher Math")
    val bioStats = ProgressRepository.getSubjectStats("Bio", "Biology")

    var selectedExamForMarks by remember { mutableStateOf<SelectedExamInfo?>(null) }

    if (selectedExamForMarks != null) {
        val info = selectedExamForMarks!!
        ExamMarksDialog(
            date = info.date,
            day = info.day,
            examName = info.examName,
            onDismiss = { selectedExamForMarks = null }
        )
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    var routineType by remember { mutableStateOf(SettingsRepository.defaultRoutineMode.value) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Title
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PROGRESS",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                )
            }
        }

        // Offline / Online Routine Toggle (Connected Pill Design Outside Card)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Offline", "Online").forEach { type ->
                    val isSelected = routineType == type
                    val weight by animateFloatAsState(if (isSelected) 1.5f else 1f, label = "weight")
                    val cornerRadius by animateDpAsState(if (isSelected) 26.dp else 12.dp, label = "corners")
                    val bgColor by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), 
                        label = "bg"
                    )
                    val contentColor by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.onPrimary 
                        else Color.White.copy(alpha = 0.7f), 
                        label = "content"
                    )

                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(cornerRadius))
                            .background(bgColor)
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) {
                                HapticHelper.performHaptic(context, HapticType.SELECTION)
                                routineType = type
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type.uppercase(),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = contentColor,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // 1. Performance Analytics Interactive Graph Card
        item {
            PerformanceGraphCard(
                hazeState = hazeState,
                routineType = routineType,
                onExamClick = { date, day, name ->
                    selectedExamForMarks = SelectedExamInfo(date, day, name)
                }
            )
        }

        // 2. Weekly Study Time & Consistency Analytics (Clickable -> Full YPT Statistics & Calendar)
        item {
            WeeklyStudyAnalyticsCard(
                hazeState = hazeState,
                onOpenStatistics = onNavigateToStudyStats
            )
        }

        // 3. Overview Performance Cards (Syllabus & Exam Mastery)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Syllabus Mastery Card
                OverviewMetricCard(
                    title = "Syllabus",
                    value = "${syllabusPercentage.toInt()}%",
                    subtitle = "$completedChaptersTotal / $totalChaptersTotal Chapters",
                    icon = Icons.Default.CheckCircleOutline,
                    accentColor = MaterialTheme.colorScheme.primary,
                    progress = if (totalChaptersTotal > 0) completedChaptersTotal.toFloat() / totalChaptersTotal else 0f,
                    modifier = Modifier.weight(1f),
                    hazeState = hazeState
                )

                // Exam Performance Card
                OverviewMetricCard(
                    title = "Exam Average",
                    value = if (totalExamsCount > 0) "${overallExamAvg.toInt()}%" else "N/A",
                    subtitle = if (totalExamsCount > 0) "$totalExamsCount Exams Logged" else "No exams yet",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    accentColor = Color(0xFF4CAF50),
                    progress = if (totalMaxMarks > 0f) totalObtainedMarks / totalMaxMarks else null,
                    modifier = Modifier.weight(1f),
                    hazeState = hazeState
                )
            }
        }

        // 3. Subject-Wise Progress Breakdown
        item {
            Text(
                text = "SUBJECT BREAKDOWN",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f),
                    letterSpacing = 1.sp
                ),
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SubjectProgressCard(
                    subjectName = "Physics",
                    icon = Icons.Default.Speed,
                    chaptersCompleted = physicsCompleted,
                    totalChapters = physicsTotal,
                    examStats = physicsStats,
                    gradientColors = listOf(Color(0xFF1E88E5), Color(0xFF1565C0)),
                    hazeState = hazeState
                )

                SubjectProgressCard(
                    subjectName = "Chemistry",
                    icon = Icons.Default.Science,
                    chaptersCompleted = chemCompleted,
                    totalChapters = chemTotal,
                    examStats = chemistryStats,
                    gradientColors = listOf(Color(0xFFE53935), Color(0xFFC62828)),
                    hazeState = hazeState
                )

                SubjectProgressCard(
                    subjectName = "Higher Mathematics",
                    icon = Icons.Default.Calculate,
                    chaptersCompleted = mathCompleted,
                    totalChapters = mathTotal,
                    examStats = mathStats,
                    gradientColors = listOf(Color(0xFFFB8C00), Color(0xFFEF6C00)),
                    hazeState = hazeState
                )

                SubjectProgressCard(
                    subjectName = "Biology",
                    icon = Icons.Default.Spa,
                    chaptersCompleted = bioCompleted,
                    totalChapters = bioTotal,
                    examStats = bioStats,
                    gradientColors = listOf(Color(0xFF43A047), Color(0xFF2E7D32)),
                    hazeState = hazeState
                )
            }
        }

        // 4. Recent Activity Log
        item {
            Text(
                text = "RECENT EXAM ACTIVITY",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f),
                    letterSpacing = 1.sp
                ),
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }

        val recordedScores = ExamMarksRepository.scoresState.entries.toList()
        if (recordedScores.isNotEmpty()) {
            items(recordedScores) { (key, score) ->
                val (date, name) = ExamMarksRepository.parseKey(key)
                RecentActivityRow(
                    date = date,
                    name = name,
                    score = score,
                    hazeState = hazeState,
                    onClick = {
                        selectedExamForMarks = SelectedExamInfo(date, "", name)
                    }
                )
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .adaptiveGlass(
                            hazeState = hazeState,
                            shape = RoundedCornerShape(20.dp),
                            style = HazeStyle(
                                backgroundColor = MaterialTheme.colorScheme.background,
                                tint = HazeTint(Color.Black.copy(alpha = 0.2f)),
                                blurRadius = 20.dp
                            )
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AssignmentLate,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No exam marks submitted yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Tap on any exam in the Exams tab to add your scores",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OverviewMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    progress: Float? = null,
    modifier: Modifier = Modifier,
    hazeState: dev.chrisbanes.haze.HazeState
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .adaptiveGlass(
                hazeState = hazeState,
                shape = RoundedCornerShape(24.dp),
                style = HazeStyle(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    tint = HazeTint(Color.Black.copy(alpha = 0.2f)),
                    blurRadius = 20.dp
                )
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = Color.White.copy(alpha = 0.6f)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.55f)
            )

            if (progress != null) {
                Spacer(modifier = Modifier.height(10.dp))
                SquigglyProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                    color = accentColor,
                    trackColor = Color.White.copy(alpha = 0.1f),
                    strokeWidth = 2.5f,
                    waveAmplitudeDp = 1.5f,
                    waveLengthDp = 14f
                )
            }
        }
    }
}

@Composable
fun SubjectProgressCard(
    subjectName: String,
    icon: ImageVector,
    chaptersCompleted: Int,
    totalChapters: Int,
    examStats: SubjectExamStats,
    gradientColors: List<Color>,
    hazeState: dev.chrisbanes.haze.HazeState
) {
    val progress = if (totalChapters > 0) chaptersCompleted.toFloat() / totalChapters else 0f
    val percentage = (progress * 100f).toInt()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .adaptiveGlass(
                hazeState = hazeState,
                shape = RoundedCornerShape(20.dp),
                style = HazeStyle(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    tint = HazeTint(Color.Black.copy(alpha = 0.18f)),
                    blurRadius = 20.dp
                )
            )
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(gradientColors)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = subjectName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                if (examStats.examsCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = gradientColors.first().copy(alpha = 0.2f),
                        border = BorderStroke(0.5.dp, gradientColors.first().copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Exam Avg: ${examStats.averagePercentage.toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stats Subtitle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Chapters: $chaptersCompleted / $totalChapters",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Squiggly Wavy Progress Indicator
            SquigglyProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
                color = gradientColors.first(),
                trackColor = Color.White.copy(alpha = 0.1f),
                strokeWidth = 3f,
                waveAmplitudeDp = 2f,
                waveLengthDp = 16f
            )
        }
    }
}

@Composable
fun RecentActivityRow(
    date: String,
    name: String,
    score: ExamScore,
    hazeState: dev.chrisbanes.haze.HazeState,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .adaptiveGlass(
                hazeState = hazeState,
                shape = RoundedCornerShape(16.dp),
                style = HazeStyle(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    tint = HazeTint(Color.Black.copy(alpha = 0.15f)),
                    blurRadius = 16.dp
                ),
                borderColor = Color.White.copy(alpha = 0.1f)
            )
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = date,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = when {
                    score.percentage >= 80f -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                    score.percentage >= 60f -> Color(0xFF2196F3).copy(alpha = 0.2f)
                    score.percentage >= 40f -> Color(0xFFFF9800).copy(alpha = 0.2f)
                    else -> Color(0xFFF44336).copy(alpha = 0.2f)
                },
                border = BorderStroke(
                    0.5.dp,
                    when {
                        score.percentage >= 80f -> Color(0xFF4CAF50).copy(alpha = 0.5f)
                        score.percentage >= 60f -> Color(0xFF2196F3).copy(alpha = 0.5f)
                        score.percentage >= 40f -> Color(0xFFFF9800).copy(alpha = 0.5f)
                        else -> Color(0xFFF44336).copy(alpha = 0.5f)
                    }
                )
            ) {
                Text(
                    text = "${formatScore(score.totalObtained)} / ${formatScore(score.totalMax)}  (${score.percentage.toInt()}%)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = when {
                        score.percentage >= 80f -> Color(0xFF81C784)
                        score.percentage >= 60f -> Color(0xFF64B5F6)
                        score.percentage >= 40f -> Color(0xFFFFB74D)
                        else -> Color(0xFFE57373)
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

private fun formatScore(value: Float): String {
    return if (value % 1f == 0f) {
        value.toInt().toString()
    } else {
        String.format("%.1f", value)
    }
}
