package com.example.pdfreader.presentation.screens.reader

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pdfreader.domain.model.AppTheme
import kotlinx.coroutines.launch

@Composable
fun CurlPageEffect(
    currentPage: Int,
    pageCount: Int,
    onPageChanged: (Int) -> Unit,
    theme: AppTheme,
    zoomLevel: Float,
    onZoomChanged: (Float) -> Unit,
    onRenderPage: (Int, (Bitmap?) -> Unit) -> Unit,
    onTap: () -> Unit,
    onLeftDoubleTap: () -> Unit = {},
    onRightDoubleTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var width by remember { mutableStateOf(0) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragDirection by remember { mutableStateOf(0) } // -1 for next (left drag), 1 for prev (right drag)

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    val cameraDist = 12f * density

    val animProgress = remember { Animatable(0f) }

    var currentBmp by remember(currentPage) { mutableStateOf<Bitmap?>(null) }
    var nextBmp by remember(currentPage) { mutableStateOf<Bitmap?>(null) }
    var prevBmp by remember(currentPage) { mutableStateOf<Bitmap?>(null) }

    var currentScale by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(currentPage) {
        onRenderPage(currentPage) { currentBmp = it }
        if (currentPage < pageCount - 1) {
            onRenderPage(currentPage + 1) { nextBmp = it }
        }
        if (currentPage > 0) {
            onRenderPage(currentPage - 1) { prevBmp = it }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { width = it.width }
            .pointerInput(currentPage, pageCount, currentScale) {
                // Disable drag to turn page if zoomed in
                if (currentScale > 1f) return@pointerInput

                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                        offsetX = 0f
                        scope.launch { animProgress.snapTo(0f) }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        if (width > 0) {
                            offsetX += dragAmount
                            val progress = (-offsetX / width).coerceIn(-1f, 1f)
                            if (progress > 0 && currentPage < pageCount - 1) {
                                dragDirection = -1
                                scope.launch { animProgress.snapTo(progress) }
                            } else if (progress < 0 && currentPage > 0) {
                                dragDirection = 1
                                scope.launch { animProgress.snapTo(-progress) }
                            }
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        val progress = animProgress.value
                        if (progress > 0.35f) {
                            scope.launch {
                                animProgress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = spring(stiffness = 200f)
                                )
                                if (dragDirection == -1) {
                                    onPageChanged(currentPage + 1)
                                } else if (dragDirection == 1) {
                                    onPageChanged(currentPage - 1)
                                }
                                offsetX = 0f
                                animProgress.snapTo(0f)
                                dragDirection = 0
                            }
                        } else {
                            scope.launch {
                                animProgress.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(stiffness = 200f)
                                )
                                offsetX = 0f
                                dragDirection = 0
                            }
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        scope.launch {
                            animProgress.animateTo(0f)
                            offsetX = 0f
                            dragDirection = 0
                        }
                    }
                )
            }
    ) {
        val progress = animProgress.value

        // Underneath Page (renders flat)
        if (dragDirection == -1 && currentPage < pageCount - 1) {
            // Turning forward: Underneath page is next page
            ZoomablePage(
                bitmap = nextBmp,
                pageIndex = currentPage + 1,
                zoomLevel = 1f,
                onZoomChanged = {},
                onTap = onTap
            )
        } else if (dragDirection == 1 && currentPage > 0) {
            // Turning backward: Underneath page is current page
            ZoomablePage(
                bitmap = currentBmp,
                pageIndex = currentPage,
                zoomLevel = 1f,
                onZoomChanged = {},
                onTap = onTap
            )
        }

        // Curling Page (renders with 3D rotation)
        if (dragDirection != 0) {
            val rotationY = if (dragDirection == -1) {
                -180f * progress
            } else {
                180f * (1f - progress)
            }

            val isFlipped = kotlin.math.abs(rotationY) > 90f
            val curlingBmp = if (dragDirection == -1) currentBmp else prevBmp
            val curlingIndex = if (dragDirection == -1) currentPage else currentPage - 1

            val backColor = when (theme) {
                AppTheme.SEPIA -> Color(0xFFEDE0CC)
                AppTheme.DARK -> Color(0xFF2A2A2A)
                AppTheme.AMOLED -> Color(0xFF121212)
                else -> Color(0xFFFFFFFF)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        this.rotationY = rotationY
                        this.cameraDistance = cameraDist
                        this.transformOrigin = TransformOrigin(0f, 0.5f)
                        this.translationX = -50f * (if (dragDirection == -1) progress else (1f - progress))
                    }
            ) {
                if (isFlipped) {
                    // Back side of the curling page
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(backColor)
                    )
                    // Edge shadow on curling side (left edge because flipped)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(16.dp)
                            .align(Alignment.CenterStart)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.15f), Color.Transparent)
                                )
                            )
                    )
                } else {
                    // Front side of the curling page
                    ZoomablePage(
                        bitmap = curlingBmp,
                        pageIndex = curlingIndex,
                        zoomLevel = 1f,
                        onZoomChanged = {},
                        onTap = onTap
                    )
                    // Edge shadow on curling side (right edge)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(16.dp)
                            .align(Alignment.CenterEnd)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.15f))
                                )
                            )
                    )
                }
            }
        } else {
            // Idle state: just render the current page flat
            ZoomablePage(
                bitmap = currentBmp,
                pageIndex = currentPage,
                zoomLevel = zoomLevel,
                onZoomChanged = {
                    currentScale = it
                    onZoomChanged(it)
                },
                onTap = onTap,
                onLeftDoubleTap = onLeftDoubleTap,
                onRightDoubleTap = onRightDoubleTap
            )
        }
    }
}

@Composable
fun ZoomablePage(
    bitmap: Bitmap?,
    pageIndex: Int,
    zoomLevel: Float,
    onZoomChanged: (Float) -> Unit,
    onTap: () -> Unit,
    onLeftDoubleTap: () -> Unit = {},
    onRightDoubleTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var scale by remember(bitmap) { mutableFloatStateOf(zoomLevel) }
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }
    var width by remember { mutableStateOf(0) }

    LaunchedEffect(zoomLevel) {
        scale = zoomLevel
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
        scale = newScale
        onZoomChanged(newScale)
        if (newScale > 1f) {
            offset = Offset(
                x = offset.x + panChange.x,
                y = offset.y + panChange.y
            )
        } else {
            offset = Offset.Zero
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { width = it.width }
            .pointerInput(bitmap, scale, width) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { pressOffset ->
                        if (width > 0) {
                            val ratio = pressOffset.x / width
                            if (ratio < 0.2f) {
                                onLeftDoubleTap()
                            } else if (ratio > 0.8f) {
                                onRightDoubleTap()
                            } else {
                                val newScale = if (scale > 1f) 1f else 2f
                                scale = newScale
                                onZoomChanged(newScale)
                                offset = Offset.Zero
                            }
                        } else {
                            val newScale = if (scale > 1f) 1f else 2f
                            scale = newScale
                            onZoomChanged(newScale)
                            offset = Offset.Zero
                        }
                    }
                )
            }
            .transformable(state = transformState)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap == null) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                // Subtle page-edge shadow on the right side (8dp wide, 0 -> 15% black alpha)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(8.dp)
                        .align(Alignment.CenterEnd)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.15f))
                            )
                        )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CurlPageEffectPreview() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("CurlPageEffect Preview", fontSize = 18.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ZoomablePagePreview() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("ZoomablePage Preview", fontSize = 18.sp)
        }
    }
}
