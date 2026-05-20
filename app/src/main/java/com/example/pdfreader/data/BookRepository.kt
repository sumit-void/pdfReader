package com.example.pdfreader.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.lifecycle.LiveData
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.action.PDActionGoTo
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

data class Chapter(val title: String, val pageIndex: Int)
data class SearchResult(val pageIndex: Int, val snippet: String)

class BookRepository(private val context: Context) {

    private val db = BookDatabase.getDatabase(context)
    private val bookDao = db.bookDao()
    private val bookmarkDao = db.bookmarkDao()

    val allBooks: LiveData<List<Book>> = bookDao.getAllBooks()

    fun getBookmarksForBook(bookId: Long): LiveData<List<Bookmark>> {
        return bookmarkDao.getBookmarksForBook(bookId)
    }

    suspend fun getBookById(id: Long): Book? = withContext(Dispatchers.IO) {
        bookDao.getBookById(id)
    }

    suspend fun updateBook(book: Book) = withContext(Dispatchers.IO) {
        bookDao.updateBook(book)
    }

    suspend fun deleteBook(book: Book) = withContext(Dispatchers.IO) {
        // Delete PDF file
        val pdfFile = File(book.localFilePath)
        if (pdfFile.exists()) pdfFile.delete()

        // Delete cover file
        val coverFile = File(book.coverPath)
        if (coverFile.exists()) coverFile.delete()

        // Room cascade will delete bookmarks
        bookDao.deleteBook(book)
    }

    suspend fun addBookmark(bookId: Long, pageNumber: Int): Long = withContext(Dispatchers.IO) {
        val existing = bookmarkDao.getBookmarkByPage(bookId, pageNumber)
        if (existing != null) {
            return@withContext existing.id
        }
        
        // Generate preview path
        val previewPath = renderPageThumbnail(bookId, pageNumber)
        val bookmark = Bookmark(
            bookId = bookId,
            pageNumber = pageNumber,
            previewPath = previewPath
        )
        bookmarkDao.insertBookmark(bookmark)
    }

    suspend fun removeBookmark(bookId: Long, pageNumber: Int) = withContext(Dispatchers.IO) {
        val bookmark = bookmarkDao.getBookmarkByPage(bookId, pageNumber)
        if (bookmark != null) {
            bookmark.previewPath?.let { path ->
                val file = File(path)
                if (file.exists()) file.delete()
            }
            bookmarkDao.deleteBookmark(bookmark)
        }
    }

    suspend fun isBookmarked(bookId: Long, pageNumber: Int): Boolean = withContext(Dispatchers.IO) {
        bookmarkDao.getBookmarkByPage(bookId, pageNumber) != null
    }

    /**
     * Imports a PDF from SAF URI. Copies it locally, extracts metadata, generates a cover,
     * and inserts the record into Room.
     */
    suspend fun importBook(uri: Uri, displayName: String): Book = withContext(Dispatchers.IO) {
        // Create directories if they don't exist
        val booksDir = File(context.filesDir, "books").apply { mkdirs() }
        val coversDir = File(context.filesDir, "covers").apply { mkdirs() }

        // Copy file
        val uniqueId = UUID.randomUUID().toString()
        val localFile = File(booksDir, "book_$uniqueId.pdf")
        
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(localFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw Exception("Failed to open input stream for URI")

        // Load document to extract metadata
        var title = displayName.removeSuffix(".pdf")
        var author: String? = null
        var totalPages = 0

        try {
            PDDocument.load(localFile).use { doc ->
                totalPages = doc.numberOfPages
                val info = doc.documentInformation
                if (!info.title.isNullOrBlank()) {
                    title = info.title
                }
                if (!info.author.isNullOrBlank()) {
                    author = info.author
                }
            }
        } catch (e: Exception) {
            Log.e("BookRepository", "Error reading PDF metadata via PDFBox", e)
        }

        // Fallback: If totalPages is 0, try native PdfRenderer
        if (totalPages == 0) {
            try {
                ParcelFileDescriptor.open(localFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        totalPages = renderer.pageCount
                    }
                }
            } catch (e: Exception) {
                Log.e("BookRepository", "Error reading page count via PdfRenderer", e)
            }
        }

        // Generate cover page thumbnail
        val coverFile = File(coversDir, "cover_$uniqueId.png")
        var coverCreated = false
        try {
            ParcelFileDescriptor.open(localFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount > 0) {
                        renderer.openPage(0).use { page ->
                            val bitmap = Bitmap.createBitmap(300, 400, Bitmap.Config.ARGB_8888)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            FileOutputStream(coverFile).use { out ->
                                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                            }
                            coverCreated = true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BookRepository", "Error generating cover preview", e)
        }

        val book = Book(
            title = title,
            author = author,
            localFilePath = localFile.absolutePath,
            coverPath = if (coverCreated) coverFile.absolutePath else "" ,
            totalPages = totalPages
        )

        val id = bookDao.insertBook(book)
        book.copy(id = id)
    }

    /**
     * Extracts all text from the PDF. Can be cached or read on demand.
     */
    suspend fun loadFullText(filePath: String): String = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) return@withContext ""
        try {
            PDDocument.load(file).use { doc ->
                val stripper = PDFTextStripper()
                stripper.getText(doc)
            }
        } catch (e: Exception) {
            Log.e("BookRepository", "Failed to extract text from PDF", e)
            ""
        }
    }

