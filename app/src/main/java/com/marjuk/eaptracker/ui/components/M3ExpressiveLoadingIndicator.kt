package com.marjuk.eaptracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * Material Design 3 Expressive Loading Indicator.
 *
 * Morphs seamlessly between expressive geometric polygons (Clover, 4-Point Star,
 * Rounded Square, Soft Pentagon, and Circle) with fluid spring dynamics, continuous
 * 360-degree rotation, and subtle pulsating Material glow.
 */
@Composable
fun M3ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 48.dp,
    strokeWidth: Dp = 3.dp,
    isFilled: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "m3_expressive_loader")

    // Continuous smooth rotation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Morph progress cycling through 5 shape stages (0f to 5f)
    val morphProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "morphProgress"
    )

    // Subtle breathing pulse scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val radius = (size.toPx() / 2f) * 0.78f * pulseScale

            rotate(degrees = rotation, pivot = center) {
                val path = generateMorphedPolygonPath(
                    center = center,
                    baseRadius = radius,
                    stage = morphProgress
                )

                if (isFilled) {
                    drawPath(
                        path = path,
                        brush = Brush.radialGradient(
                            colors = listOf(color, color.copy(alpha = 0.85f)),
                            center = center,
                            radius = radius * 1.2f
                        ),
                        style = Fill
                    )
                } else {
                    // Outer glow
                    drawPath(
                        path = path,
                        color = color.copy(alpha = 0.25f),
                        style = Stroke(
                            width = strokeWidth.toPx() * 2.2f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                    // Core stroke
                    drawPath(
                        path = path,
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                color.copy(alpha = 0.5f),
                                color,
                                color.copy(alpha = 0.9f),
                                color
                            ),
                            center = center
                        ),
                        style = Stroke(
                            width = strokeWidth.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}

/**
 * Generates an expressive morphed polygon path based on fractional stage (0..5).
 * Stage 0: 4-Lobed Clover (Petal)
 * Stage 1: 4-Pointed Star / Burst
 * Stage 2: Soft Rounded Square
 * Stage 3: Smooth Pentagon
 * Stage 4: Circle
 * Stage 5: Wrap back to 0
 */
private fun generateMorphedPolygonPath(
    center: Offset,
    baseRadius: Float,
    stage: Float
): Path {
    val path = Path()
    val numPoints = 120
    val stageIndex = stage.toInt() % 5
    val stageFraction = stage - stage.toInt()

    // Blend between current shape and next shape
    val nextStageIndex = (stageIndex + 1) % 5

    for (i in 0 until numPoints) {
        val angle = (2 * PI * i / numPoints).toFloat()

        val rCurrent = getShapeRadius(angle, stageIndex, baseRadius)
        val rNext = getShapeRadius(angle, nextStageIndex, baseRadius)
        val blendedRadius = rCurrent + (rNext - rCurrent) * stageFraction

        val x = center.x + blendedRadius * cos(angle)
        val y = center.y + blendedRadius * sin(angle)

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()
    return path
}

/**
 * Returns the radial distance at a specific angle for the given polygon stage.
 */
private fun getShapeRadius(angle: Float, shapeType: Int, baseRadius: Float): Float {
    return when (shapeType) {
        0 -> {
            // 4-Lobed Clover / Petal Shape
            val cloverFactor = 0.78f + 0.22f * cos(4f * angle)
            baseRadius * cloverFactor
        }
        1 -> {
            // 4-Pointed Star / Burst (Concave Polygon)
            val starFactor = 0.65f + 0.35f * (abs(cos(2f * angle)).pow(1.6f))
            baseRadius * starFactor
        }
        2 -> {
            // Soft Rounded Square (Superellipse / Squircle)
            val n = 3.2f
            val cosA = abs(cos(angle))
            val sinA = abs(sin(angle))
            val superellipseR = 1f / (cosA.pow(n) + sinA.pow(n)).pow(1f / n)
            baseRadius * superellipseR * 0.88f
        }
        3 -> {
            // Soft Rounded Pentagon
            val pentagonFactor = 0.84f + 0.16f * cos(5f * angle)
            baseRadius * pentagonFactor
        }
        else -> {
            // Smooth Circle
            baseRadius * 0.92f
        }
    }
}
