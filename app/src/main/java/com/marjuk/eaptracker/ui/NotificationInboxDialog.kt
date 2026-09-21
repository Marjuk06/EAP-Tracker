package com.marjuk.eaptracker.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.marjuk.eaptracker.LocalHazeState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import com.marjuk.eaptracker.ui.components.adaptiveGlass

@Composable
fun NotificationInboxDialog(
    hazeState: HazeState? = null,
    onNavigate: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val notifs = NotificationHistoryRepository.itemsState
    val effectiveHazeState = hazeState ?: LocalHazeState.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f)
                .clip(RoundedCornerShape(28.dp))
                .adaptiveGlass(
                    hazeState = effectiveHazeState,
                    shape = RoundedCornerShape(28.dp),
                    style = HazeStyle(
                        backgroundColor = MaterialTheme.colorScheme.background,
                        tint = HazeTint(Color.Black.copy(alpha = 0.35f)),
                        blurRadius = 24.dp
                    ),
                    borderColor = Color.White.copy(alpha = 0.15f)
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "NOTIFICATIONS",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${notifs.size}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (notifs.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    HapticHelper.performHaptic(context, HapticType.LIGHT)
                                    NotificationHistoryRepository.clearAll()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Clear All",
                                    color = Color(0xFFEF5350),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                HapticHelper.performHaptic(context, HapticType.LIGHT)
                                onDismiss()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (notifs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.06f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsNone,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No Notifications Yet",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Timetable updates, syllabus additions, exam alerts, and backups will appear here instantly.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                ),
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(notifs, key = { it.id }) { notif ->
                            val catColor = when (notif.category.lowercase()) {
                                "alert", "urgent" -> Color(0xFFEF5350)
                                "motivation", "quote" -> Color(0xFFFFB74D)
                                "exam" -> Color(0xFF81C784)
                                "backup" -> Color(0xFF4FC3F7)
                                "syllabus" -> Color(0xFFBA68C8)
                                else -> MaterialTheme.colorScheme.primary
                            }

                            val catIcon = when (notif.category.lowercase()) {
                                "alert", "urgent" -> Icons.Default.Warning
                                "motivation", "quote" -> Icons.Default.FormatQuote
                                "exam" -> Icons.AutoMirrored.Filled.Assignment
                                "backup" -> Icons.Default.CloudDone
                                "syllabus" -> Icons.Default.MenuBook
                                else -> Icons.Default.Notifications
                            }

                            val dateFmt = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault())
                            val timeStr = dateFmt.format(java.util.Date(notif.timestamp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
                                    .clickable(enabled = notif.actionRoute != null) {
                                        notif.actionRoute?.let { route ->
                                            HapticHelper.performHaptic(context, HapticType.LIGHT)
                                            onDismiss()
                                            onNavigate(route)
                                        }
                                    }
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(CircleShape)
                                                    .background(catColor.copy(alpha = 0.18f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = catIcon,
                                                    contentDescription = null,
                                                    tint = catColor,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = notif.title,
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = timeStr,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color.White.copy(alpha = 0.45f),
                                                    fontSize = 10.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = {
                                                    HapticHelper.performHaptic(context, HapticType.LIGHT)
                                                    NotificationHistoryRepository.removeNotification(notif.id)
                                                },
                                                modifier = Modifier.size(22.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Delete",
                                                    tint = Color.White.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = notif.message,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color.White.copy(alpha = 0.88f),
                                            lineHeight = 19.sp,
                                            fontSize = 13.sp
                                        )
                                    )

                                    // Action Buttons (Action URL or Deep Route)
                                    if (notif.actionUrl != null || notif.actionRoute != null) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            if (notif.actionUrl != null) {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                                    modifier = Modifier.clickable {
                                                        try {
                                                            HapticHelper.performHaptic(context, HapticType.LIGHT)
                                                            val url = notif.actionUrl.trim()
                                                            val validUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
                                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validUrl))
                                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                            context.startActivity(intent)
                                                        } catch (_: Exception) {}
                                                    }
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                    ) {
                                                        Text(
                                                            text = notif.actionButtonText ?: "Open Link",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                            } else if (notif.actionRoute != null) {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = Color.White.copy(alpha = 0.1f),
                                                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
                                                    modifier = Modifier.clickable {
                                                        HapticHelper.performHaptic(context, HapticType.LIGHT)
                                                        onDismiss()
                                                        onNavigate(notif.actionRoute)
                                                    }
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                    ) {
                                                        Text(
                                                            text = notif.actionButtonText ?: "View Details",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                            )
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(12.dp)
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
}
