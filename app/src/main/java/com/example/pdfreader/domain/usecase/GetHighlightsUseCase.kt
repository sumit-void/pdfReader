package com.example.pdfreader.domain.usecase

import com.example.pdfreader.domain.model.Highlight
import com.example.pdfreader.domain.repository.HighlightRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHighlightsUseCase @Inject constructor(
    private val highlightRepository: HighlightRepository
) {
    operator fun invoke(bookId: Long): Flow<List<Highlight>> {
        return highlightRepository.getHighlightsForBook(bookId)
    }
}
