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
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ButtonDefaults
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
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.IconButtonDefaults
import com.example.pdfreader.presentation.theme.PaperbackTheme
import androidx.compose.foundation.layout.widthIn

@Composable
fun ReaderScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBookmarks: (Long) -> Unit,
    onNavigateToHighlights: (Long) -> Unit,
    onNavigateToToc: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    BackHandler(onBack = onNavigateBack)
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

            PaperbackTheme(appTheme = state.theme, dynamicColor = true) {
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
    var showChatSheet by remember { mutableStateOf(false) }

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
        if (state.isReflowMode) {
            val reflowFontFamily = when (state.reflowFontFamily) {
                "Lora" -> FontFamily.Serif
                "Merriweather" -> FontFamily.Serif
                "Roboto" -> FontFamily.SansSerif
                else -> FontFamily.Default
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onToggleControls() }
                        )
                    }
                    .padding(horizontal = 20.dp)
                    .background(
                        when (state.theme) {
                            AppTheme.DARK -> Color(0xFF121212)
                            AppTheme.AMOLED -> Color.Black
                            AppTheme.SEPIA -> Color(0xFFF5ECD7)
                            else -> Color.White
                        }
                    )
            ) {
                if (state.isExtractingReflow) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(top = 70.dp, bottom = 100.dp)
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                            text = state.reflowText.ifBlank { "No text found on this page." },
                            style = TextStyle(
                                fontFamily = reflowFontFamily,
                                fontSize = state.reflowFontSize.sp,
                                lineHeight = (state.reflowFontSize * state.reflowLineSpacing).sp,
                                letterSpacing = state.reflowLetterSpacing.sp,
                                color = when (state.theme) {
                                    AppTheme.DARK, AppTheme.AMOLED -> Color.White
                                    AppTheme.SEPIA -> Color(0xFF4A3C2D)
                                    AppTheme.E_INK -> Color.Black
                                    else -> Color.Black
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = { viewModel.previousPage() },
                                enabled = state.currentPage > 0
                            ) {
                                Text("< Previous Page")
                            }
                            TextButton(
                                onClick = { viewModel.nextPage() },
                                enabled = state.currentPage < state.totalPages - 1
                            ) {
                                Text("Next Page >")
                            }
                        }
                    }
                }
            }
        } else {
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
        }

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
                        onClick = { viewModel.toggleAutoCrop() },
                        modifier = Modifier.semantics { contentDescription = "Auto Crop margins" }
                    ) {
                        Icon(
                            Icons.Filled.Crop,
                            contentDescription = "Auto Crop",
                            tint = if (state.isAutoCropEnabled) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleReflowMode() },
                        modifier = Modifier.semantics { contentDescription = "Text Reflow mode" }
                    ) {
                        Icon(
                            Icons.Filled.TextFields,
                            contentDescription = "Text Reflow",
                            tint = if (state.isReflowMode) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface
                        )
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
                    if (state.isReflowMode) {
                        ReflowSettingsPanel(state = state, viewModel = viewModel)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
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

        // AI Chat FAB
        AnimatedVisibility(
            visible = state.showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 168.dp, end = 24.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    showChatSheet = true
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(Icons.Filled.Chat, contentDescription = "Chat with Book")
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

        // ModalBottomSheet for Chat with Book
        if (showChatSheet) {
            ModalBottomSheet(
                onDismissRequest = { showChatSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "Chat with Book (AI RAG)",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (state.book.id == -1L) {
                        // Warn and block transient documents
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Offline AI Chat is not available for external, transient PDF documents. Import this book into your Library to unlock full semantic indexing and local Q&A capabilities.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        TextButton(
                            onClick = { showChatSheet = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Dismiss")
                        }
                    } else {
                        // Chat content
                        var userQuestion by remember { mutableStateOf("") }
                        val chatScrollState = rememberScrollState()

                        // Auto-scroll to bottom of chat when new messages arrive
                        LaunchedEffect(state.chatMessages.size) {
                            chatScrollState.animateScrollTo(chatScrollState.maxValue)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 200.dp, max = 350.dp)
                                .verticalScroll(chatScrollState)
                                .weight(1f, fill = false)
                        ) {
                            if (state.chatMessages.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Ask any deep questions about this book! I will search through its pages and give you contextual answers.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                }
                            } else {
                                state.chatMessages.forEach { chatMsg ->
                                    val isAi = chatMsg.sender == "ai"
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isAi) 4.dp else 16.dp,
                                                bottomEnd = if (isAi) 16.dp else 4.dp
                                            ),
                                            color = if (isAi) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
                                            border = if (isAi) androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                            ) else null,
                                            modifier = Modifier.widthIn(max = 280.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = chatMsg.message,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (isAi) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (state.isAskingQuestion && state.chatMessages.lastOrNull()?.sender != "ai") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Searching index and generating response...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (state.questionError != null) {
                                Text(
                                    text = state.questionError,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Question Input Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = userQuestion,
                                onValueChange = { userQuestion = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp
                                ),
                                decorationBox = { innerTextField ->
                                    if (userQuestion.isEmpty()) {
                                        Text(
                                            text = "Ask a question about this book...",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    innerTextField()
                                }
                            )

                            IconButton(
                                onClick = {
                                    if (userQuestion.isNotBlank() && !state.isAskingQuestion) {
                                        viewModel.askBookQuestion(userQuestion)
                                        userQuestion = ""
                                    }
                                },
                                enabled = userQuestion.isNotBlank() && !state.isAskingQuestion,
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Icon(
                                    Icons.Filled.Send,
                                    contentDescription = "Send",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReflowSettingsPanel(
    state: ReaderUiState.Success,
    viewModel: ReaderViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "Reflow Settings",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Font Size Adjustment
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        val newSize = (state.reflowFontSize - 2f).coerceAtLeast(12f)
                        viewModel.updateReflowSettings(
                            fontSize = newSize,
                            lineSpacing = state.reflowLineSpacing,
                            letterSpacing = state.reflowLetterSpacing,
                            fontFamily = state.reflowFontFamily
                        )
                    }
                ) {
                    Text(
                        "A-",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Text(
                    text = "${state.reflowFontSize.toInt()} sp",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                
                IconButton(
                    onClick = {
                        val newSize = (state.reflowFontSize + 2f).coerceAtMost(36f)
                        viewModel.updateReflowSettings(
                            fontSize = newSize,
                            lineSpacing = state.reflowLineSpacing,
                            letterSpacing = state.reflowLetterSpacing,
                            fontFamily = state.reflowFontFamily
                        )
                    }
                ) {
                    Text(
                        "A+",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Font Family Selection Chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Lora", "Merriweather", "Roboto").forEach { font ->
                    val isSelected = state.reflowFontFamily == font
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        onClick = {
                            viewModel.updateReflowSettings(
                                fontSize = state.reflowFontSize,
                                lineSpacing = state.reflowLineSpacing,
                                letterSpacing = state.reflowLetterSpacing,
                                fontFamily = font
                            )
                        },
                        modifier = Modifier.height(32.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = font,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
