package com.example.pdfreader.presentation.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.state.updateAppWidgetState
import com.example.pdfreader.data.local.dao.BookDao
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class PaperbackWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PaperbackWidget()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun bookDao(): BookDao
    }

    override fun onUpdate(context: Context, appWidgetManager: android.appwidget.AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateWidgetData(context)
    }

    companion object {
        fun updateWidgetData(context: Context) {
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                try {
                    val entryPoint = EntryPoints.get(context.applicationContext, WidgetEntryPoint::class.java)
                    val bookDao = entryPoint.bookDao()
                    val lastBook = bookDao.getLastOpenedBook()
                    
                    val manager = GlanceAppWidgetManager(context)
                    val glanceIds = manager.getGlanceIds(PaperbackWidget::class.java)
                    for (glanceId in glanceIds) {
                        updateAppWidgetState(context, glanceId) { prefs ->
                            if (lastBook != null) {
                                prefs[PaperbackWidget.KEY_BOOK_ID] = lastBook.id
                                prefs[PaperbackWidget.KEY_TITLE] = lastBook.title
                                prefs[PaperbackWidget.KEY_AUTHOR] = lastBook.author
                                val progress = if (lastBook.pageCount > 0) {
                                    (lastBook.currentPage.toFloat() / (lastBook.pageCount - 1).coerceAtLeast(1) * 100).toInt().coerceIn(0, 100)
                                } else {
                                    0
                                }
                                prefs[PaperbackWidget.KEY_PROGRESS] = progress
                                prefs[PaperbackWidget.KEY_COVER_PATH] = lastBook.coverPath
                            } else {
                                prefs.clear()
                            }
                        }
                        PaperbackWidget().update(context, glanceId)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error updating widget data")
                }
            }
        }
    }
}
