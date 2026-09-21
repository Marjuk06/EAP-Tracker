package com.marjuk.eaptracker.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marjuk.eaptracker.LocalHazeState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import java.util.Calendar

data class StatTabItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun StudyStatisticsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val hazeState = LocalHazeState.current

    var selectedTab by remember { mutableStateOf("Day") }
    val tabItems = listOf(
        StatTabItem("Day", Icons.Default.CalendarToday),
        StatTabItem("Week", Icons.Default.DateRange),
        StatTabItem("Month", Icons.Default.CalendarMonth)
    )

    // Calendar state (Year & Month 1..12)
    val todayCal = Calendar.getInstance()
    var currentYear by remember { mutableIntStateOf(todayCal.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableIntStateOf(todayCal.get(Calendar.MONTH) + 1) }

    var selectedDateItem by remember { mutableStateOf<CalendarDayItem?>(null) }

    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    HapticHelper.performHaptic(context, HapticType.LIGHT)
                    onBack()
                },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = "STATISTICS",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )
            )

            Spacer(modifier = Modifier.size(42.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Tab Filter Navbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabItems.forEach { item ->
                val isSelected = selectedTab == item.title

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            HapticHelper.performHaptic(context, HapticType.SELECTION)
                            selectedTab = item.title
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Tab Content
        when (selectedTab) {
            "Day" -> {
                CalendarDayView(
                    hazeState = hazeState,
                    year = currentYear,
                    month = currentMonth,
                    monthName = monthNames[currentMonth - 1],
                    selectedItem = selectedDateItem,
                    onPrevMonth = {
                        HapticHelper.performHaptic(context, HapticType.LIGHT)
                        if (currentMonth == 1) {
                            currentMonth = 12
                            currentYear -= 1
                        } else {
                            currentMonth -= 1
                        }
                    },
                    onNextMonth = {
                        HapticHelper.performHaptic(context, HapticType.LIGHT)
                        if (currentMonth == 12) {
                            currentMonth = 1
                            currentYear += 1
                        } else {
                            currentMonth += 1
                        }
                    },
                    onSelectDay = { dayItem ->
                        HapticHelper.performHaptic(context, HapticType.SELECTION)
                        selectedDateItem = dayItem
                    }
                )
            }
            "Week" -> {
                WeeklyDetailedStatsView(hazeState = hazeState)
            }
            "Month" -> {
                MonthlyOverviewStatsView(
                    hazeState = hazeState,
                    year = currentYear,
                    onPrevYear = { currentYear -= 1 },
                    onNextYear = { currentYear += 1 }
                )
            }
        }
    }
}

// ================= 1. CALENDAR DAY VIEW (APP THEME HEATMAP) =================

@Composable
fun CalendarDayView(
    hazeState: HazeState,
    year: Int,
    month: Int,
    monthName: String,
    selectedItem: CalendarDayItem?,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (CalendarDayItem) -> Unit
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(26.dp)
    val themePrimary = MaterialTheme.colorScheme.primary
    val daysGrid = remember(year, month, StudyTrackerRepository.todayStudyMinutes.intValue) {
        StudyTrackerRepository.getCalendarMonthGrid(year, month)
    }

    val totalMonthMinutes = daysGrid.filter { it.isCurrentMonth }.sumOf { it.studyMinutes }
    val totalMonthHours = totalMonthMinutes / 60
    val totalMonthMinsRem = totalMonthMinutes % 60
    val currentMonthDaysWithStudy = daysGrid.filter { it.isCurrentMonth && it.studyMinutes > 0 }
    val avgHours = if (currentMonthDaysWithStudy.isNotEmpty()) (totalMonthMinutes / 60f) / currentMonthDaysWithStudy.size else 0f

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Main Calendar Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .hazeChild(
                        state = hazeState,
                        style = HazeStyle(
                            backgroundColor = MaterialTheme.colorScheme.background,
                            tint = HazeTint(Color.Black.copy(alpha = 0.22f)),
                            blurRadius = 24.dp
                        )
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(themePrimary.copy(alpha = 0.4f), Color.White.copy(alpha = 0.08f))
                            )
                        ),
                        shape
                    )
                    .padding(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Month Navigator Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPrevMonth,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = Color.White)
                        }

                        Text(
                            text = "$monthName $year",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        )

                        IconButton(
                            onClick = onNextMonth,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Weekday Header (Sat, Sun, Mon, Tue, Wed, Thu, Fri)
                    val weekdays = listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        weekdays.forEach { day ->
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar Grid
                    val rows = daysGrid.chunked(7)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        rows.forEach { weekRow ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                weekRow.forEach { dayItem ->
                                    val isSelected = selectedItem?.dayNumber == dayItem.dayNumber &&
                                            selectedItem.month == dayItem.month &&
                                            selectedItem.year == dayItem.year

                                    CalendarDayCell(
                                        item = dayItem,
                                        isSelected = isSelected,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onSelectDay(dayItem) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Heatmap Scale Legend Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            HeatmapLegendChip(label = "0+", color = Color.White.copy(alpha = 0.08f))
                            HeatmapLegendChip(label = "4+", color = themePrimary.copy(alpha = 0.35f))
                            HeatmapLegendChip(label = "7+", color = themePrimary.copy(alpha = 0.6f))
                            HeatmapLegendChip(label = "10+", color = themePrimary.copy(alpha = 0.85f))
                            HeatmapLegendChip(label = "12+", color = themePrimary)
                        }

                        Text(
                            text = "${monthName.take(3)}: ${totalMonthHours}H ${totalMonthMinsRem}M",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = themePrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }

        // Selected Date Details Card
        if (selectedItem != null) {
            item {
                SelectedDateSummaryCard(
                    item = selectedItem,
                    hazeState = hazeState,
                    onAddMinutes = { mins ->
                        HapticHelper.performHaptic(context, HapticType.LIGHT)
                        val updated = (selectedItem.studyMinutes + mins).coerceAtLeast(0)
                        StudyTrackerRepository.saveStudyMinutesForDate(
                            selectedItem.year, selectedItem.month, selectedItem.dayNumber, updated
                        )
                    }
                )
            }
        }

        // Month Summary Metrics
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "Total Monthly Study", style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.55f)))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "${totalMonthHours}h ${totalMonthMinsRem}m", style = MaterialTheme.typography.titleMedium.copy(color = themePrimary, fontWeight = FontWeight.Black))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "Daily Average", style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.55f)))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "${String.format("%.1f", avgHours)} hrs/day", style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFF00E5FF), fontWeight = FontWeight.Black))
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    item: CalendarDayItem,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val themePrimary = MaterialTheme.colorScheme.primary
    val cellColor = when {
        !item.isCurrentMonth -> Color.White.copy(alpha = 0.02f)
        item.heatmapLevel == 0 -> Color.White.copy(alpha = 0.05f)
        item.heatmapLevel == 1 -> themePrimary.copy(alpha = 0.22f)
        item.heatmapLevel == 2 -> themePrimary.copy(alpha = 0.45f)
        item.heatmapLevel == 3 -> themePrimary.copy(alpha = 0.68f)
        item.heatmapLevel == 4 -> themePrimary.copy(alpha = 0.88f)
        else -> themePrimary
    }

    val textColor = when {
        !item.isCurrentMonth -> Color.White.copy(alpha = 0.2f)
        item.heatmapLevel >= 4 -> MaterialTheme.colorScheme.onPrimary
        item.heatmapLevel >= 1 -> Color.White.copy(alpha = 0.95f)
        else -> Color.White.copy(alpha = 0.7f)
    }

    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = modifier
            .aspectRatio(0.72f)
            .clip(shape)
            .background(cellColor)
            .border(
                if (item.isToday) 1.5.dp else if (isSelected) 1.dp else 0.5.dp,
                if (item.isToday) themePrimary else if (isSelected) Color.White else Color.White.copy(alpha = 0.08f),
                shape
            )
            .clickable(onClick = onClick)
            .padding(2.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // Day Number
            Text(
                text = "${item.dayNumber}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (item.isToday || isSelected) FontWeight.Black else FontWeight.Bold,
                    fontSize = 10.sp,
                    color = if (item.isToday) themePrimary else textColor
                )
            )

            // Duration text (e.g. 8:14)
            if (item.formattedDuration.isNotBlank() && item.isCurrentMonth) {
                Text(
                    text = item.formattedDuration,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor
                    ),
                    maxLines = 1
                )
            } else {
                Spacer(modifier = Modifier.height(1.dp))
            }
        }
    }
}

