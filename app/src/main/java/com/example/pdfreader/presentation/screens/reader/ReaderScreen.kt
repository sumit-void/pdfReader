@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.pdfreader.presentation.screens.reader

import android.graphics.Bitmap
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.pdfreader.util.PageColorFilter
import com.example.pdfreader.presentation.screens.reader.CurlPageEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pdfreader.domain.model.AppTheme
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBookmarks: (Long) -> Unit,
    onNavigateToHighlights: (Long) -> Unit,
    onNavigateToToc: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    when (val state = uiState) {
        is ReaderUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Opening book…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        is ReaderUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Unable to open PDF",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    TextButton(onClick = onNavigateBack) {
                        Text("Go Back")
                    }
                }
            }
        }

        is ReaderUiState.Success -> {
            // Keep screen awake
            if (state.keepScreenAwake) {
                DisposableEffect(Unit) {
                    val window = (context as? android.app.Activity)?.window
                    window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    onDispose {
                        window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
            }

            // Brightness
            if (state.brightness >= 0f) {
                DisposableEffect(state.brightness) {
                    val window = (context as? android.app.Activity)?.window
                    val params = window?.attributes
                    params?.screenBrightness = state.brightness
                    window?.attributes = params
                    onDispose {
                        val p = window?.attributes
                        p?.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                        window?.attributes = p
                    }
                }
            }

            ReaderContent(
                state = state,
                viewModel = viewModel,
                onPageChanged = { viewModel.goToPage(it) },
                onToggleControls = { viewModel.toggleControls() },
                onToggleBookmark = { viewModel.toggleBookmark() },
                onBack = onNavigateBack,
                onToc = { onNavigateToToc(state.book.id) },
                onBookmarks = { onNavigateToBookmarks(state.book.id) },
                onHighlights = { onNavigateToHighlights(state.book.id) },
                onSettings = onNavigateToSettings,
                onRenderPage = { page, callback -> viewModel.renderPageForPager(page, callback) },
                onSetScreenWidth = { viewModel.setScreenWidth(it) }
            )
        }
    }
}

