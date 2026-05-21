package com.example.pdfreader.data.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfRenderCache @Inject constructor() {

    companion object {
        private const val MAX_CACHE_SIZE = 10
        private const val RENDER_SCALE = 2
    }

    private val memoryCache = object : LruCache<String, Bitmap>(MAX_CACHE_SIZE) {
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: Bitmap,
            newValue: Bitmap?
        ) {
            if (evicted && !oldValue.isRecycled) {
                oldValue.recycle()
            }
        }
    }

    private var currentRenderer: PdfRenderer? = null
    private var currentPfd: ParcelFileDescriptor? = null
    private var currentFilePath: String = ""
    private val renderMutex = Mutex()

    suspend fun openPdf(filePath: String): Int = withContext(Dispatchers.IO) {
        renderMutex.withLock {
            if (currentFilePath == filePath && currentRenderer != null) {
                return@withContext currentRenderer!!.pageCount
            }
            closeInternal()
            try {
                val file = File(filePath)
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                currentPfd = pfd
                currentRenderer = renderer
                currentFilePath = filePath
                renderer.pageCount
            } catch (e: Exception) {
                Timber.e(e, "Failed to open PDF: $filePath")
                throw e
            }
        }
    }

    suspend fun renderPage(pageIndex: Int, screenWidth: Int): Bitmap? = withContext(Dispatchers.IO) {
        renderMutex.withLock {
            val cacheKey = "${currentFilePath}_${pageIndex}_${screenWidth}"
            memoryCache.get(cacheKey)?.let { cached ->
                if (!cached.isRecycled) return@withContext cached
            }

            val renderer = currentRenderer ?: return@withContext null
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null

            try {
                val page = renderer.openPage(pageIndex)
                val scale = RENDER_SCALE
                val width = screenWidth * scale
                val height = (page.height.toFloat() / page.width * width).toInt()

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                memoryCache.put(cacheKey, bitmap)
                bitmap
            } catch (e: Exception) {
                Timber.e(e, "Failed to render page $pageIndex")
                null
            }
        }
    }

    fun getPageCount(): Int = currentRenderer?.pageCount ?: 0

    suspend fun close() {
        renderMutex.withLock {
            closeInternal()
        }
    }

    private fun closeInternal() {
        try {
            currentRenderer?.close()
            currentPfd?.close()
        } catch (e: Exception) {
            Timber.e(e, "Error closing PDF renderer")
        }
        currentRenderer = null
        currentPfd = null
        currentFilePath = ""
        memoryCache.evictAll()
    }

    fun clearCache() {
        memoryCache.evictAll()
    }
}
