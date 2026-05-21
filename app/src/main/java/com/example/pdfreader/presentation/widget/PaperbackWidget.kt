package com.example.pdfreader.presentation.widget

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.pdfreader.presentation.MainActivity
import java.io.File

class PaperbackWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*>
        get() = PreferencesGlanceStateDefinition

    companion object {
        val KEY_BOOK_ID = longPreferencesKey("book_id")
        val KEY_TITLE = stringPreferencesKey("book_title")
        val KEY_AUTHOR = stringPreferencesKey("book_author")
        val KEY_PROGRESS = intPreferencesKey("book_progress")
        val KEY_COVER_PATH = stringPreferencesKey("book_cover_path")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val bookId = prefs[KEY_BOOK_ID] ?: -1L
            val title = prefs[KEY_TITLE] ?: ""
            val author = prefs[KEY_AUTHOR] ?: ""
            val progress = prefs[KEY_PROGRESS] ?: 0
            val coverPath = prefs[KEY_COVER_PATH] ?: ""

            WidgetContent(
                context = context,
                bookId = bookId,
                title = title,
                author = author,
                progress = progress,
                coverPath = coverPath
            )
        }
    }

    @androidx.compose.runtime.Composable
    private fun WidgetContent(
        context: Context,
        bookId: Long,
        title: String,
        author: String,
        progress: Int,
        coverPath: String
    ) {
        val hasBook = bookId != -1L

        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(android.R.color.background_dark))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!hasBook) {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Paperback",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(android.R.color.white)
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "No books read recently.",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = ColorProvider(android.R.color.darker_gray)
                        )
                    )
                }
            } else {
                // Book cover
                val coverFile = File(coverPath)
                val bitmap = try {
                    if (coverPath.isNotBlank() && coverFile.exists()) {
                        BitmapFactory.decodeFile(coverFile.absolutePath)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }

                if (bitmap != null) {
                    Image(
                        provider = ImageProvider(bitmap),
                        contentDescription = "Cover",
                        modifier = GlanceModifier
                            .size(width = 60.dp, height = 90.dp)
                            .background(ColorProvider(android.R.color.black))
                    )
                } else {
                    FallbackCover()
                }

                Spacer(modifier = GlanceModifier.width(12.dp))

                // Book details
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(android.R.color.white)
                        ),
                        maxLines = 2
                    )
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = author,
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = ColorProvider(android.R.color.darker_gray)
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = progress / 100f,
                        modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                        color = ColorProvider(android.R.color.holo_blue_light),
                        backgroundColor = ColorProvider(android.R.color.darker_gray)
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "$progress% completed",
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = ColorProvider(android.R.color.holo_blue_light)
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.width(12.dp))

                // Deep Link Action
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("paperback://reader/$bookId"),
                    context,
                    MainActivity::class.java
                )
                
                Button(
                    text = "Continue",
                    onClick = actionStartActivity(intent),
                    modifier = GlanceModifier.padding(vertical = 4.dp)
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun FallbackCover() {
        Box(
            modifier = GlanceModifier
                .size(width = 60.dp, height = 90.dp)
                .background(ColorProvider(android.R.color.holo_blue_dark)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "📕",
                style = TextStyle(fontSize = 24.sp)
            )
        }
    }
}
