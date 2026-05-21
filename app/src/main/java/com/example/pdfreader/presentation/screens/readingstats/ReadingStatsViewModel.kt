package com.example.pdfreader.presentation.screens.readingstats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdfreader.domain.model.ReadingStats
import com.example.pdfreader.domain.model.WeeklyActivity
import com.example.pdfreader.domain.usecase.GetReadingStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class ReadingStatsUiState(
    val stats: ReadingStats = ReadingStats(),
    val weeklyActivity: List<WeeklyActivity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ReadingStatsViewModel @Inject constructor(
    private val getReadingStatsUseCase: GetReadingStatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReadingStatsUiState())
    val uiState: StateFlow<ReadingStatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            try {
                val stats = getReadingStatsUseCase.getStats()
                val weekly = getReadingStatsUseCase.getWeeklyActivity()
                _uiState.update {
                    it.copy(
                        stats = stats,
                        weeklyActivity = weekly,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load reading stats")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
