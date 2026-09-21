package com.marjuk.eaptracker.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

@Composable
fun WeeklyStudyAnalyticsCard(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    showFullStatsButton: Boolean = true,
    onOpenStatistics: () -> Unit = {}
) {
    val shape = RoundedCornerShape(26.dp)
    val weeklyStats = remember(StudyTrackerRepository.todayStudyMinutes.intValue) {
        StudyTrackerRepository.getWeeklyStudyStats()
    }
    val goalHours = ProfileRepository.dailyStudyHoursGoal.intValue.toFloat()

    val totalWeeklyHours = weeklyStats.sumOf { it.studyHours.toDouble() }.toFloat()
    val dailyAvg = if (weeklyStats.isNotEmpty()) totalWeeklyHours / weeklyStats.size else 0f
    val daysGoalMet = weeklyStats.count { it.studyHours >= goalHours && goalHours > 0 }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    tint = HazeTint(Color.Black.copy(alpha = 0.2f)),
                    blurRadius = 24.dp
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), Color.White.copy(alpha = 0.06f))
                    )
                ),
                shape
            )
            .then(
                if (showFullStatsButton) Modifier.clickable { onOpenStatistics() }
                else Modifier
            )
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "WEEKLY STUDY TIME",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "7-Day Consistency Tracker",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                if (showFullStatsButton) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.clickable { onOpenStatistics() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Full Stats",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$daysGoalMet/7 Met",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3-Metric Summary Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Total Time", style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp))
                    Text(text = "${String.format("%.1f", totalWeeklyHours)}h", style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFF00E5FF), fontWeight = FontWeight.ExtraBold))
                }

                VerticalDivider(modifier = Modifier.height(24.dp).width(1.dp), color = Color.White.copy(alpha = 0.12f))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Daily Avg", style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp))
                    Text(text = "${String.format("%.1f", dailyAvg)}h", style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
                }

                VerticalDivider(modifier = Modifier.height(24.dp).width(1.dp), color = Color.White.copy(alpha = 0.12f))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Target Goal", style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp))
                    Text(text = "${goalHours.toInt()}h / day", style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFFFFB74D), fontWeight = FontWeight.Bold))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 7-Day Bar Chart
            val maxBarHeight = 110.dp
            val maxHoursInWeek = maxOf(goalHours * 1.2f, weeklyStats.maxOfOrNull { it.studyHours } ?: goalHours, 6f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(maxBarHeight + 36.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyStats.forEach { stat ->
                    val fraction = (stat.studyHours / maxHoursInWeek).coerceIn(0.06f, 1f)
                    val isGoalMet = stat.studyHours >= goalHours && goalHours > 0

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Value label above bar
                        Text(
                            text = if (stat.studyHours > 0) "${String.format("%.1f", stat.studyHours)}h" else "0",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                fontWeight = if (stat.isToday) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isGoalMet) Color(0xFF81C784) else if (stat.isToday) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.6f)
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Vertical Bar
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(maxBarHeight * fraction)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                .background(
                                    Brush.verticalGradient(
                                        if (isGoalMet) listOf(Color(0xFFFFD54F), Color(0xFF4CAF50))
                                        else if (stat.isToday) listOf(Color(0xFF00E5FF), MaterialTheme.colorScheme.primary)
                                        else listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.12f))
                                    )
                                )
                                .border(
                                    0.5.dp,
                                    if (stat.isToday) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.15f),
                                    RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                                )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Day label (e.g. Sat, Sun, Today)
                        Text(
                            text = if (stat.isToday) "Today" else stat.dayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (stat.isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (stat.isToday) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }
        }
    }
}