@Composable
private fun ReaderContent(
    state: ReaderUiState.Success,
    viewModel: ReaderViewModel,
    onPageChanged: (Int) -> Unit,
    onToggleControls: () -> Unit,
    onToggleBookmark: () -> Unit,
    onBack: () -> Unit,
    onToc: () -> Unit,
    onBookmarks: () -> Unit,
    onHighlights: () -> Unit,
    onSettings: () -> Unit,
    onRenderPage: (Int, (Bitmap?) -> Unit) -> Unit,
    onSetScreenWidth: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    var showSummarySheet by remember { mutableStateOf(false) }

    val density = LocalDensity.current.density
    val hapticFeedback = LocalHapticFeedback.current

    // Bookmark tap scale bounce animation
    var bookmarkTrigger by remember { mutableStateOf(0) }
    val bookmarkScale by animateFloatAsState(
        targetValue = if (bookmarkTrigger % 2 == 1) 1.3f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        finishedListener = {
            if (bookmarkTrigger % 2 == 1) {
                bookmarkTrigger++
            }
        },
        label = "BookmarkScale"
    )

    // Theme-Aware Cross-Fade Animation
    var lastTheme by remember { mutableStateOf(state.theme) }
    val themeChangeProgress = remember { Animatable(1f) }

    LaunchedEffect(state.theme) {
        if (state.theme != lastTheme) {
            themeChangeProgress.snapTo(0f)
            themeChangeProgress.animateTo(1f, animationSpec = tween(400))
            lastTheme = state.theme
        }
    }

    val progress = themeChangeProgress.value
    val oldOverlayColor = PageColorFilter.getOverlayColor(lastTheme)
    val newOverlayColor = PageColorFilter.getOverlayColor(state.theme)

    // Page count flip animation
    var displayedPage by remember { mutableStateOf(state.currentPage) }
    val rotationXAnim = remember { Animatable(0f) }

    LaunchedEffect(state.currentPage) {
        if (state.currentPage != displayedPage) {
            rotationXAnim.animateTo(90f, animationSpec = tween(durationMillis = 120))
            displayedPage = state.currentPage
            rotationXAnim.animateTo(0f, animationSpec = tween(durationMillis = 120))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                when (state.theme) {
                    AppTheme.DARK -> Color(0xFF121212)
                    AppTheme.AMOLED -> Color.Black
                    AppTheme.SEPIA -> Color(0xFFF5ECD7)
                    else -> Color.White
                }
            )
            .onSizeChanged { size ->
                onSetScreenWidth(size.width)
            }
    ) {
        // PDF Pages Pager (using 3D CurlPageEffect)
        CurlPageEffect(
            currentPage = state.currentPage,
            pageCount = state.totalPages,
            onPageChanged = onPageChanged,
            theme = state.theme,
            pageTurnStyle = state.pageTurnStyle,
            readingDirection = state.readingDirection,
            zoomLevel = state.zoomLevel,
            onZoomChanged = { viewModel.updateZoomLevel(it) },
            onRenderPage = onRenderPage,
            onTap = onToggleControls,
            onLeftDoubleTap = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                if (state.readingDirection == com.example.pdfreader.domain.model.ReadingDirection.RTL) {
                    viewModel.nextPage()
                } else {
                    viewModel.previousPage()
                }
            },
            onRightDoubleTap = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                if (state.readingDirection == com.example.pdfreader.domain.model.ReadingDirection.RTL) {
                    viewModel.previousPage()
                } else {
                    viewModel.nextPage()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Box for theme cross-fade
        if (progress < 1f && oldOverlayColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(oldOverlayColor.copy(alpha = oldOverlayColor.alpha * (1f - progress)))
            )
        }
        if (newOverlayColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(newOverlayColor.copy(alpha = newOverlayColor.alpha * progress))
            )
        }

        // Top Control Bar
        AnimatedVisibility(
            visible = state.showControls,
            enter = slideInVertically(animationSpec = tween(250), initialOffsetY = { -it }) + fadeIn(animationSpec = tween(250)),
            exit = slideOutVertically(animationSpec = tween(250), targetOffsetY = { -it }) + fadeOut(animationSpec = tween(250)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "Close reader" }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = state.book.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = state.book.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onToc,
                        modifier = Modifier.semantics { contentDescription = "Table of contents" }
                    ) {
                        Icon(Icons.Filled.FormatListNumbered, contentDescription = "TOC")
                    }

                    IconButton(
                        onClick = {
                            bookmarkTrigger++
                            onToggleBookmark()
                        },
                        modifier = Modifier.semantics { contentDescription = "Bookmark page" }
                    ) {
                        Icon(
                            if (state.isBookmarked) Icons.Filled.Bookmark
                            else Icons.Filled.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (state.isBookmarked) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.graphicsLayer {
                                scaleX = bookmarkScale
                                scaleY = bookmarkScale
                            }
                        )
                    }



                    IconButton(
                        onClick = onSettings,
                        modifier = Modifier.semantics { contentDescription = "Settings" }
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            }
        }

        // Bottom Control Bar
        AnimatedVisibility(
            visible = state.showControls,
            enter = slideInVertically(animationSpec = tween(250), initialOffsetY = { it }) + fadeIn(animationSpec = tween(250)),
            exit = slideOutVertically(animationSpec = tween(250), targetOffsetY = { it }) + fadeOut(animationSpec = tween(250)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Flip pill animation container
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                            modifier = Modifier
                                .graphicsLayer {
                                    rotationX = rotationXAnim.value
                                    cameraDistance = 12f * density
                                }
                                .animateContentSize()
                        ) {
                            Text(
                                text = "${displayedPage + 1} / ${state.totalPages}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        Row {
                            IconButton(
                                onClick = onBookmarks,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Bookmark,
                                    contentDescription = "Bookmarks",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = onHighlights,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Highlight,
                                    contentDescription = "Highlights",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (state.totalPages > 1) {
                        Slider(
                            value = state.currentPage.toFloat(),
                            onValueChange = { value ->
                                onPageChanged(value.toInt())
                            },
                            valueRange = 0f..(state.totalPages - 1).toFloat(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }

        // Summarize FAB
        AnimatedVisibility(
            visible = state.showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 24.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    showSummarySheet = true
                    viewModel.summarizeCurrentPage()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = "Summarize Page")
            }
        }



        // ModalBottomSheet for Gemini AI Summary
        if (showSummarySheet) {
            ModalBottomSheet(
                onDismissRequest = { 
                    showSummarySheet = false 
                    viewModel.clearSummary()
                },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "AI Page Summary",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (state.isSummarizing && state.summaryText.isEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Gemini is reading and summarizing...")
                        }
                    } else if (state.summaryError != null) {
                        Text(
                            text = state.summaryError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                                .verticalScroll(scrollState)
                        ) {
                            Text(
                                text = state.summaryText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    TextButton(
                        onClick = { 
                            showSummarySheet = false 
                            viewModel.clearSummary()
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}
