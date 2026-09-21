package com.marjuk.eaptracker.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marjuk.eaptracker.LocalHazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import com.marjuk.eaptracker.ui.components.adaptiveGlass

data class SelectedExamInfo(
    val date: String,
    val day: String,
    val examName: String
)

@Composable
fun ExamsScreen(onNavigateToFullExams: (String) -> Unit = {}) {
    var examRoutineType by remember { mutableStateOf("Offline") }
    val examData = com.marjuk.eaptracker.ui.admin.DynamicExamRepository.getExams(examRoutineType)
    val pageSize = 7
    val pageCount = maxOf(1, (examData.size + pageSize - 1) / pageSize)
    val pagerState = rememberPagerState(pageCount = { pageCount })

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "EXAMS",
            style = MaterialTheme.typography.headlineMedium.copy(
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            ),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Offline / Online Toggle (Connected pill style)
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(56.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Offline", "Online").forEach { type ->
                val isSelected = examRoutineType == type
                val weight by animateFloatAsState(if (isSelected) 1.5f else 1f, label = "weight")
                val cornerRadius by animateDpAsState(if (isSelected) 28.dp else 12.dp, label = "corners")
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
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { examRoutineType = type },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = type.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Exam Pager with Frosted Glass Effect
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 16.dp
        ) { pageIndex ->
            val startIndex = pageIndex * pageSize
            val endIndex = minOf(startIndex + pageSize, examData.size)
            val currentWeekExams = examData.subList(startIndex, endIndex)

            val hazeState = LocalHazeState.current
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(28.dp))
                    .adaptiveGlass(
                        hazeState = hazeState,
                        shape = RoundedCornerShape(28.dp),
                        style = HazeStyle(
                            backgroundColor = MaterialTheme.colorScheme.background,
                            tint = HazeTint(Color.Black.copy(alpha = 0.15f)),
                            blurRadius = 20.dp
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    var showSyllabus by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WEEK ${pageIndex + 1}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        val cornerRadius by animateDpAsState(
                            targetValue = if (showSyllabus) 24.dp else 4.dp, 
                            label = "buttonShape"
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(cornerRadius))
                                .background(if (showSyllabus) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f))
                                .clickable { showSyllabus = !showSyllabus }
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Syllabus",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (showSyllabus) MaterialTheme.colorScheme.onPrimary else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                    ) {
                        currentWeekExams.forEachIndexed { index, item ->
                            ExamRow(
                                item = item, 
                                showSyllabus = showSyllabus,
                                onExamClick = { date, day, name ->
                                    selectedExamForMarks = SelectedExamInfo(date, day, name)
                                }
                            )
                            if (index < currentWeekExams.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    thickness = 0.5.dp,
                                    color = Color.White.copy(alpha = 0.15f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Full Exam Schedule Split Button
        var isExpanded by remember { mutableStateOf(false) }
        val rotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "rotate")
        val trailingInnerCorner by animateDpAsState(if (isExpanded) 28.dp else 4.dp, label = "inner")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp, topEnd = 4.dp, bottomEnd = 4.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(modifier = Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Assignment, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "FULL EXAM SCHEDULE",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            letterSpacing = 1.sp
                        )
                    }
                }
                Box(modifier = Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp, topStart = trailingInnerCorner, bottomStart = trailingInnerCorner))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown, 
                        null, 
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.rotate(rotation)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Row(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf("Offline", "Online").forEachIndexed { index, type ->
                        val shape = when (index) {
                            0 -> RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 4.dp, bottomEnd = 4.dp)
                            else -> RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp, topStart = 4.dp, bottomStart = 4.dp)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(shape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { onNavigateToFullExams(type.lowercase()) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                type.uppercase(),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
fun ExamRow(
    item: ExamItem, 
    showSyllabus: Boolean = false,
    onExamClick: (date: String, day: String, examName: String) -> Unit = { _, _, _ -> }
) {
    val dayAbbr = item.day.take(3)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(72.dp)) {
            Text(
                text = item.date.split("-")[0],
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = "${item.date.split("-")[1]} . $dayAbbr",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Crossfade(targetState = showSyllabus, label = "exam_syllabus_crossfade") { show ->
                if (show) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Exam Syllabus",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (!item.syllabus.isNullOrEmpty()) {
                            item.syllabus.forEach { s ->
                                Text(
                                    text = s,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    ),
                                    color = Color.White.copy(alpha = 0.92f),
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        } else {
                            Text(
                                text = "Standard lecture syllabus",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (item.exams.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF81C784).copy(alpha = 0.15f))
                                    .border(0.5.dp, Color(0xFF81C784).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Self Study & Revision",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = Color(0xFF81C784),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        } else {
                            item.exams.forEach { examText ->
                            val key = ExamMarksRepository.makeKey(item.date, examText)
                            val score = ExamMarksRepository.scoresState[key]
                            val isWeeklyOrRevision = examText.contains("Weekly", ignoreCase = true) || 
                                                     examText.contains("Revision", ignoreCase = true) ||
                                                     examText.contains("Monthly", ignoreCase = true)

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (score != null) Color.White.copy(alpha = 0.08f) else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onExamClick(item.date, item.day, examText) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp, horizontal = if (score != null) 6.dp else 0.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = examText,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 13.sp,
                                            lineHeight = 17.sp
                                        ),
                                        fontWeight = if (isWeeklyOrRevision) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isWeeklyOrRevision) MaterialTheme.colorScheme.secondary else Color.White,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    if (score != null) {
                                        val benchmark = SettingsRepository.benchmarkPassPercentage.intValue.toFloat()
                                        val isTargetMet = score.percentage >= benchmark

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (isTargetMet) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                                                    border = BorderStroke(0.5.dp, Color(0xFF4CAF50).copy(alpha = 0.5f)),
                                                    modifier = Modifier.padding(end = 6.dp)
                                                ) {
                                                    Text(
                                                        text = "🎯 ${score.percentage.toInt()}%",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color(0xFF81C784),
                                                            fontSize = 10.sp
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            } else {
                                                val diff = (benchmark - score.percentage).toInt()
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFFFF9800).copy(alpha = 0.18f),
                                                    border = BorderStroke(0.5.dp, Color(0xFFFF9800).copy(alpha = 0.4f)),
                                                    modifier = Modifier.padding(end = 6.dp)
                                                ) {
                                                    Text(
                                                        text = "⚠️ -${diff}%",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFFFB74D),
                                                            fontSize = 10.sp
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color.White.copy(alpha = 0.08f),
                                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
                                            ) {
                                                Text(
                                                    text = "${formatScore(score.totalObtained)}/${formatScore(score.totalMax)}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                        ) {
                                            Text(
                                                text = "+ Mark",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
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

private fun formatScore(value: Float): String {
    return if (value % 1f == 0f) {
        value.toInt().toString()
    } else {
        String.format("%.1f", value)
    }
}
