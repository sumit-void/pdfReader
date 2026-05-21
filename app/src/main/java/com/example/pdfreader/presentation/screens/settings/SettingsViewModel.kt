package com.example.pdfreader.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdfreader.data.preferences.UserPreferences
import com.example.pdfreader.domain.model.AppTheme
import com.example.pdfreader.domain.model.PageTurnStyle
import com.example.pdfreader.domain.model.ReadingDirection
import com.example.pdfreader.domain.repository.ReadingStatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val theme: AppTheme = AppTheme.LIGHT,
    val fontSize: Float = 16f,
    val pageTurnStyle: PageTurnStyle = PageTurnStyle.CURL,
    val readingDirection: ReadingDirection = ReadingDirection.LTR,
    val brightness: Float = -1f,
    val keepScreenAwake: Boolean = false,
    val appLockEnabled: Boolean = false,
    val blockScreenshots: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val readingStatsRepository: ReadingStatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observePreferences()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            userPreferences.theme.collectLatest { theme ->
                _uiState.update { it.copy(theme = try { AppTheme.valueOf(theme) } catch (_: Exception) { AppTheme.LIGHT }) }
            }
        }
        viewModelScope.launch {
            userPreferences.fontSize.collectLatest { size ->
                _uiState.update { it.copy(fontSize = size) }
            }
        }
        viewModelScope.launch {
            userPreferences.pageTurnStyle.collectLatest { style ->
                _uiState.update { it.copy(pageTurnStyle = try { PageTurnStyle.valueOf(style) } catch (_: Exception) { PageTurnStyle.CURL }) }
            }
        }
        viewModelScope.launch {
            userPreferences.readingDirection.collectLatest { dir ->
                _uiState.update { it.copy(readingDirection = try { ReadingDirection.valueOf(dir) } catch (_: Exception) { ReadingDirection.LTR }) }
            }
        }
        viewModelScope.launch {
            userPreferences.brightness.collectLatest { brightness ->
                _uiState.update { it.copy(brightness = brightness) }
            }
        }
        viewModelScope.launch {
            userPreferences.keepScreenAwake.collectLatest { awake ->
                _uiState.update { it.copy(keepScreenAwake = awake) }
            }
        }
        viewModelScope.launch {
            userPreferences.appLockEnabled.collectLatest { enabled ->
                _uiState.update { it.copy(appLockEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            userPreferences.blockScreenshots.collectLatest { blocked ->
                _uiState.update { it.copy(blockScreenshots = blocked) }
            }
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { userPreferences.setTheme(theme.name) }
    }

    fun setFontSize(size: Float) {
        viewModelScope.launch { userPreferences.setFontSize(size) }
    }

    fun setPageTurnStyle(style: PageTurnStyle) {
        viewModelScope.launch { userPreferences.setPageTurnStyle(style.name) }
    }

    fun setReadingDirection(direction: ReadingDirection) {
        viewModelScope.launch { userPreferences.setReadingDirection(direction.name) }
    }

    fun setBrightness(brightness: Float) {
        viewModelScope.launch { userPreferences.setBrightness(brightness) }
    }

    fun setKeepScreenAwake(awake: Boolean) {
        viewModelScope.launch { userPreferences.setKeepScreenAwake(awake) }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setAppLockEnabled(enabled) }
    }

    fun setBlockScreenshots(blocked: Boolean) {
        viewModelScope.launch { userPreferences.setBlockScreenshots(blocked) }
    }

    fun clearHistory() {
        viewModelScope.launch { readingStatsRepository.clearHistory() }
    }
}
