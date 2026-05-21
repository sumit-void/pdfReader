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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
    val pagerState = rememberPagerState(
        initialPage = state.currentPage,
        pageCount = { state.totalPages }
    )
    var showSummarySheet by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onPageChanged(page)
        }
    }

    LaunchedEffect(state.currentPage) {
        if (pagerState.currentPage != state.currentPage) {
            pagerState.scrollToPage(state.currentPage)
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
        // PDF Pages Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondBoundsPageCount = 1
        ) { pageIndex ->
            PdfPage(
                pageIndex = pageIndex,
                onRenderPage = onRenderPage,
                onTap = onToggleControls
            )
        }

        // Top Control Bar
        AnimatedVisibility(
            visible = state.showControls,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it }),
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
                        onClick = onToggleBookmark,
                        modifier = Modifier.semantics { contentDescription = "Bookmark page" }
                    ) {
                        Icon(
                            if (state.isBookmarked) Icons.Filled.Bookmark
                            else Icons.Filled.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (state.isBookmarked) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { viewModel.startTts() },
                        modifier = Modifier.semantics { contentDescription = "Read aloud" }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Read aloud",
                            tint = if (state.isTtsActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
            visible = state.showControls && !state.isTtsActive,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
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
                        Text(
                            text = "Page ${state.currentPage + 1} of ${state.totalPages}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

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
                                scope.launch {
                                    pagerState.scrollToPage(value.toInt())
                                }
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
            visible = state.showControls && !state.isTtsActive,
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

        // TTS Text Overlay
        if (state.isTtsActive && state.pageText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp, bottom = 120.dp, start = 24.dp, end = 24.dp)
                    .background(
                        color = when (state.theme) {
                            AppTheme.DARK -> Color(0xFF1E1E1E).copy(alpha = 0.95f)
                            AppTheme.AMOLED -> Color.Black.copy(alpha = 0.95f)
                            AppTheme.SEPIA -> Color(0xFFF4ECD8).copy(alpha = 0.95f)
                            else -> Color.White.copy(alpha = 0.95f)
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                val annotatedString = remember(state.pageText, state.ttsWordRange) {
                    buildAnnotatedString {
                        append(state.pageText)
                        state.ttsWordRange?.let { (start, end) ->
                            if (start in 0..state.pageText.length && end in start..state.pageText.length) {
                                addStyle(
                                    style = SpanStyle(
                                        background = Color.Yellow.copy(alpha = 0.5f),
                                        fontWeight = FontWeight.Bold
                                    ),
                                    start = start,
                                    end = end
                                )
                            }
                        }
                    }
                }
                
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reading Aloud",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = when (state.theme) {
                                AppTheme.DARK, AppTheme.AMOLED -> Color.White
                                else -> Color.Black
                            }
                        )
                        IconButton(onClick = { viewModel.stopTts() }) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = "Stop",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        val scrollState = rememberScrollState()
                        Text(
                            text = annotatedString,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = 28.sp,
                                fontSize = 18.sp
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState),
                            color = when (state.theme) {
                                AppTheme.DARK, AppTheme.AMOLED -> Color.White.copy(alpha = 0.9f)
                                else -> Color.Black.copy(alpha = 0.9f)
                            }
                        )
                    }
                }
            }
        }

        // TTS Playback Control Bar
        AnimatedVisibility(
            visible = state.isTtsActive,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (state.isTtsPlaying) {
                                viewModel.pauseTts()
                            } else {
                                viewModel.resumeTts()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (state.isTtsPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (state.isTtsPlaying) "Pause" else "Play",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { viewModel.stopTts() }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Stop,
                            contentDescription = "Stop",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
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

@Composable
private fun PdfPage(
    pageIndex: Int,
    onRenderPage: (Int, (Bitmap?) -> Unit) -> Unit,
    onTap: () -> Unit
) {
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(pageIndex) { mutableStateOf(true) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(pageIndex) {
        isLoading = true
        onRenderPage(pageIndex) { renderedBitmap ->
            bitmap = renderedBitmap
            isLoading = false
        }
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        if (scale > 1f) {
            offset = Offset(
                x = offset.x + panChange.x,
                y = offset.y + panChange.y
            )
        } else {
            offset = Offset.Zero
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        scale = 1f
                        offset = Offset.Zero
                    }
                )
            }
            .transformable(state = transformState)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading || bitmap == null) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }

        bitmap?.let { bmp ->
            if (!bmp.isRecycled) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "PDF page ${pageIndex + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
