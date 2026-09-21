package com.marjuk.eaptracker.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.marjuk.eaptracker.LocalHazeState
import com.marjuk.eaptracker.ui.HapticHelper
import com.marjuk.eaptracker.ui.HapticType
import com.marjuk.eaptracker.ui.ProfileRepository
import com.marjuk.eaptracker.ui.StudyTrackerRepository

/**
 * DynamicIslandPill
 * An animated, interactive floating Dynamic Island pill situated at the top camera punch-hole level.
 * Features:
 * - Compact state: Pulses with live study timer & active subject.
 * - Expanded HUD state: Interactive controller with Play/Pause, Finish & Log, Fullscreen focus, and Goal progress.
 */
@Composable
fun DynamicIslandPill(
    modifier: Modifier = Modifier,
    onNavigateToFullscreen: () -> Unit = {}
) {
    val context = LocalContext.current
    val hazeState = LocalHazeState.current

    val isRunning by StudyTrackerRepository.isTimerRunning
    val elapsedSeconds by StudyTrackerRepository.sessionElapsedSeconds
    val activeSubject by StudyTrackerRepository.activeSubject
    val todayMins by StudyTrackerRepository.todayStudyMinutes
    val goalHours = ProfileRepository.dailyStudyHoursGoal.intValue

    var isExpanded by remember { mutableStateOf(false) }

    // Only display Dynamic Island when timer is active or has paused session
    if (!isRunning && elapsedSeconds == 0) {
        return
    }

    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60
    val formattedTime = if (hours > 0) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "island_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = if (isRunning) 1f else 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "island_dot_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .statusBarsPadding()
            .zIndex(100f),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220, delayMillis = 50)) +
                        scaleIn(initialScale = 0.92f, animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(150)) +
                                scaleOut(targetScale = 0.92f, animationSpec = tween(150))
                    )
            },
            label = "island_expand_transition"
        ) { expanded ->
            if (expanded) {
                // ==================== EXPANDED HUD STATE ====================
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(26.dp))
                        .adaptiveGlass(
                            hazeState = hazeState,
                            shape = RoundedCornerShape(26.dp),
                            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        )
                        .padding(18.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Top Header: Subject + Collapse icon
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            color = if (isRunning) Color(0xFF00E5FF) else Color(0xFFFFB74D),
                                            shape = CircleShape
                                        )
                                )
                                Text(
                                    text = activeSubject,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = {
                                    HapticHelper.performHaptic(context, HapticType.LIGHT)
                                    isExpanded = false
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Collapse Island",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Big Glowing Timer Display
                        Text(
                            text = formattedTime,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )

                        // Today's Goal Progress Bar
                        val todayHours = todayMins / 60f
                        val progressFraction = (todayHours / goalHours.coerceAtLeast(1)).coerceIn(0f, 1f)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Daily Goal Progress",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${String.format(java.util.Locale.US, "%.1f", todayHours)} / ${goalHours}h (${(progressFraction * 100).toInt()}%)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }

                        // Interactive Control Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Play / Pause Button
                            Button(
                                onClick = {
                                    HapticHelper.performHaptic(context, HapticType.MEDIUM)
                                    if (isRunning) {
                                        StudyTrackerRepository.pauseFocusTimer(context)
                                    } else {
                                        StudyTrackerRepository.startFocusTimer(activeSubject, context)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRunning) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                                    contentColor = if (isRunning) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isRunning) "Pause" else "Resume",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isRunning) "Pause" else "Resume",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }

                            // 2. Finish & Log Button
                            OutlinedButton(
                                onClick = {
                                    HapticHelper.performHaptic(context, HapticType.SUCCESS)
                                    StudyTrackerRepository.logAndSaveSession(context)
                                    isExpanded = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color(0xFF4CAF50).copy(alpha = 0.5f)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Finish Session",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Finish",
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }

                            // 3. Fullscreen Button
                            IconButton(
                                onClick = {
                                    HapticHelper.performHaptic(context, HapticType.LIGHT)
                                    isExpanded = false
                                    onNavigateToFullscreen()
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInFull,
                                    contentDescription = "Fullscreen Focus",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // ==================== COMPACT PILL STATE ====================
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .adaptiveGlass(
                            hazeState = hazeState,
                            shape = RoundedCornerShape(30.dp),
                            borderColor = if (isRunning) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.12f)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            HapticHelper.performHaptic(context, HapticType.SELECTION)
                            isExpanded = true
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pulsing Neon Dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (isRunning) Color(0xFF00E5FF).copy(alpha = pulseAlpha) else Color(0xFFFFB74D),
                                shape = CircleShape
                            )
                    )

                    // Timer String
                    Text(
                        text = formattedTime,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Divider dot
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )

                    // Subject text (truncated)
                    Text(
                        text = activeSubject,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.widthIn(max = 110.dp),
                        overflow = TextOverflow.Ellipsis
                    )

                    // Expand icon
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand Island",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
