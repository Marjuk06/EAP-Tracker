package com.marjuk.eaptracker.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FloatingNavbar(
    items: List<NavigationItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(bottom = 32.dp, start = 12.dp, end = 12.dp)
            .height(56.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp) // Exact "Connected" gap from video
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            
            // 1. Morph the corner radius (Professional Morph)
            val cornerRadius by animateDpAsState(
                targetValue = if (isSelected) 28.dp else 12.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy, 
                    stiffness = Spring.StiffnessLow
                ),
                label = "cornerRadius"
            )

            // 2. Animate background color (Solid colors now)
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) 
                    MaterialTheme.colorScheme.onSurfaceVariant 
                else 
                    MaterialTheme.colorScheme.surfaceVariant, // Removed alpha for solid look
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "backgroundColor"
            )

            // 3. Animate content color
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) 
                    MaterialTheme.colorScheme.surface 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "contentColor"
            )

            // 4. Animate layout weight for smooth expansion
            val weight by animateFloatAsState(
                targetValue = if (isSelected) 1.5f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy, 
                    stiffness = Spring.StiffnessLow
                ),
                label = "weight"
            )

            val context = androidx.compose.ui.platform.LocalContext.current
            Box(
                modifier = Modifier
                    .weight(weight)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(backgroundColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        HapticHelper.performHaptic(context, HapticType.SELECTION)
                        onNavigate(item.route)
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .animateContentSize()
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        modifier = Modifier.size(20.dp),
                        tint = contentColor
                    )
                    if (isSelected) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 12.sp,
                            color = contentColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
