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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import kotlin.math.sin

enum class PuppetLevel(val title: String, val thresholdHours: Float, val badge: String) {
    DEFAULT("Default Status", 0f, "🌱 0h+"),
    SPARKLE("3+ Hours (Focus)", 3f, "✨ 3h+"),
    STEAM("7+ Hours (Hardcore)", 7f, "♨️ 7h+"),
    FIRE("10+ Hours (God Tier)", 10f, "🔥 10h+")
}

@Composable
fun StudyPuppetAvatar(
    modifier: Modifier = Modifier,
    studyHours: Float = 0f,
    overrideLevel: PuppetLevel? = null,
    strokeColor: Color = MaterialTheme.colorScheme.primary,
    lampGlowColor: Color = Color(0xFFFFD54F),
    isStudying: Boolean = true,
    size: Dp = 120.dp
) {
    val level = overrideLevel ?: when {
        studyHours >= 10f -> PuppetLevel.FIRE
        studyHours >= 7f -> PuppetLevel.STEAM
        studyHours >= 3f -> PuppetLevel.SPARKLE
        else -> PuppetLevel.DEFAULT
    }

    // Infinite animation transitions for lively puppets
    val infiniteTransition = rememberInfiniteTransition(label = "puppetAnim")

    // Head bobbing / breathing animation
    val breathOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isStudying) 3f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    // Bandana ribbon flutter
    val ribbonFlutter by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ribbon"
    )

    // Steam rise progress (0f..1f)
    val steamProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "steam"
    )

    // Flame flicker height & wiggle
    val flameFlicker by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame"
    )

    // Sparkle pulse
    val sparklePulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val strokePx = (w * 0.024f).coerceIn(2.5f, 6.5f)

            // 1. Draw Desk
            drawDesk(w, h, strokeColor, strokePx)

            // 2. Draw Lamp & Glow Beam
            drawStudyLamp(w, h, strokeColor, lampGlowColor, strokePx, isStudying)

            // 3. Draw Student (Head, Torso, Bandana)
            drawStudent(w, h, strokeColor, strokePx, breathOffset, ribbonFlutter)

            // 4. Draw Level Specific Aura / Effects
            when (level) {
                PuppetLevel.DEFAULT -> {
                    // Soft desk glow
                }
                PuppetLevel.SPARKLE -> {
                    drawSparkles(w, h, strokeColor, sparklePulse)
                }
                PuppetLevel.STEAM -> {
                    drawRisingSteam(w, h, strokeColor, steamProgress, breathOffset)
                }
                PuppetLevel.FIRE -> {
                    drawFlames(w, h, strokeColor, flameFlicker, breathOffset)
                }
            }
        }
    }
}

// 1. Desk Drawing
private fun DrawScope.drawDesk(w: Float, h: Float, color: Color, strokePx: Float) {
    val tableY = h * 0.70f
    val legBottom = h * 0.92f
    val leftX = w * 0.18f
    val rightX = w * 0.82f

    // Table Top Horizontal
    drawLine(
        color = color,
        start = Offset(leftX, tableY),
        end = Offset(rightX, tableY),
        strokeWidth = strokePx * 1.2f,
        cap = StrokeCap.Round
    )

    // Table Top Thickness line
    drawLine(
        color = color,
        start = Offset(leftX + w * 0.04f, tableY + h * 0.035f),
        end = Offset(rightX - w * 0.04f, tableY + h * 0.035f),
        strokeWidth = strokePx,
        cap = StrokeCap.Round
    )

    // Left Leg
    drawLine(
        color = color,
        start = Offset(leftX + w * 0.06f, tableY),
        end = Offset(leftX + w * 0.06f, legBottom),
        strokeWidth = strokePx,
        cap = StrokeCap.Round
    )

    // Right Leg
    drawLine(
        color = color,
        start = Offset(rightX - w * 0.06f, tableY),
        end = Offset(rightX - w * 0.06f, legBottom),
        strokeWidth = strokePx,
        cap = StrokeCap.Round
    )

    // Crossbar
    drawLine(
        color = color,
        start = Offset(leftX + w * 0.06f, h * 0.83f),
        end = Offset(rightX - w * 0.06f, h * 0.83f),
        strokeWidth = strokePx * 0.9f,
        cap = StrokeCap.Round
    )
}

