package com.example.pdfreader.data.repository

import com.example.pdfreader.data.local.dao.HighlightDao
import com.example.pdfreader.data.local.entity.HighlightEntity
import com.example.pdfreader.domain.model.Highlight
import com.example.pdfreader.domain.model.HighlightColor
import com.example.pdfreader.domain.repository.HighlightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HighlightRepositoryImpl @Inject constructor(
    private val highlightDao: HighlightDao
) : HighlightRepository {

    override fun getHighlightsForBook(bookId: Long): Flow<List<Highlight>> {
        return highlightDao.getHighlightsForBook(bookId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getHighlightsForPage(bookId: Long, page: Int): Flow<List<Highlight>> {
        return highlightDao.getHighlightsForPage(bookId, page).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun addHighlight(highlight: Highlight): Long {
        return highlightDao.insertHighlight(
            HighlightEntity(
                bookId = highlight.bookId,
                pageNumber = highlight.pageNumber,
                startIndex = highlight.startIndex,
                endIndex = highlight.endIndex,
                text = highlight.text,
                color = highlight.color.name
            )
        )
    }

    override suspend fun deleteHighlight(id: Long) {
        highlightDao.deleteHighlightById(id)
    }

    override suspend fun getHighlightById(id: Long): Highlight? {
        return highlightDao.getHighlightById(id)?.toDomainModel()
    }

    private fun HighlightEntity.toDomainModel(): Highlight {
        return Highlight(
            id = id,
            bookId = bookId,
            pageNumber = pageNumber,
            startIndex = startIndex,
            endIndex = endIndex,
            text = text,
            color = try { HighlightColor.valueOf(color) } catch (_: Exception) { HighlightColor.YELLOW },
            dateCreated = dateCreated
        )
    }
}
