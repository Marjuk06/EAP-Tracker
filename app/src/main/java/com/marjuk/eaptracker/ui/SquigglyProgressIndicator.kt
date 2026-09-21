package com.marjuk.eaptracker.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun SquigglyProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.outlineVariant,
    strokeWidth: Float = 3.5f,
    waveAmplitudeDp: Float = 1.8f,
    waveLengthDp: Float = 14f
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "progress"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val waveAmplitude = waveAmplitudeDp.dp.toPx()
        val waveLength = waveLengthDp.dp.toPx()

        // Function to create squiggly path
        fun createWavyPath(endX: Float): Path {
            val path = Path()
            if (endX <= 0) return path
            
            path.moveTo(0f, centerY)
            var x = 0f
            while (x < endX) {
                val y = centerY + waveAmplitude * sin((x / waveLength) * 2 * Math.PI.toFloat()).toFloat()
                path.lineTo(x, y)
                x += 1.5f
            }
            return path
        }

        // Draw Track (Lighter wavy line)
        drawPath(
            path = createWavyPath(width),
            color = trackColor.copy(alpha = 0.35f),
            style = Stroke(width = strokeWidth.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Draw Progress (Solid wavy line)
        if (animatedProgress > 0) {
            drawPath(
                path = createWavyPath(width * animatedProgress),
                color = color,
                style = Stroke(width = (strokeWidth + 0.5f).dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}