@Composable
fun HeatmapLegendChip(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color,
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.85f)
            ),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun SelectedDateSummaryCard(
    item: CalendarDayItem,
    hazeState: HazeState,
    onAddMinutes: (Int) -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val goalHours = ProfileRepository.dailyStudyHoursGoal.intValue.toFloat()
    val isGoalMet = item.studyHours >= goalHours && goalHours > 0
    val themePrimary = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    tint = HazeTint(Color.Black.copy(alpha = 0.2f)),
                    blurRadius = 20.dp
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    if (isGoalMet) Color(0xFF4CAF50).copy(alpha = 0.4f) else themePrimary.copy(alpha = 0.4f)
                ),
                shape
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${item.dayNumber} %02d/%04d".format(item.month, item.year),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = if (item.isToday) "Today's Study Session" else "Logged Record",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isGoalMet) Color(0xFF4CAF50).copy(alpha = 0.2f) else themePrimary.copy(alpha = 0.2f),
                    border = BorderStroke(0.5.dp, if (isGoalMet) Color(0xFF4CAF50) else themePrimary)
                ) {
                    Text(
                        text = if (item.studyMinutes > 0) "${String.format("%.1f", item.studyHours)} hrs (${item.studyMinutes}m)" else "No Study Logged",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (isGoalMet) Color(0xFF81C784) else themePrimary,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick adjustment buttons for selected date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(30 to "+30m", 60 to "+1h", 120 to "+2h", -30 to "-30m").forEach { (mins, label) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { onAddMinutes(mins) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (mins < 0) Color(0xFFFF8A80) else Color(0xFF00E5FF),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

// ================= 2. WEEKLY DETAILED STATS VIEW =================

@Composable
fun WeeklyDetailedStatsView(hazeState: HazeState) {
    val weeklyStats = remember(StudyTrackerRepository.todayStudyMinutes.intValue) {
        StudyTrackerRepository.getWeeklyStudyStats()
    }
    val goalHours = ProfileRepository.dailyStudyHoursGoal.intValue.toFloat()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            WeeklyStudyAnalyticsCard(
                hazeState = hazeState,
                showFullStatsButton = false
            )
        }

        item {
            Text(
                text = "DAY-BY-DAY LOG",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }

        items(weeklyStats) { stat ->
            val isMet = stat.studyHours >= goalHours && goalHours > 0
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isMet) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stat.dayName.take(1),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (isMet) Color(0xFF81C784) else Color.White
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "${stat.dayName} (${stat.dateStr})",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = if (stat.isToday) "Today's Log" else if (isMet) "Target Met" else "Under Target",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isMet) Color(0xFF81C784) else Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Text(
                        text = "${String.format("%.1f", stat.studyHours)} hrs",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = if (isMet) Color(0xFFFFD54F) else Color.White
                        )
                    )
                }
            }
        }
    }
}

