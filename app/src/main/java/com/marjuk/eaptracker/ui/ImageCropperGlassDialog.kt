package com.marjuk.eaptracker.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ImageCropperGlassDialog(
    hazeState: HazeState,
    imageUri: Uri,
    onDismiss: () -> Unit,
    onCropSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Transform states
    var userScale by remember { mutableFloatStateOf(1f) }
    var userOffset by remember { mutableStateOf(Offset.Zero) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var viewportSizePx by remember { mutableStateOf(IntSize.Zero) }

    // Load and orient bitmap
    LaunchedEffect(imageUri) {
        isLoading = true
        withContext(Dispatchers.IO) {
            loadedBitmap = decodeAndOrientBitmap(context, imageUri)
        }
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp))
                .hazeChild(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = MaterialTheme.colorScheme.background,
                        tint = HazeTint(Color.Black.copy(alpha = 0.45f)),
                        blurRadius = 24.dp
                    )
                )
                .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Crop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "CROP AVATAR",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            )
                            Text(
                                text = "1:1 Square & Circular Fit",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading || loadedBitmap == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else {
                    val bmp = loadedBitmap!!

                    // 1:1 Viewport Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF0F141C))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                            .onSizeChanged { viewportSizePx = it }
                            .clipToBounds()
                            .pointerInput(bmp, rotationDegrees) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    userScale = (userScale * zoom).coerceIn(1f, 4f)
                                    userOffset += pan
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Display image with gestures applied
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Crop Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = userScale
                                    scaleY = userScale
                                    translationX = userOffset.x
                                    translationY = userOffset.y
                                    rotationZ = rotationDegrees
                                }
                        )

                        // Frosted Dark Overlay Mask with 1:1 circle/square guide
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            val cropRadius = (min(canvasWidth, canvasHeight) * 0.92f) / 2f
                            val centerOffset = Offset(canvasWidth / 2f, canvasHeight / 2f)

                            // Subtle rule-of-thirds grid inside circle
                            val thirdX = cropRadius * 2f / 3f
                            val thirdY = cropRadius * 2f / 3f
                            val startX = centerOffset.x - cropRadius
                            val startY = centerOffset.y - cropRadius

                            // Grid lines
                            drawLine(
                                color = Color.White.copy(alpha = 0.2f),
                                start = Offset(startX + thirdX, startY),
                                end = Offset(startX + thirdX, startY + cropRadius * 2f),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.2f),
                                start = Offset(startX + thirdX * 2f, startY),
                                end = Offset(startX + thirdX * 2f, startY + cropRadius * 2f),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.2f),
                                start = Offset(startX, startY + thirdY),
                                end = Offset(startX + cropRadius * 2f, startY + thirdY),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.2f),
                                start = Offset(startX, startY + thirdY * 2f),
                                end = Offset(startX + cropRadius * 2f, startY + thirdY * 2f),
                                strokeWidth = 1f
                            )

                            // Glowing cyan circular ring
                            drawCircle(
                                brush = Brush.sweepGradient(
                                    listOf(
                                        Color(0xFF00E5FF),
                                        Color(0xFF7C4DFF),
                                        Color(0xFF00E5FF)
                                    )
                                ),
                                radius = cropRadius,
                                center = centerOffset,
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Zoom Controls & Quick Action Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { userScale = max(1f, userScale - 0.25f) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White)
                        }

                        Slider(
                            value = userScale,
                            onValueChange = { userScale = it },
                            valueRange = 1f..4f,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            )
                        )

                        IconButton(
                            onClick = { userScale = min(4f, userScale + 0.25f) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White)
                        }

                        Text(
                            text = "${(userScale * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.width(42.dp),
                            textAlign = TextAlign.End
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Rotation & Reset Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { rotationDegrees = (rotationDegrees + 90f) % 360f },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.2f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rotate 90°", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                userScale = 1f
                                userOffset = Offset.Zero
                                rotationDegrees = 0f
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.2f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.8f))
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom Actions: Cancel & Crop & Save
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.9f))
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    val cropped = cropBitmap(
                                        sourceBitmap = bmp,
                                        rotation = rotationDegrees,
                                        scale = userScale,
                                        panOffset = userOffset,
                                        viewportSize = viewportSizePx
                                    )
                                    if (cropped != null) {
                                        val savedPath = ProfileRepository.saveCroppedAvatar(context, cropped)
                                        withContext(Dispatchers.Main) {
                                            if (savedPath != null) {
                                                onCropSuccess(savedPath)
                                                Toast.makeText(context, "Avatar updated successfully!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Failed to save cropped image", Toast.LENGTH_SHORT).show()
                                            }
                                            onDismiss()
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Error cropping image", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1.3f).height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Avatar", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Decodes Bitmap and corrects orientation based on EXIF metadata.
 */
private fun decodeAndOrientBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val exif = ExifInterface(inputStream)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        inputStream.close()

        val rawBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                decoder.isMutableRequired = true
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } ?: return null

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        }

        if (!matrix.isIdentity) {
            Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
        } else {
            rawBitmap
        }
    } catch (e: Exception) {
        e.printStackTrace()
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Performs exact pixel crop based on rotation, scale, offset, and 1:1 square bounds.
 */
private fun cropBitmap(
    sourceBitmap: Bitmap,
    rotation: Float,
    scale: Float,
    panOffset: Offset,
    viewportSize: IntSize
): Bitmap? {
    return try {
        // 1. Rotate base bitmap if needed
        val baseBitmap = if (rotation != 0f) {
            val rotMatrix = Matrix().apply { postRotate(rotation) }
            Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.width, sourceBitmap.height, rotMatrix, true)
        } else {
            sourceBitmap
        }

        val bmpW = baseBitmap.width.toFloat()
        val bmpH = baseBitmap.height.toFloat()

        val vpSize = if (viewportSize.width > 0) viewportSize.width.toFloat() else 600f

        // ContentScale.Crop base ratio (fill viewport)
        val baseScale = max(vpSize / bmpW, vpSize / bmpH)
        val effectiveScale = baseScale * scale

        // Displayed dimensions
        val dispW = bmpW * effectiveScale
        val dispH = bmpH * effectiveScale

        // Center of viewport relative to displayed image
        val viewCenterX = (dispW / 2f) - panOffset.x
        val viewCenterY = (dispH / 2f) - panOffset.y

        // Viewport rect in displayed image coordinates
        val viewLeft = viewCenterX - (vpSize / 2f)
        val viewTop = viewCenterY - (vpSize / 2f)

        // Convert to bitmap pixel coordinates
        val cropX = (viewLeft / effectiveScale).coerceIn(0f, bmpW - 1f)
        val cropY = (viewTop / effectiveScale).coerceIn(0f, bmpH - 1f)
        val cropDim = (vpSize / effectiveScale).coerceAtMost(min(bmpW - cropX, bmpH - cropY))

        val finalDim = max(1, cropDim.roundToInt())
        val cropped = Bitmap.createBitmap(
            baseBitmap,
            cropX.roundToInt().coerceIn(0, baseBitmap.width - finalDim),
            cropY.roundToInt().coerceIn(0, baseBitmap.height - finalDim),
            finalDim,
            finalDim
        )

        // Scale down to a crisp 512x512 square if larger
        if (cropped.width > 512) {
            Bitmap.createScaledBitmap(cropped, 512, 512, true)
        } else {
            cropped
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