// 2. Study Lamp & Beam Drawing
private fun DrawScope.drawStudyLamp(
    w: Float,
    h: Float,
    strokeColor: Color,
    glowColor: Color,
    strokePx: Float,
    isStudying: Boolean
) {
    val lampBaseX = w * 0.70f
    val lampBaseY = h * 0.70f
    val lampTopY = h * 0.44f

    // Lamp Base
    drawLine(
        color = strokeColor,
        start = Offset(lampBaseX - w * 0.04f, lampBaseY),
        end = Offset(lampBaseX + w * 0.04f, lampBaseY),
        strokeWidth = strokePx * 1.2f,
        cap = StrokeCap.Round
    )

    // Lamp Stem
    drawLine(
        color = strokeColor,
        start = Offset(lampBaseX, lampBaseY),
        end = Offset(lampBaseX, lampTopY + h * 0.06f),
        strokeWidth = strokePx,
        cap = StrokeCap.Round
    )

    // Lamp Neck Angled
    drawLine(
        color = strokeColor,
        start = Offset(lampBaseX, lampTopY + h * 0.06f),
        end = Offset(lampBaseX - w * 0.03f, lampTopY),
        strokeWidth = strokePx,
        cap = StrokeCap.Round
    )

    // Lamp Trapezoid Shade
    val shadePath = Path().apply {
        moveTo(lampBaseX - w * 0.07f, lampTopY + h * 0.045f)
        lineTo(lampBaseX + w * 0.01f, lampTopY + h * 0.045f)
        lineTo(lampBaseX - w * 0.01f, lampTopY - h * 0.01f)
        lineTo(lampBaseX - w * 0.05f, lampTopY - h * 0.01f)
        close()
    }

    drawPath(
        path = shadePath,
        color = strokeColor,
        style = Stroke(width = strokePx, join = StrokeJoin.Round)
    )

    // Translucent light cone beam onto desk
    if (isStudying) {
        val lightBeam = Path().apply {
            moveTo(lampBaseX - w * 0.07f, lampTopY + h * 0.045f)
            lineTo(lampBaseX + w * 0.01f, lampTopY + h * 0.045f)
            lineTo(w * 0.82f, h * 0.70f)
            lineTo(w * 0.42f, h * 0.70f)
            close()
        }

        drawPath(
            path = lightBeam,
            brush = Brush.verticalGradient(
                colors = listOf(
                    glowColor.copy(alpha = 0.25f),
                    glowColor.copy(alpha = 0.03f)
                ),
                startY = lampTopY,
                endY = h * 0.70f
            )
        )
    }
}