    /**
     * Extracts text page-by-page from the PDF and returns a list of pages.
     */
    suspend fun loadPagesText(filePath: String): List<String> = withContext(Dispatchers.IO) {
        val file = File(filePath)
        val pagesText = mutableListOf<String>()
        if (!file.exists()) return@withContext pagesText
        try {
            PDDocument.load(file).use { doc ->
                val stripper = PDFTextStripper()
                for (i in 0 until doc.numberOfPages) {
                    stripper.startPage = i + 1
                    stripper.endPage = i + 1
                    pagesText.add(stripper.getText(doc).trim())
                }
            }
        } catch (e: Exception) {
            Log.e("BookRepository", "Failed to extract pages text", e)
        }
        pagesText
    }

    /**
     * Extracts the Table of Contents outline items.
     */
    suspend fun getTableOfContents(filePath: String): List<Chapter> = withContext(Dispatchers.IO) {
        val file = File(filePath)
        val chapters = mutableListOf<Chapter>()
        if (!file.exists()) return@withContext chapters

        try {
            PDDocument.load(file).use { doc ->
                val outline = doc.documentCatalog.documentOutline
                if (outline != null) {
                    var current = outline.firstChild
                    while (current != null) {
                        val title = current.title
                        var pageIndex = -1
                        
                        // Resolve page destination
                        val destination = current.destination
                        if (destination is PDPageDestination) {
                            pageIndex = destination.retrievePageNumber()
                        } else if (current.action is PDActionGoTo) {
                            val actionDest = (current.action as PDActionGoTo).destination
                            if (actionDest is PDPageDestination) {
                                pageIndex = actionDest.retrievePageNumber()
                            }
                        }

                        // Fallback: If pageIndex not resolved directly, look in catalog
                        if (pageIndex < 0 && destination != null) {
                            // Traverse catalog to search page node (simplification)
                            pageIndex = 0 
                        }

                        if (title != null) {
                            chapters.add(Chapter(title, if (pageIndex >= 0) pageIndex else 0))
                        }
                        current = current.nextSibling as? PDOutlineItem
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BookRepository", "Failed to load TOC", e)
        }
        chapters
    }

    /**
     * Performs keyword search on PDF text page-by-page.
     */
    suspend fun searchBook(filePath: String, query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SearchResult>()
        if (query.isBlank()) return@withContext results
        val file = File(filePath)
        if (!file.exists()) return@withContext results

        try {
            PDDocument.load(file).use { doc ->
                val stripper = PDFTextStripper()
                for (i in 0 until doc.numberOfPages) {
                    stripper.startPage = i + 1
                    stripper.endPage = i + 1
                    val text = stripper.getText(doc)
                    if (text.contains(query, ignoreCase = true)) {
                        val snippet = getSnippet(text, query)
                        results.add(SearchResult(pageIndex = i, snippet = snippet))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BookRepository", "Failed to search PDF", e)
        }
        results
    }

    private fun getSnippet(text: String, query: String): String {
        val index = text.indexOf(query, ignoreCase = true)
        if (index == -1) return text.take(60)
        
        val start = Math.max(0, index - 30)
        val end = Math.min(text.length, index + query.length + 30)
        var snippet = text.substring(start, end).replace('\n', ' ')
        if (start > 0) snippet = "..." + snippet
        if (end < text.length) snippet = snippet + "..."
        return snippet.trim()
    }

    private fun renderPageThumbnail(bookId: Long, pageIndex: Int): String? {
        val bookmarksDir = File(context.filesDir, "bookmarks").apply { mkdirs() }
        val previewFile = File(bookmarksDir, "bookmark_${bookId}_$pageIndex.png")
        
        try {
            val book = bookDao.getBookById(bookId) ?: return null
            ParcelFileDescriptor.open(File(book.localFilePath), ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (pageIndex < renderer.pageCount) {
                        renderer.openPage(pageIndex).use { page ->
                            val bitmap = Bitmap.createBitmap(150, 200, Bitmap.Config.ARGB_8888)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            FileOutputStream(previewFile).use { out ->
                                bitmap.compress(Bitmap.CompressFormat.PNG, 80, out)
                            }
                            return previewFile.absolutePath
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BookRepository", "Error rendering bookmark preview", e)
        }
        return null
    }
}
