package com.example.pdfreader.presentation.screens.highlights

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

enum class CardStyle(
    val displayName: String,
    val background: @Composable () -> Modifier,
    val textColor: Color,
    val metaColor: Color,
    val fontFamily: FontFamily,
    val fontStyle: androidx.compose.ui.text.font.FontStyle = androidx.compose.ui.text.font.FontStyle.Normal,
    val useBorder: Boolean = false,
    val showQuoteMark: Boolean = true
) {
    WARM_GRADIENT(
        displayName = "Warm",
        background = {
            Modifier.background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFF39C12), Color(0xFFE74C3C))
                )
            )
        },
        textColor = Color.White,
        metaColor = Color.White.copy(alpha = 0.8f),
        fontFamily = FontFamily.SansSerif,
        showQuoteMark = true
    ),
    COOL_GRADIENT(
        displayName = "Sunset",
        background = {
            Modifier.background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))
                )
            )
        },
        textColor = Color.White,
        metaColor = Color.White.copy(alpha = 0.8f),
        fontFamily = FontFamily.SansSerif,
        showQuoteMark = true
    ),
    MINIMAL_LIGHT(
        displayName = "Minimal",
        background = {
            Modifier.background(Color.White)
        },
        textColor = Color(0xFF2C3E50),
        metaColor = Color(0xFF7F8C8D),
        fontFamily = FontFamily.Serif,
        useBorder = true,
        showQuoteMark = true
    ),
    MIDNIGHT_DARK(
        displayName = "Midnight",
        background = {
            Modifier.background(Color(0xFF0F172A))
        },
        textColor = Color(0xFFF8FAFC),
        metaColor = Color(0xFF94A3B8),
        fontFamily = FontFamily.SansSerif,
        showQuoteMark = true
    ),
    CLASSIC_SEPIA(
        displayName = "Sepia",
        background = {
            Modifier.background(Color(0xFFF4ECD8))
        },
        textColor = Color(0xFF5C4033),
        metaColor = Color(0xFF8B5A2B),
        fontFamily = FontFamily.Serif,
        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
        showQuoteMark = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteCardEditorScreen(
    onNavigateBack: () -> Unit,
    viewModel: QuoteCardEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var selectedStyle by remember { mutableStateOf(CardStyle.WARM_GRADIENT) }
    var fontSize by remember { mutableFloatStateOf(18f) }
    var viewToCapture by remember { mutableStateOf<View?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Quote Exporter",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewToCapture?.let { view ->
                                shareBitmap(context, view)
                            } ?: run {
                                Toast.makeText(context, "Preview not ready", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !uiState.isLoading && uiState.highlight != null
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Export and Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.errorMessage ?: "An error occurred",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.background)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Card Preview Canvas Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    ComposeView(ctx).apply {
                                        setContent {
                                            CardPreview(
                                                text = uiState.highlight?.text ?: "",
                                                title = uiState.bookTitle,
                                                author = uiState.bookAuthor,
                                                style = selectedStyle,
                                                fontSize = fontSize
                                            )
                                        }
                                        viewToCapture = this
                                    }
                                },
                                update = { view ->
                                    view.setContent {
                                        CardPreview(
                                            text = uiState.highlight?.text ?: "",
                                            title = uiState.bookTitle,
                                            author = uiState.bookAuthor,
                                            style = selectedStyle,
                                            fontSize = fontSize
                                        )
                                    }
                                    viewToCapture = view
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Controls panel
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Text(
                                text = "Customize Card",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Font size controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Text Size",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${fontSize.toInt()} sp",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Slider(
                                value = fontSize,
                                onValueChange = { fontSize = it },
                                valueRange = 12f..32f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Styles",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Horizontal styles selection list
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(CardStyle.entries) { style ->
                                    StyleSelectorItem(
                                        style = style,
                                        isSelected = selectedStyle == style,
                                        onClick = { selectedStyle = style }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CardPreview(
    text: String,
    title: String,
    author: String,
    style: CardStyle,
    fontSize: Float
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(style.background())
            .then(
                if (style.useBorder) {
                    Modifier.border(1.dp, Color(0xFFD1D5DB))
                } else {
                    Modifier
                }
            )
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (style.showQuoteMark) {
                Text(
                    text = "“",
                    style = TextStyle(
                        fontFamily = style.fontFamily,
                        fontSize = (fontSize * 2.5).sp,
                        fontWeight = FontWeight.Bold,
                        color = style.textColor.copy(alpha = 0.2f),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.height((fontSize * 1.5).dp)
                )
            }

            Text(
                text = text,
                style = TextStyle(
                    fontFamily = style.fontFamily,
                    fontSize = fontSize.sp,
                    fontStyle = style.fontStyle,
                    fontWeight = FontWeight.Medium,
                    color = style.textColor,
                    lineHeight = (fontSize * 1.45).sp,
                    textAlign = TextAlign.Center
                ),
                maxLines = 8,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Decorative separator line
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(1.dp)
                    .background(style.metaColor.copy(alpha = 0.3f))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = (fontSize * 0.7f).sp,
                    fontWeight = FontWeight.Bold,
                    color = style.textColor,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.9f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = author,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = (fontSize * 0.62f).sp,
                    fontWeight = FontWeight.Normal,
                    color = style.metaColor,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.9f)
            )
        }
    }
}

@Composable
fun StyleSelectorItem(
    style: CardStyle,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .border(
                    width = if (isSelected) 2.5.dp else 0.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = CircleShape
                )
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .then(
                        when (style) {
                            CardStyle.WARM_GRADIENT -> Modifier.background(Brush.linearGradient(colors = listOf(Color(0xFFF39C12), Color(0xFFE74C3C))))
                            CardStyle.COOL_GRADIENT -> Modifier.background(Brush.linearGradient(colors = listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))))
                            CardStyle.MINIMAL_LIGHT -> Modifier.background(Color.White).border(1.dp, Color(0xFFE5E7EB), CircleShape)
                            CardStyle.MIDNIGHT_DARK -> Modifier.background(Color(0xFF0F172A))
                            CardStyle.CLASSIC_SEPIA -> Modifier.background(Color(0xFFF4ECD8))
                        }
                    )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = style.displayName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun shareBitmap(context: Context, view: View) {
    if (view.width == 0 || view.height == 0) {
        Toast.makeText(context, "Rendering card, try again...", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        view.draw(canvas)

        val sharedImagesDir = File(context.cacheDir, "shared_images")
        if (!sharedImagesDir.exists()) {
            sharedImagesDir.mkdirs()
        }
        val file = File(sharedImagesDir, "quote_card_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "com.example.pdfreader.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share Quote Card"))
    } catch (e: Exception) {
        Timber.e(e, "Failed to share quote card image")
        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