// 3. Student (Head, Torso, Bandana)
private fun DrawScope.drawStudent(
    w: Float,
    h: Float,
    color: Color,
    strokePx: Float,
    breathOffset: Float,
    ribbonFlutter: Float
) {
    val headCenterX = w * 0.38f
    val headCenterY = h * 0.43f + breathOffset
    val headRadius = w * 0.10f

    // Torso / Shoulders (Hunched over study posture)
    val bodyPath = Path().apply {
        moveTo(w * 0.24f, h * 0.70f)
        cubicTo(
            w * 0.24f, h * 0.54f + breathOffset,
            w * 0.34f, h * 0.50f + breathOffset,
            headCenterX, h * 0.52f + breathOffset
        )
        cubicTo(
            w * 0.44f, h * 0.50f + breathOffset,
            w * 0.53f, h * 0.54f + breathOffset,
            w * 0.53f, h * 0.70f
        )
    }

    drawPath(
        path = bodyPath,
        color = color,
        style = Stroke(width = strokePx * 1.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Arms bent on desk
    drawLine(
        color = color,
        start = Offset(w * 0.26f, h * 0.66f + breathOffset * 0.5f),
        end = Offset(w * 0.48f, h * 0.69f),
        strokeWidth = strokePx,
        cap = StrokeCap.Round
    )

    // Head Circle
    drawCircle(
        color = color,
        center = Offset(headCenterX, headCenterY),
        radius = headRadius,
        style = Stroke(width = strokePx)
    )

    // Focus Bandana Headband across forehead
    val bandY = headCenterY - headRadius * 0.15f
    drawLine(
        color = color,
        start = Offset(headCenterX - headRadius * 0.95f, bandY),
        end = Offset(headCenterX + headRadius * 0.95f, bandY),
        strokeWidth = strokePx * 1.2f,
        cap = StrokeCap.Round
    )

    // Bandana ribbons fluttering at the back of head
    val ribbonStartX = headCenterX - headRadius * 0.95f
    val ribbon1 = Path().apply {
        moveTo(ribbonStartX, bandY)
        quadraticTo(
            ribbonStartX - w * 0.05f + ribbonFlutter,
            bandY - h * 0.02f,
            ribbonStartX - w * 0.09f + ribbonFlutter,
            bandY - h * 0.04f
        )
    }
    val ribbon2 = Path().apply {
        moveTo(ribbonStartX, bandY)
        quadraticTo(
            ribbonStartX - w * 0.05f - ribbonFlutter,
            bandY + h * 0.02f,
            ribbonStartX - w * 0.08f - ribbonFlutter,
            bandY + h * 0.04f
        )
    }

    drawPath(path = ribbon1, color = color, style = Stroke(width = strokePx * 0.9f, cap = StrokeCap.Round))
    drawPath(path = ribbon2, color = color, style = Stroke(width = strokePx * 0.9f, cap = StrokeCap.Round))
}

// 4. Sparkles Effect (3+ Hours)
private fun DrawScope.drawSparkles(w: Float, h: Float, color: Color, pulse: Float) {
    val sparklePositions = listOf(
        Offset(w * 0.22f, h * 0.28f),
        Offset(w * 0.28f, h * 0.22f),
        Offset(w * 0.35f, h * 0.18f),
        Offset(w * 0.44f, h * 0.21f)
    )

    sparklePositions.forEachIndexed { index, pos ->
        val scale = if (index % 2 == 0) pulse else (1.4f - pulse)
        val s = (w * 0.035f) * scale

        // 4-point cross star sparkle
        drawLine(
            color = color.copy(alpha = scale.coerceIn(0.3f, 1f)),
            start = Offset(pos.x - s, pos.y),
            end = Offset(pos.x + s, pos.y),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = color.copy(alpha = scale.coerceIn(0.3f, 1f)),
            start = Offset(pos.x, pos.y - s),
            end = Offset(pos.x, pos.y + s),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

// 5. Rising Steam Effect (7+ Hours)
private fun DrawScope.drawRisingSteam(w: Float, h: Float, color: Color, progress: Float, breathOffset: Float) {
    val headCenterX = w * 0.38f
    val baseSteamY = h * 0.30f + breathOffset

    val steamColumns = listOf(
        headCenterX - w * 0.045f,
        headCenterX,
        headCenterX + w * 0.045f
    )

    steamColumns.forEachIndexed { i, startX ->
        val yOffset = (progress * h * 0.12f)
        val currentBaseY = baseSteamY - yOffset + (i * h * 0.03f)
        val alpha = (1f - (yOffset / (h * 0.12f))).coerceIn(0.15f, 0.95f)

        val steamPath = Path().apply {
            moveTo(startX, currentBaseY)
            cubicTo(
                startX + w * 0.02f, currentBaseY - h * 0.03f,
                startX - w * 0.02f, currentBaseY - h * 0.06f,
                startX + w * 0.01f, currentBaseY - h * 0.09f
            )
        }

        drawPath(
            path = steamPath,
            color = color.copy(alpha = alpha),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

// 6. Blazing Flames Effect (10+ Hours)
private fun DrawScope.drawFlames(w: Float, h: Float, color: Color, flameFlicker: Float, breathOffset: Float) {
    val headCenterX = w * 0.38f
    val headCenterY = h * 0.34f + breathOffset

    val flameHeight = h * 0.16f * flameFlicker
    val flameWidth = w * 0.14f

    val flamePath = Path().apply {
        moveTo(headCenterX - flameWidth, headCenterY + h * 0.02f)
        // Left tongue
        quadraticTo(
            headCenterX - flameWidth * 0.8f, headCenterY - flameHeight * 0.6f,
            headCenterX - flameWidth * 0.5f, headCenterY - flameHeight * 0.4f
        )
        // Center top tongue
        quadraticTo(
            headCenterX, headCenterY - flameHeight,
            headCenterX + flameWidth * 0.3f, headCenterY - flameHeight * 0.5f
        )
        // Right tongue
        quadraticTo(
            headCenterX + flameWidth * 0.7f, headCenterY - flameHeight * 0.7f,
            headCenterX + flameWidth, headCenterY + h * 0.02f
        )
        close()
    }

    // Outer flame glow
    drawPath(
        path = flamePath,
        color = Color(0xFFFFD54F).copy(alpha = 0.35f),
        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Inner fiery stroke
    drawPath(
        path = flamePath,
        color = color,
        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

// ================= PUPPET SELECTOR & PREVIEW DIALOG =================

@Composable
fun PuppetPreviewDialog(
    currentHours: Float,
    hazeState: HazeState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var previewLevel by remember { mutableStateOf<PuppetLevel?>(null) }
    val themePrimary = MaterialTheme.colorScheme.primary

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF14141A),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "STUDY PUPPET STATUS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    )
                )

                Text(
                    text = "Your puppet evolves automatically as you study more hours today!",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 11.5.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // Large Animated Live Preview of Selected Puppet
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, themePrimary.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    StudyPuppetAvatar(
                        studyHours = currentHours,
                        overrideLevel = previewLevel,
                        strokeColor = themePrimary,
                        size = 120.dp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 4 Puppet Cards Grid (Default, 3+h, 7+h, 10+h)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PuppetLevel.entries.forEach { level ->
                        val isSelected = (previewLevel ?: when {
                            currentHours >= 10f -> PuppetLevel.FIRE
                            currentHours >= 7f -> PuppetLevel.STEAM
                            currentHours >= 3f -> PuppetLevel.SPARKLE
                            else -> PuppetLevel.DEFAULT
                        }) == level

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) themePrimary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 0.5.dp,
                                if (isSelected) themePrimary else Color.White.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    HapticHelper.performHaptic(context, HapticType.SELECTION)
                                    previewLevel = level
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                StudyPuppetAvatar(
                                    overrideLevel = level,
                                    strokeColor = if (isSelected) themePrimary else Color.White.copy(alpha = 0.8f),
                                    size = 46.dp,
                                    isStudying = true
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = level.badge,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) themePrimary else Color.White.copy(alpha = 0.7f)
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Close Button
                Button(
                    onClick = {
                        HapticHelper.performHaptic(context, HapticType.LIGHT)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themePrimary)
                ) {
                    Text(
                        text = "Close",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
