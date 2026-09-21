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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import com.marjuk.eaptracker.ui.components.adaptiveGlass

@Composable
fun DailyStudyGoalCard(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    onNavigateToStats: () -> Unit = {},
    onOpenFullScreenTimer: () -> Unit = {}
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(26.dp)

    val goalHours = ProfileRepository.dailyStudyHoursGoal.intValue
    val studyMins by StudyTrackerRepository.todayStudyMinutes
    val isTimerRunning by StudyTrackerRepository.isTimerRunning
    val elapsedSecs by StudyTrackerRepository.sessionElapsedSeconds

    val currentHours = studyMins / 60f
    val progress = if (goalHours > 0) (currentHours / goalHours).coerceIn(0f, 1f) else 0f
    val percentInt = if (goalHours > 0) ((currentHours / goalHours) * 100).toInt() else 0
    val isGoalAchieved = currentHours >= goalHours && goalHours > 0

    var isCardExpanded by remember { mutableStateOf(false) }

    var showPuppetDialog by remember { mutableStateOf(false) }

    if (showPuppetDialog) {
        PuppetPreviewDialog(
            currentHours = currentHours,
            hazeState = hazeState,
            onDismiss = { showPuppetDialog = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .adaptiveGlass(
                hazeState = hazeState,
                shape = shape,
                style = HazeStyle(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    tint = HazeTint(Color.Black.copy(alpha = 0.22f)),
                    blurRadius = 24.dp
                ),
                borderWidth = 1.dp,
                borderColor = if (isGoalAchieved) Color(0xFFFFD54F).copy(alpha = 0.5f)
                              else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).clickable { onNavigateToStats() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isGoalAchieved) Color(0xFFFFD54F).copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                            .clickable {
                                HapticHelper.performHaptic(context, HapticType.LIGHT)
                                showPuppetDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        StudyPuppetAvatar(
                            studyHours = currentHours,
                            strokeColor = if (isGoalAchieved) Color(0xFFFFD54F) else MaterialTheme.colorScheme.primary,
                            isStudying = isTimerRunning,
                            size = 34.dp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "DAILY STUDY GOAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isGoalAchieved) Color(0xFFFFD54F) else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            maxLines = 1
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${String.format("%.1f", currentHours)} of ${goalHours}.0 hrs ($percentInt%)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                            if (isGoalAchieved) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFFD54F).copy(alpha = 0.2f),
                                    border = BorderStroke(0.5.dp, Color(0xFFFFD54F))
                                ) {
                                    Text(
                                        text = "🏆 Achieved",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFFFFD54F),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp
                                        ),
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Fullscreen Focus Mode Button
                    Surface(
                        onClick = {
                            HapticHelper.performHaptic(context, HapticType.LIGHT)
                            onOpenFullScreenTimer()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInFull,
                                contentDescription = "Full Screen Focus Mode",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // 2. Expand / Collapse Timer Button
                    Surface(
                        onClick = {
                            HapticHelper.performHaptic(context, HapticType.LIGHT)
                            isCardExpanded = !isCardExpanded
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isTimerRunning) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(
                            0.8.dp,
                            if (isTimerRunning) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.18f)
                        ),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isTimerRunning) Icons.Default.PlayArrow else Icons.Default.Timer,
                                contentDescription = null,
                                tint = if (isTimerRunning) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isCardExpanded) "Hide" else if (isTimerRunning) "Active" else "Timer",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isTimerRunning) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = if (isCardExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Linear Progress Bar with glowing accent
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                if (isGoalAchieved) listOf(Color(0xFF4CAF50), Color(0xFFFFD54F))
                                else listOf(Color(0xFF00E5FF), MaterialTheme.colorScheme.primary)
                            )
                        )
                )
            }

            // Expanded Live Timer & Quick Log Controls
            AnimatedVisibility(visible = isCardExpanded || isTimerRunning) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    // Timer Digital Display
                    val hrs = elapsedSecs / 3600
                    val mins = (elapsedSecs % 3600) / 60
                    val secs = elapsedSecs % 60
                    val timerDisplay = "%02d:%02d:%02d".format(hrs, mins, secs)

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isTimerRunning) {
                                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                    val alpha by infiniteTransition.animateFloat(
                                        initialValue = 0.3f,
                                        targetValue = 1f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(800, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "alpha"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                Text(
                                    text = if (isTimerRunning) "FOCUS SESSION IN PROGRESS" else "LIVE FOCUS STOPWATCH",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isTimerRunning) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = timerDisplay,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (isTimerRunning) MaterialTheme.colorScheme.primary else Color.White,
                                    letterSpacing = 2.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Timer Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!isTimerRunning) {
                                    Button(
                                        onClick = {
                                            HapticHelper.performHaptic(context, HapticType.SUCCESS)
                                            StudyTrackerRepository.startFocusTimer()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Start Focus", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            HapticHelper.performHaptic(context, HapticType.LIGHT)
                                            StudyTrackerRepository.pauseFocusTimer()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                    ) {
                                        Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Pause", fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            HapticHelper.performHaptic(context, HapticType.SUCCESS)
                                            StudyTrackerRepository.logAndSaveSession()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4CAF50)
                                        )
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save", fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Fullscreen Focus Mode Button
                                OutlinedButton(
                                    onClick = {
                                        HapticHelper.performHaptic(context, HapticType.LIGHT)
                                        onOpenFullScreenTimer()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(horizontal = 10.dp)
                                ) {
                                    Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", modifier = Modifier.size(18.dp))
                                }

                                if (elapsedSecs > 0 && !isTimerRunning) {
                                    OutlinedButton(
                                        onClick = {
                                            HapticHelper.performHaptic(context, HapticType.LIGHT)
                                            StudyTrackerRepository.resetSessionTimer()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                                        contentPadding = PaddingValues(horizontal = 10.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Log Buttons Row
                    Text(
                        text = "Quick Log Study Time:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(30 to "+30m", 60 to "+1h", 120 to "+2h", -30 to "-30m").forEach { (mins, text) ->
                            val isNegative = mins < 0
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isNegative) Color(0xFFFF5252).copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    )
                                    .border(
                                        0.5.dp,
                                        if (isNegative) Color(0xFFFF5252).copy(alpha = 0.3f)
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        HapticHelper.performHaptic(context, HapticType.LIGHT)
                                        StudyTrackerRepository.addStudyMinutes(mins)
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isNegative) Color(0xFFFF8A80) else MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
