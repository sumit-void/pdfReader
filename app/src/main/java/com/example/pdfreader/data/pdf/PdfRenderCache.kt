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

    suspend fun renderPage(pageIndex: Int, screenWidth: Int, theme: com.example.pdfreader.domain.model.AppTheme): Bitmap? = withContext(Dispatchers.IO) {
        renderMutex.withLock {
            val cacheKey = "${currentFilePath}_${pageIndex}_${screenWidth}_${theme.name}"
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

                // Apply theme-specific selective pixel filtering to preserve photos and illustrations
                if (theme != com.example.pdfreader.domain.model.AppTheme.LIGHT) {
                    val pixels = IntArray(width * height)
                    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                    for (i in pixels.indices) {
                        val pixel = pixels[i]
                        val a = (pixel shr 24) and 0xFF
                        val r = (pixel shr 16) and 0xFF
                        val g = (pixel shr 8) and 0xFF
                        val b = pixel and 0xFF

                        val maxVal = maxOf(r, maxOf(g, b))
                        val minVal = minOf(r, minOf(g, b))
                        if (maxVal - minVal <= 30) {
                            val luminance = (r + g + b) / 3f
                            val factor = luminance / 255f
                            val (newR, newG, newB) = when (theme) {
                                com.example.pdfreader.domain.model.AppTheme.SEPIA -> Triple(
                                    (92 + (245 - 92) * factor).toInt().coerceIn(0, 255),
                                    (61 + (236 - 61) * factor).toInt().coerceIn(0, 255),
                                    (46 + (215 - 46) * factor).toInt().coerceIn(0, 255)
                                )
                                com.example.pdfreader.domain.model.AppTheme.DARK -> Triple(
                                    (220 - 192 * factor).toInt().coerceIn(0, 255),
                                    (220 - 192 * factor).toInt().coerceIn(0, 255),
                                    (222 - 192 * factor).toInt().coerceIn(0, 255)
                                )
                                com.example.pdfreader.domain.model.AppTheme.AMOLED -> Triple(
                                    (236 - 236 * factor).toInt().coerceIn(0, 255),
                                    (236 - 236 * factor).toInt().coerceIn(0, 255),
                                    (236 - 236 * factor).toInt().coerceIn(0, 255)
                                )
                                else -> Triple(r, g, b)
                            }
                            pixels[i] = (a shl 24) or (newR shl 16) or (newG shl 8) or newB
                        }
                    }
                    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
                }
                val finalBitmap = bitmap

                memoryCache.put(cacheKey, finalBitmap)
                finalBitmap
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
