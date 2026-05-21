package com.example.pdfreader.data.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pdfreader.data.local.PaperbackDatabase
import com.example.pdfreader.data.local.entity.BookPageEntity
import com.example.pdfreader.domain.repository.BookRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber

class FtsIndexingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FtsIndexingWorkerEntryPoint {
        fun bookRepository(): BookRepository
        fun database(): PaperbackDatabase
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                FtsIndexingWorkerEntryPoint::class.java
            )
            val bookRepository = entryPoint.bookRepository()
            val database = entryPoint.database()
            val bookDao = database.bookDao()
            val bookPageDao = database.bookPageDao()

            val books = bookDao.getAllBooks().first()
            if (books.isEmpty()) {
                return@withContext Result.success()
            }

            for (book in books) {
                if (isStopped) break

                val totalPages = book.pageCount
                val indexedPages = bookPageDao.getIndexedPageIndices(book.id).toSet()

                for (pageIndex in 0 until totalPages) {
                    if (isStopped) break
                    if (indexedPages.contains(pageIndex)) continue

                    val text = bookRepository.extractPageText(book.filePath, pageIndex)
                    if (text.isNotBlank()) {
                        bookPageDao.insertPage(
                            BookPageEntity(
                                bookId = book.id,
                                pageIndex = pageIndex,
                                pageText = text
                            )
                        )
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error during background FTS indexing")
            Result.retry()
        }
    }
}
