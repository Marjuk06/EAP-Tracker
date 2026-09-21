package com.marjuk.eaptracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.marjuk.eaptracker.LocalHazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.platform.LocalContext

@Composable
fun ChapterExpandableItem(
    chapter: Chapter,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSectionToggle: (Int) -> Unit
) {
    val context = LocalContext.current
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "rotate")
    
    val trailingInnerCorner by animateDpAsState(
        targetValue = if (isExpanded) 28.dp else 4.dp, 
        label = "trailingInnerCorner"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val hazeState = LocalHazeState.current
        
        // Chapter Header (Split Button Style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    HapticHelper.performHaptic(context, HapticType.LIGHT)
                    onToggle()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chapter Name
            val titleShape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp, topEnd = 4.dp, bottomEnd = 4.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(titleShape)
                    .hazeChild(
                        state = hazeState, 
                        shape = titleShape,
                        style = HazeStyle(
                            backgroundColor = MaterialTheme.colorScheme.background,
                            tint = HazeTint(Color.Black.copy(alpha = 0.15f)), 
                            blurRadius = 20.dp
                        )
                    )
                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), titleShape)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = chapter.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Box(modifier = Modifier.size(2.dp))

            // Dropdown Arrow
            val arrowShape = RoundedCornerShape(
                topEnd = 28.dp, 
                bottomEnd = 28.dp, 
                topStart = trailingInnerCorner, 
                bottomStart = trailingInnerCorner
            )
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(arrowShape)
                    .hazeChild(
                        state = hazeState, 
                        shape = arrowShape,
                        style = HazeStyle(
                            backgroundColor = MaterialTheme.colorScheme.background,
                            tint = HazeTint(Color.Black.copy(alpha = 0.15f)), 
                            blurRadius = 20.dp
                        )
                    )
                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), arrowShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Progress Sections (Frosted Glass Effect)
        AnimatedVisibility(visible = isExpanded) {
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .height(48.dp)
                    .animateContentSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                chapter.sections.forEachIndexed { index, section ->
                    val isComp = section.isCompleted
                    
                    // Progress Morph Animation (Using Expressive Spring)
                    val cornerRadius by animateDpAsState(
                        targetValue = if (isComp) 24.dp else 8.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "progressCorner"
                    )

                    val contentColor by animateColorAsState(
                        targetValue = if (isComp) 
                            MaterialTheme.colorScheme.onPrimary 
                        else 
                            Color.White.copy(alpha = 0.85f),
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "contentColor"
                    )

                    val outerShape = when (index) {
                        0 -> RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = cornerRadius, bottomEnd = cornerRadius)
                        chapter.sections.size - 1 -> RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp, topStart = cornerRadius, bottomStart = cornerRadius)
                        else -> RoundedCornerShape(cornerRadius)
                    }

                    val tintColor = if (isComp) 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.75f) 
                    else 
                        Color.Black.copy(alpha = 0.2f)

                    val borderColor = if (isComp) 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) 
                    else 
                        Color.White.copy(alpha = 0.15f)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(outerShape)
                            .hazeChild(
                                state = hazeState,
                                shape = outerShape,
                                style = HazeStyle(
                                    backgroundColor = MaterialTheme.colorScheme.background,
                                    tint = HazeTint(tintColor),
                                    blurRadius = 20.dp
                                )
                            )
                            .border(0.5.dp, borderColor, outerShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                HapticHelper.performHaptic(context, HapticType.SUCCESS)
                                onSectionToggle(index)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = section.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = contentColor
                            )
                            Text(
                                text = section.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    textDecoration = if (isComp) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                fontSize = 9.sp,
                                color = contentColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
