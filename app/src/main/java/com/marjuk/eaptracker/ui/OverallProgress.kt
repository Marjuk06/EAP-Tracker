package com.marjuk.eaptracker.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import com.marjuk.eaptracker.LocalHazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun OverallProgress(
    completedChapters: Int,
    totalChapters: Int,
    modifier: Modifier = Modifier
) {
    val targetProgress = if (totalChapters > 0) completedChapters.toFloat() / totalChapters else 0f
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(targetProgress) {
        // "Acceleration" animation: Fill to 100% then settle back to actual progress
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
        )
        animatedProgress.animateTo(
            targetValue = targetProgress,
            animationSpec = tween(durationMillis = 800)
        )
    }

    val hazeState = LocalHazeState.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.large)
            .hazeChild(
                state = hazeState, 
                shape = MaterialTheme.shapes.large,
                style = HazeStyle(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    tint = HazeTint(Color.Black.copy(alpha = 0.15f)), 
                    blurRadius = 20.dp
                )
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), MaterialTheme.shapes.large)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your Progress",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$completedChapters / $totalChapters chapters completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val outlineColor = MaterialTheme.colorScheme.outlineVariant

                Canvas(modifier = Modifier.size(70.dp)) {
                    val strokeWidth = 6.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = Offset(size.width / 2, size.height / 2)
                    
                    val startAngle = 140f
                    val totalSweep = 260f
                    
                    // Function to draw a wavy arc
                    fun drawWavyArc(sweep: Float, color: Color, width: Float) {
                        val path = Path()
                        val points = 100
                        val waveAmplitude = 1.2.dp.toPx() // Thinner wave for arc
                        val waveFrequency = 18f // More frequent waves
                        
                        for (i in 0..points) {
                            val angleDeg = startAngle + (sweep * i / points)
                            val angleRad = Math.toRadians(angleDeg.toDouble())
                            
                            // Add wave displacement to radius
                            val displacement = waveAmplitude * sin(i.toDouble() / points * sweep / 360 * 2 * PI * waveFrequency)
                            val currentRadius = radius + displacement.toFloat()
                            
                            val x = center.x + currentRadius * cos(angleRad).toFloat()
                            val y = center.y + currentRadius * sin(angleRad).toFloat()
                            
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        
                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }

                    // Draw Background Wavy Arc
                    drawWavyArc(totalSweep, outlineColor.copy(alpha = 0.3f), strokeWidth)
                    
                    // Draw Progress Wavy Arc
                    if (animatedProgress.value > 0) {
                        drawWavyArc(totalSweep * animatedProgress.value, primaryColor, strokeWidth + 1.dp.toPx())
                    }
                }
                
                Text(
                    text = "${(animatedProgress.value * 100).toInt()}%",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