// ================= 3. MONTHLY OVERVIEW STATS VIEW =================

@Composable
fun MonthlyOverviewStatsView(
    hazeState: HazeState,
    year: Int,
    onPrevYear: () -> Unit,
    onNextYear: () -> Unit
) {
    val themePrimary = MaterialTheme.colorScheme.primary
    val monthSummaries = remember(year, StudyTrackerRepository.todayStudyMinutes.intValue) {
        StudyTrackerRepository.getYearlyMonthSummaries(year)
    }

    val totalYearHours = monthSummaries.sumOf { it.totalHours.toDouble() }.toFloat()
    val activeMonths = monthSummaries.count { it.totalHours > 0f }
    val monthlyAvg = if (activeMonths > 0) totalYearHours / activeMonths else 0f

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Year Navigator & Totals
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPrevYear,
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color.White)
                        }

                        Text(
                            text = "YEAR $year",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        )

                        IconButton(
                            onClick = onNextYear,
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Year Study", style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.55f)))
                            Text("${String.format("%.1f", totalYearHours)} hrs", style = MaterialTheme.typography.titleLarge.copy(color = themePrimary, fontWeight = FontWeight.Black))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Monthly Average", style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.55f)))
                            Text("${String.format("%.1f", monthlyAvg)} hrs", style = MaterialTheme.typography.titleLarge.copy(color = Color(0xFF00E5FF), fontWeight = FontWeight.Black))
                        }
                    }
                }
            }
        }

        // 12 Months Density Grid
        item {
            Text(
                text = "12-MONTH DENSITY GRID",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        val chunked = monthSummaries.chunked(3)
        items(chunked) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { mStat ->
                    val bg = when {
                        mStat.totalHours >= 150f -> themePrimary
                        mStat.totalHours >= 100f -> themePrimary.copy(alpha = 0.75f)
                        mStat.totalHours >= 50f -> themePrimary.copy(alpha = 0.5f)
                        mStat.totalHours > 0f -> themePrimary.copy(alpha = 0.25f)
                        else -> Color.White.copy(alpha = 0.05f)
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = bg,
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = mStat.monthName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            )
                            Text(
                                text = if (mStat.totalHours > 0f) "${mStat.totalHours.toInt()}h" else "--",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ================= 4. STUDY TREND ANALYTICS VIEW =================

@Composable
fun StudyTrendAnalyticsView(hazeState: HazeState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "FOCUS DISCIPLINE & STREAK",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "“Consistency beats intensity. One page, one problem, one step closer.”",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.85f),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    )
                }
            }
        }

        item {
            WeeklyStudyAnalyticsCard(
                hazeState = hazeState,
                showFullStatsButton = false
            )
        }
    }
}
