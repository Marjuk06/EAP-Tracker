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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun HomeScreen(
    onNavigateToFullRoutine: (String) -> Unit = {},
    onNavigateToExams: () -> Unit = {},
    onNavigateToStudyStats: () -> Unit = {},
    onOpenFullScreenTimer: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    var routineType by remember { mutableStateOf("Offline") }
    
    val routineData = com.marjuk.eaptracker.ui.admin.DynamicRoutineRepository.getRoutines(routineType)
    val pageSize = 7
    val pageCount = maxOf(1, (routineData.size + pageSize - 1) / pageSize)
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header Title (Exact MaterialTheme.typography.headlineMedium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "HOME",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Daily Study Goal & Live Focus Card
        DailyStudyGoalCard(
            hazeState = LocalHazeState.current,
            modifier = Modifier.padding(horizontal = 16.dp),
            onNavigateToStats = onNavigateToStudyStats,
            onOpenFullScreenTimer = onOpenFullScreenTimer
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Offline / Online Toggle (Connected style)
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(56.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Offline", "Online").forEach { type ->
                val isSelected = routineType == type
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
                        ) { routineType = type },
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

        // Routine Pager (Frosted Glass Effect)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 16.dp
        ) { pageIndex ->
            val startIndex = pageIndex * pageSize
            val endIndex = minOf(startIndex + pageSize, routineData.size)
            val currentWeek = routineData.subList(startIndex, endIndex)

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
                    var showTopics by remember { mutableStateOf(false) }
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
                        val cornerRadius by animateDpAsState(targetValue = if (showTopics) 24.dp else 4.dp, label = "buttonShape")
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(cornerRadius))
                                .background(if (showTopics) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f))
                                .clickable { showTopics = !showTopics }
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Topics",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (showTopics) MaterialTheme.colorScheme.onPrimary else Color.White,
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
                        currentWeek.forEachIndexed { index, item ->
                            RoutineRow(
                                item = item, 
                                showTopics = showTopics,
                                onExamClick = { _, _, _ ->
                                    HapticHelper.performHaptic(context, HapticType.LIGHT)
                                    onNavigateToExams()
                                }
                            )
                            if (index < currentWeek.lastIndex) {
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

        // Full Routine Split Button (Expandable)
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
                            "FULL ROUTINE",
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
                                .clickable { onNavigateToFullRoutine(type.lowercase()) },
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
fun RoutineRow(
    item: RoutineItem, 
    showTopics: Boolean = false,
    onExamClick: (date: String, day: String, examName: String) -> Unit = { _, _, _ -> }
) {
    val dayAbbr = item.day.take(3)
    val hasNoClass = item.classSubject.isNullOrBlank() || item.classSubject.equals("Off", ignoreCase = true) || item.classSubject.equals("Off Day", ignoreCase = true)
    val hasNoExams = item.examDetails == null || (item.examDetails is String && (item.examDetails.isBlank() || item.examDetails.equals("None", ignoreCase = true) || item.examDetails.equals("Off Day", ignoreCase = true))) || (item.examDetails is List<*> && item.examDetails.isEmpty())
    val isSelfStudyDay = hasNoClass && hasNoExams

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
            if (isSelfStudyDay) {
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
                Crossfade(targetState = showTopics, label = "topics_crossfade") { show ->
                    if (show) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            item.classSubject?.let {
                                Text(
                                    it, 
                                    style = MaterialTheme.typography.bodyMedium, 
                                    fontWeight = FontWeight.Bold, 
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (!item.topics.isNullOrEmpty()) {
                                item.topics.forEach { topic ->
                                    Text(
                                        text = topic,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp
                                        ),
                                        color = Color.White.copy(alpha = 0.92f),
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            } else {
                                item.examDetails?.let { details ->
                                    val list = when (details) {
                                        is String -> listOf(details)
                                        is List<*> -> details.filterIsInstance<String>()
                                        else -> emptyList()
                                    }
                                    list.forEach { examText ->
                                        Text(
                                            examText, 
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            item.classSubject?.let {
                                Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            item.examDetails?.let { details ->
                                val examList = when (details) {
                                    is String -> listOf(details)
                                    is List<*> -> details.filterIsInstance<String>()
                                    else -> emptyList()
                                }
                                examList.forEach { examText ->
                                    val key = ExamMarksRepository.makeKey(item.date, examText)
                                    val score = ExamMarksRepository.scoresState[key]

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (score != null) Color.White.copy(alpha = 0.08f) else Color.Transparent,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onExamClick(item.date, item.day, examText) }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp, horizontal = if (score != null) 4.dp else 0.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = examText, 
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                lineHeight = 16.sp,
                                                modifier = Modifier.weight(1f)
                                            )

                                        Spacer(modifier = Modifier.width(6.dp))

                                        if (score != null) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = when {
                                                    score.percentage >= 80f -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                                    score.percentage >= 60f -> Color(0xFF2196F3).copy(alpha = 0.2f)
                                                    score.percentage >= 40f -> Color(0xFFFF9800).copy(alpha = 0.2f)
                                                    else -> Color(0xFFF44336).copy(alpha = 0.2f)
                                                }
                                            ) {
                                                val obtainedStr = if (score.totalObtained % 1f == 0f) score.totalObtained.toInt().toString() else score.totalObtained.toString()
                                                val maxStr = if (score.totalMax % 1f == 0f) score.totalMax.toInt().toString() else score.totalMax.toString()
                                                Text(
                                                    text = "$obtainedStr/$maxStr",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                    color = when {
                                                        score.percentage >= 80f -> Color(0xFF81C784)
                                                        score.percentage >= 60f -> Color(0xFF64B5F6)
                                                        score.percentage >= 40f -> Color(0xFFFFB74D)
                                                        else -> Color(0xFFE57373)
                                                    },
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
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
}
