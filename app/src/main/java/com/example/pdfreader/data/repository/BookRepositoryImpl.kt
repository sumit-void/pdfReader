package com.example.pdfreader.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.pdfreader.data.local.dao.BookDao
import com.example.pdfreader.data.local.entity.BookEntity
import com.example.pdfreader.domain.model.Book
import com.example.pdfreader.domain.repository.BookRepository
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookDao: BookDao
) : BookRepository {

    init {
        PDFBoxResourceLoader.init(context)
    }

    override fun getAllBooks(): Flow<List<Book>> {
        return bookDao.getAllBooks().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun searchBooks(query: String): Flow<List<Book>> {
        return bookDao.searchBooks(query).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getBookById(id: Long): Book? {
        return bookDao.getBookById(id)?.toDomainModel()
    }

    override fun observeBook(id: Long): Flow<Book?> {
        return bookDao.observeBookById(id).map { it?.toDomainModel() }
    }

    override suspend fun getLastOpenedBook(): Book? {
        return bookDao.getLastOpenedBook()?.toDomainModel()
    }

    override suspend fun importPdf(uri: Uri): Result<Book> = withContext(Dispatchers.IO) {
        try {
            // Copy PDF to app internal storage
            val booksDir = File(context.filesDir, "books").apply { mkdirs() }
            val fileName = "book_${System.currentTimeMillis()}.pdf"
            val destFile = File(booksDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Cannot open file"))

            // Extract metadata using PDFBox
            var title = destFile.nameWithoutExtension
            var author = "Unknown Author"
            var pageCount = 0

            try {
                PDDocument.load(destFile).use { document ->
                    pageCount = document.numberOfPages
                    val info = document.documentInformation
                    if (!info.title.isNullOrBlank()) title = info.title
                    if (!info.author.isNullOrBlank()) author = info.author
                }
            } catch (e: Exception) {
                Timber.w(e, "PDFBox metadata extraction failed, trying PdfRenderer")
                try {
                    val pfd = ParcelFileDescriptor.open(destFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(pfd)
                    pageCount = renderer.pageCount
                    renderer.close()
                    pfd.close()
                } catch (e2: Exception) {
                    Timber.e(e2, "PdfRenderer fallback also failed")
                    destFile.delete()
                    return@withContext Result.failure(e2)
                }
            }

            // Generate cover thumbnail
            val coverPath = generateCoverThumbnail(destFile, "cover_${System.currentTimeMillis()}.png")

            // Check if book already exists
            val existingBook = bookDao.getBookByPath(destFile.absolutePath)
            if (existingBook != null) {
                return@withContext Result.success(existingBook.toDomainModel())
            }

            val entity = BookEntity(
                filePath = destFile.absolutePath,
                title = title,
                author = author,
                pageCount = pageCount,
                coverPath = coverPath,
                fileSize = destFile.length(),
                dateAdded = System.currentTimeMillis()
            )

            val id = bookDao.insertBook(entity)
            Result.success(entity.copy(id = id).toDomainModel())
        } catch (e: Exception) {
            Timber.e(e, "Failed to import PDF")
            Result.failure(e)
        }
    }

    override suspend fun deleteBook(book: Book) = withContext(Dispatchers.IO) {
        try {
            val entity = bookDao.getBookById(book.id) ?: return@withContext
            // Delete file
            File(entity.filePath).delete()
            // Delete cover
            if (entity.coverPath.isNotBlank()) File(entity.coverPath).delete()
            // Delete from DB
            bookDao.deleteBook(entity)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete book")
        }
    }

    override suspend fun updateReadingProgress(bookId: Long, page: Int) {
        bookDao.updateReadingProgress(bookId, page)
        com.example.pdfreader.presentation.widget.PaperbackWidgetReceiver.updateWidgetData(context)
    }

    override suspend fun addReadingTime(bookId: Long, durationMs: Long) {
        bookDao.addReadingTime(bookId, durationMs)
    }

    override suspend fun getBookCount(): Int = bookDao.getBookCount()

    override suspend fun getTotalPagesRead(): Int = bookDao.getTotalPagesRead() ?: 0

    override suspend fun extractPageText(filePath: String, pageIndex: Int): String = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            PDDocument.load(file).use { document ->
                val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
                stripper.startPage = pageIndex + 1
                stripper.endPage = pageIndex + 1
                stripper.getText(document) ?: ""
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract text from page $pageIndex of $filePath")
            ""
        }
    }

    private suspend fun generateCoverThumbnail(pdfFile: File, coverFileName: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
                val coverFile = File(coversDir, coverFileName)

                val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                val page = renderer.openPage(0)

                val scale = 2
                val bitmap = Bitmap.createBitmap(
                    page.width * scale,
                    page.height * scale,
                    Bitmap.Config.ARGB_8888
                )
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                pfd.close()

                FileOutputStream(coverFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                }
                bitmap.recycle()

                coverFile.absolutePath
            } catch (e: Exception) {
                Timber.e(e, "Failed to generate cover thumbnail")
                ""
            }
        }
    }

    private fun BookEntity.toDomainModel(): Book {
        return Book(
            id = id,
            filePath = filePath,
            title = title,
            author = author,
            pageCount = pageCount,
            coverPath = coverPath,
            currentPage = currentPage,
            totalReadTimeMs = totalReadTimeMs,
            fileSize = fileSize,
            dateAdded = dateAdded,
            lastOpened = lastOpened,
            progress = if (pageCount > 0) (currentPage.toFloat() / pageCount * 100).toInt() else 0
        )
    }
}
