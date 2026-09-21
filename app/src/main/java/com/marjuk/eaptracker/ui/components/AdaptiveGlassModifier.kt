package com.marjuk.eaptracker.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

/**
 * Adaptive Glass & Solid Surface Card Modifier:
 * - On Android 12+ (API 31+): Renders genuine optical frosted glass using Haze blur.
 * - On Below Android 12 (API 24 - 30): Renders a crisp, solid cyber dark surface card matching the theme.
 */
fun Modifier.adaptiveGlass(
    hazeState: HazeState?,
    shape: Shape,
    style: HazeStyle? = null,
    borderWidth: Dp = 0.5.dp,
    borderColor: Color = Color.White.copy(alpha = 0.12f),
    solidBackgroundColor: Color? = null
): Modifier = composed {
    val isAndroid12OrAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    if (isAndroid12OrAbove && hazeState != null) {
        val effectiveStyle = style ?: HazeStyle(
            backgroundColor = MaterialTheme.colorScheme.background,
            tint = HazeTint(Color.Black.copy(alpha = 0.15f)),
            blurRadius = 20.dp
        )
        this
            .hazeChild(state = hazeState, shape = shape, style = effectiveStyle)
            .border(borderWidth, borderColor, shape)
    } else {
        val bgColor = solidBackgroundColor ?: MaterialTheme.colorScheme.surface
        this
            .background(bgColor, shape)
            .border(borderWidth, borderColor, shape)
    }
}
