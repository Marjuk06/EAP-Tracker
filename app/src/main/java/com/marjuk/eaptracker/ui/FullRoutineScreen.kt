package com.marjuk.eaptracker.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.marjuk.eaptracker.LocalHazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FullRoutineScreen(type: String, onBack: () -> Unit) {
    val routineData = com.marjuk.eaptracker.ui.admin.DynamicRoutineRepository.getRoutines(type)
    val title = "${type.uppercase()} ROUTINE"
    var showTopics by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                ),
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            val cornerRadius by animateDpAsState(targetValue = if (showTopics) 24.dp else 4.dp, label = "buttonShape")
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(if (showTopics) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f))
                    .clickable { showTopics = !showTopics }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(routineData) { item ->
                val hazeState = LocalHazeState.current
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .hazeChild(
                            state = hazeState, 
                            shape = RoundedCornerShape(24.dp),
                            style = HazeStyle(
                                backgroundColor = MaterialTheme.colorScheme.background,
                                tint = HazeTint(Color.Black.copy(alpha = 0.15f)),
                                blurRadius = 20.dp
                            )
                        )
                        .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                ) {
                    // Content
                    Box(modifier = Modifier.padding(16.dp)) {
                        RoutineRow(item, showTopics = showTopics)
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(120.dp)) }
        }
    }
}
