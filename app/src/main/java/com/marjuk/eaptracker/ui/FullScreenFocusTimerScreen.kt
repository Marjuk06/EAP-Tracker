package com.marjuk.eaptracker.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marjuk.eaptracker.LocalHazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

@Composable
fun FullScreenFocusTimerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val hazeState = LocalHazeState.current
    val themePrimary = MaterialTheme.colorScheme.primary

    val isRunning by StudyTrackerRepository.isTimerRunning
    val sessionSeconds by StudyTrackerRepository.sessionElapsedSeconds
    val todayMins by StudyTrackerRepository.todayStudyMinutes
    val goalHours = ProfileRepository.dailyStudyHoursGoal.intValue

    // Keep screen active while in full-screen focus mode
    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            if (!SettingsRepository.keepScreenOn.value) {
                window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    // Rotating Motivational Quote
    val quoteText = remember { QuotesRepository.getRandomQuote().quote }

    // Pulsing animation for active focus
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRunning) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Formatted session time HH:MM:SS or M:SS
    val hours = sessionSeconds / 3600
    val minutes = (sessionSeconds % 3600) / 60
    val seconds = sessionSeconds % 60
    val formattedTime = if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d:%02d".format(0, minutes, seconds)
    }

    val todayTotalHours = (todayMins + (sessionSeconds / 60)) / 60f

    var showPuppetDialog by remember { mutableStateOf(false) }

    if (showPuppetDialog) {
        PuppetPreviewDialog(
            currentHours = todayTotalHours,
            hazeState = hazeState,
            onDismiss = { showPuppetDialog = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0E))
    ) {
        // Subtle ambient radial glow behind timer
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (isRunning) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            themePrimary.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(size.width / 2, size.height * 0.45f),
                        radius = size.width * 0.7f
                    )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
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
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Exit Fullscreen",
                        tint = Color.White
                    )
                }

                // D-Day Target Exam Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = themePrimary.copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, themePrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.clickable {
                        HapticHelper.performHaptic(context, HapticType.LIGHT)
                        showPuppetDialog = true
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isRunning) Color(0xFF00E676) else themePrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "D-Day • Focus Mode",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = themePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                IconButton(
                    onClick = {
                        HapticHelper.performHaptic(context, HapticType.LIGHT)
                        onBack()
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Exit Fullscreen",
                        tint = Color.White
                    )
                }
            }

            // 2. Center Immersion Area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                // Live Status Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isRunning) Color(0xFF00E676).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(
                        0.5.dp,
                        if (isRunning) Color(0xFF00E676).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(if (isRunning) Color(0xFF00E676) else Color.White.copy(alpha = 0.4f))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRunning) "FOCUSING" else "PAUSED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isRunning) Color(0xFF81C784) else Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Giant Digital Timer Typography
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 62.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Sub-Metrics Row (Session Time vs Today Total)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Current Session",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${sessionSeconds / 60}m ${sessionSeconds % 60}s",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = themePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(26.dp)
                            .width(1.dp)
                            .background(Color.White.copy(alpha = 0.12f))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Today's Total",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${String.format("%.1f", todayTotalHours)} / ${goalHours}.0h",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color(0xFFFFD54F),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Animated Study Puppet Avatar (Clickable to preview all 4 levels)
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(themePrimary.copy(alpha = 0.08f))
                        .border(1.dp, themePrimary.copy(alpha = 0.25f), CircleShape)
                        .clickable {
                            HapticHelper.performHaptic(context, HapticType.LIGHT)
                            showPuppetDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    StudyPuppetAvatar(
                        studyHours = todayTotalHours,
                        strokeColor = themePrimary,
                        isStudying = isRunning,
                        size = 110.dp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Motivational Quote Pill
                Text(
                    text = "“$quoteText”",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.7f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            // 3. Bottom Controls Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset Button
                IconButton(
                    onClick = {
                        HapticHelper.performHaptic(context, HapticType.MEDIUM)
                        StudyTrackerRepository.resetSessionTimer()
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Timer",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Giant Play/Pause Focus Button
                IconButton(
                    onClick = {
                        HapticHelper.performHaptic(context, HapticType.STRONG)
                        if (isRunning) {
                            StudyTrackerRepository.pauseFocusTimer()
                        } else {
                            StudyTrackerRepository.startFocusTimer()
                        }
                    },
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                if (isRunning) listOf(Color(0xFFE53935), Color(0xFFC62828))
                                else listOf(themePrimary, themePrimary.copy(alpha = 0.8f))
                            )
                        )
                        .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Pause" else "Start Focus",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Save Session Button
                IconButton(
                    onClick = {
                        HapticHelper.performHaptic(context, HapticType.SUCCESS)
                        val addedMins = StudyTrackerRepository.logAndSaveSession()
                        if (addedMins > 0) {
                            android.widget.Toast.makeText(context, "Logged +${addedMins}m to today's study", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50).copy(alpha = 0.18f))
                        .border(0.5.dp, Color(0xFF4CAF50).copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save Session",
                        tint = Color(0xFF81C784),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
