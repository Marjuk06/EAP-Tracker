package com.marjuk.eaptracker.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

data class GraphPoint(
    val label: String,
    val date: String,
    val percentage: Float,
    val obtained: Float,
    val max: Float,
    val subject: String,
    val category: String,
    val fullName: String
)

@Composable
fun PerformanceGraphCard(
    hazeState: HazeState,
    routineType: String = "Offline",
    onExamClick: (date: String, day: String, name: String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current

    var selectedCategory by remember { mutableStateOf("All") }
    val examCategories = listOf("All", "Daily", "Weekly", "Finals")

    var selectedSubject by remember { mutableStateOf("All") }
    val subjects = listOf("All", "Physics", "Chemistry", "Math", "Biology")

    fun getExamCategory(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.contains("weekly") || lower.startsWith("w-") || lower.contains(" w-") || lower.contains("week") -> "Weekly"
            lower.contains("monthly") || lower.contains("paper final") || lower.contains("subject final") || 
            lower.contains("final") || lower.contains("mock") || lower.contains("model test") || lower.contains("grand") -> "Finals"
            else -> "Daily"
        }
    }

    val allPoints = remember(ExamMarksRepository.scoresState.size, routineType, selectedCategory, selectedSubject) {
        val points = mutableListOf<GraphPoint>()
        val examList = com.marjuk.eaptracker.ui.admin.DynamicExamRepository.getExams(routineType)
        
        fun processExam(examDate: String, examName: String) {
            val key = ExamMarksRepository.makeKey(examDate, examName)
            val score = ExamMarksRepository.scoresState[key]
            if (score != null) {
                val sub = when {
                    examName.startsWith("P-", ignoreCase = true) || examName.contains("Physics", ignoreCase = true) -> "Physics"
                    examName.startsWith("C-", ignoreCase = true) || examName.contains("Chemistry", ignoreCase = true) -> "Chemistry"
                    examName.startsWith("M-", ignoreCase = true) || examName.contains("Math", ignoreCase = true) -> "Math"
                    examName.startsWith("Bio", ignoreCase = true) || examName.contains("Biology", ignoreCase = true) -> "Biology"
                    else -> "Engineering"
                }
                val cat = getExamCategory(examName)
                val matchesSubject = selectedSubject == "All" || selectedSubject.equals(sub, ignoreCase = true)
                val matchesCategory = selectedCategory == "All" || selectedCategory.equals(cat, ignoreCase = true)

                if (matchesSubject && matchesCategory) {
                    val labelShort = examName.split(" ").take(2).joinToString(" ")
                    if (points.none { it.date == examDate && it.fullName == examName }) {
                        points.add(
                            GraphPoint(
                                label = labelShort,
                                date = examDate,
                                percentage = score.percentage,
                                obtained = score.totalObtained,
                                max = score.totalMax,
                                subject = sub,
                                category = cat,
                                fullName = examName
                            )
                        )
                    }
                }
            }
        }

        examList.forEach { item ->
            item.exams.forEach { name -> processExam(item.date, name) }
        }

        points
    }

    val displayPoints = allPoints

    val primaryGraphColor = when (selectedSubject) {
        "Physics" -> Color(0xFF29B6F6)
        "Chemistry" -> Color(0xFFEF5350)
        "Math" -> Color(0xFFFFA726)
        "Biology" -> Color(0xFF66BB6A)
        else -> Color(0xFF00E5FF) // Cyan for all
    }

    val secondaryGraphColor = when (selectedSubject) {
        "Physics" -> Color(0xFF0D47A1)
        "Chemistry" -> Color(0xFFB71C1C)
        "Math" -> Color(0xFFE65100)
        "Biology" -> Color(0xFF1B5E20)
        else -> Color(0xFF7C4DFF) // Purple
    }

    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    // Quick stats
    val highestScore = if (displayPoints.isNotEmpty()) displayPoints.maxOf { it.percentage } else 0f
    val lowestScore = if (displayPoints.isNotEmpty()) displayPoints.minOf { it.percentage } else 0f
    val avgScore = if (displayPoints.isNotEmpty()) displayPoints.map { it.percentage }.average().toFloat() else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .hazeChild(
                state = hazeState,
                shape = RoundedCornerShape(28.dp),
                style = HazeStyle(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    tint = HazeTint(Color.Black.copy(alpha = 0.22f)),
                    blurRadius = 24.dp
                )
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(primaryGraphColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = null,
                            tint = primaryGraphColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PERFORMANCE ANALYTICS",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = if (displayPoints.isEmpty()) "No $routineType Exams" else "$routineType: ${displayPoints.size} Exams Analyzed",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = primaryGraphColor.copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, primaryGraphColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "Avg ${avgScore.toInt()}%",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = primaryGraphColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Exam Category Connected Segmented Pill Buttons (All, Daily, Weekly, Finals)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                examCategories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    val weight by animateFloatAsState(if (isSelected) 1.35f else 1f, label = "catWeight")
                    val cornerRadius by animateDpAsState(if (isSelected) 22.dp else 10.dp, label = "catCorners")
                    val bgColor by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.primary 
                        else Color.White.copy(alpha = 0.08f), 
                        label = "catBg"
                    )
                    val contentColor by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.onPrimary 
                        else Color.White.copy(alpha = 0.7f), 
                        label = "catContent"
                    )

                    val labelTitle = when (cat) {
                        "All" -> "All"
                        "Daily" -> "Daily"
                        "Weekly" -> "Weekly"
                        else -> "Finals"
                    }

                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(cornerRadius))
                            .background(bgColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { 
                                HapticHelper.performHaptic(context, HapticType.SELECTION)
                                selectedCategory = cat 
                                selectedPointIndex = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = labelTitle,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            ),
                            color = contentColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Subject Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(subjects) { subject ->
                    val isSelected = selectedSubject == subject
                    val chipBg by animateColorAsState(
                        if (isSelected) primaryGraphColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f),
                        label = "chipBg"
                    )
                    val chipBorder by animateColorAsState(
                        if (isSelected) primaryGraphColor else Color.White.copy(alpha = 0.12f),
                        label = "chipBorder"
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(chipBg)
                            .border(0.5.dp, chipBorder, RoundedCornerShape(16.dp))
                            .clickable {
                                HapticHelper.performHaptic(context, HapticType.SELECTION)
                                selectedSubject = subject
                                selectedPointIndex = null
                            }
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = subject,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Graph Canvas with smooth Bézier curve & Touch Tooltip
            val animProgress = remember { Animatable(0f) }
            LaunchedEffect(routineType, selectedSubject, selectedCategory, displayPoints.size) {
                animProgress.snapTo(0f)
                animProgress.animateTo(1f, animationSpec = tween(900, easing = FastOutSlowInEasing))
            }

            if (displayPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.03f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No $routineType $selectedCategory exam scores recorded for $selectedSubject yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val pointWidthDp = 64.dp
                val isScrollable = displayPoints.size > 5
                val chartScrollState = rememberScrollState()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                ) {
                    // Y-Axis Static Labels Column on the left
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(28.dp)
                            .padding(top = 10.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("100%", "75%", "50%", "25%", "0%").forEach { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = Color.White.copy(alpha = 0.4f),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Chart Canvas + X-Axis Labels (Scrollable if > 5 points)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 30.dp)
                            .then(if (isScrollable) Modifier.horizontalScroll(chartScrollState) else Modifier)
                    ) {
                        val totalContentWidth = if (isScrollable) pointWidthDp * displayPoints.size else 280.dp

                        Column(
                            modifier = Modifier
                                .width(if (isScrollable) totalContentWidth else 280.dp)
                                .fillMaxHeight()
                        ) {
                            // Canvas Area
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .pointerInput(displayPoints) {
                                        detectTapGestures { offset ->
                                            val spacing = size.width / displayPoints.size
                                            val index = (offset.x / spacing).toInt().coerceIn(0, displayPoints.lastIndex)
                                            selectedPointIndex = if (selectedPointIndex == index) null else index
                                        }
                                    }
                            ) {
                                val width = size.width
                                val height = size.height
                                val paddingTop = 16.dp.toPx()
                                val paddingBottom = 16.dp.toPx()
                                val chartHeight = height - paddingTop - paddingBottom

                                // Draw Horizontal Grid Lines
                                val benchmark = SettingsRepository.benchmarkPassPercentage.intValue.toFloat()
                                val levels = listOf(100f, 75f, 50f, 25f, 0f)
                                levels.forEach { level ->
                                    val y = paddingTop + chartHeight * (1f - level / 100f)
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.08f),
                                        start = Offset(0f, y),
                                        end = Offset(width, y),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }

                                // Benchmark Line
                                val benchmarkY = paddingTop + chartHeight * (1f - benchmark / 100f)
                                drawLine(
                                    color = Color(0xFFFFD54F).copy(alpha = 0.7f),
                                    start = Offset(0f, benchmarkY),
                                    end = Offset(width, benchmarkY),
                                    strokeWidth = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                                )

                                val pointOffsets = mutableListOf<Offset>()
                                val colWidth = width / displayPoints.size

                                displayPoints.forEachIndexed { i, pt ->
                                    val x = (i + 0.5f) * colWidth
                                    val animatedPercent = pt.percentage * animProgress.value
                                    val y = paddingTop + chartHeight * (1f - (animatedPercent / 100f).coerceIn(0f, 1f))
                                    pointOffsets.add(Offset(x, y))
                                }

                                if (pointOffsets.size > 1) {
                                    val fillPath = Path().apply { moveTo(pointOffsets.first().x, pointOffsets.first().y) }
                                    val curvePath = Path().apply { moveTo(pointOffsets.first().x, pointOffsets.first().y) }

                                    for (i in 0 until pointOffsets.size - 1) {
                                        val p0 = pointOffsets[i]
                                        val p1 = pointOffsets[i + 1]
                                        val controlX1 = p0.x + (p1.x - p0.x) / 2f
                                        val controlY1 = p0.y
                                        val controlX2 = p0.x + (p1.x - p0.x) / 2f
                                        val controlY2 = p1.y

                                        curvePath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                                        fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                                    }

                                    fillPath.lineTo(pointOffsets.last().x, height - paddingBottom)
                                    fillPath.lineTo(pointOffsets.first().x, height - paddingBottom)
                                    fillPath.close()

                                    drawPath(
                                        path = fillPath,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                primaryGraphColor.copy(alpha = 0.35f * animProgress.value),
                                                secondaryGraphColor.copy(alpha = 0.08f * animProgress.value),
                                                Color.Transparent
                                            ),
                                            startY = paddingTop,
                                            endY = height - paddingBottom
                                        )
                                    )

                                    drawPath(
                                        path = curvePath,
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(primaryGraphColor, secondaryGraphColor)
                                        ),
                                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }

                                // Draw Points
                                pointOffsets.forEachIndexed { i, offset ->
                                    val isSelected = selectedPointIndex == i

                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                primaryGraphColor.copy(alpha = if (isSelected) 0.7f else 0.25f),
                                                Color.Transparent
                                            ),
                                            center = offset,
                                            radius = (if (isSelected) 18.dp else 12.dp).toPx()
                                        ),
                                        center = offset,
                                        radius = (if (isSelected) 18.dp else 12.dp).toPx()
                                    )

                                    drawCircle(
                                        color = primaryGraphColor,
                                        center = offset,
                                        radius = (if (isSelected) 6.dp else 4.5.dp).toPx()
                                    )

                                    drawCircle(
                                        color = Color.White,
                                        center = offset,
                                        radius = (if (isSelected) 3.5.dp else 2.5.dp).toPx()
                                    )
                                }
                            }

                            // X-Axis Labels Row (Strictly 1 Line per Point, Never Overflows!)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(30.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                displayPoints.forEachIndexed { i, pt ->
                                    val isSelected = selectedPointIndex == i
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = pt.label,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                            ),
                                            color = if (isSelected) primaryGraphColor else Color.White.copy(alpha = 0.65f),
                                            modifier = Modifier.clickable { selectedPointIndex = if (selectedPointIndex == i) null else i }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Floating Tooltip on Selected Point
                    selectedPointIndex?.let { index ->
                        if (index in displayPoints.indices) {
                            val pt = displayPoints[index]
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth(0.9f) // বক্সটি যেন স্ক্রিনের বাইরে না যায়, তাই সর্বোচ্চ 90% সাইজ দেওয়া হলো
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E293B).copy(alpha = 0.95f))
                                    .border(0.5.dp, primaryGraphColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${pt.fullName} (${pt.date})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis // নাম বড় হলে শেষে ... দেখাবে
                                    )
                                    Text(
                                        text = "Score: ${formatScore(pt.obtained)} / ${formatScore(pt.max)}  (${pt.percentage.toInt()}%)",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = primaryGraphColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(14.dp))

            // Quick Stats Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickStatItem(title = "Peak Score", value = if (displayPoints.isEmpty()) "--" else "${highestScore.toInt()}%", color = Color(0xFF4CAF50))
                QuickStatItem(title = "Average", value = if (displayPoints.isEmpty()) "--" else "${avgScore.toInt()}%", color = primaryGraphColor)
                QuickStatItem(title = "Lowest", value = if (displayPoints.isEmpty()) "--" else "${lowestScore.toInt()}%", color = Color(0xFFFF9800))
                QuickStatItem(title = "Exams Logged", value = "${displayPoints.size}", color = Color(0xFF64B5F6))
            }
        }
    }
}

@Composable
private fun QuickStatItem(
    title: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = Color.White.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = color
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
