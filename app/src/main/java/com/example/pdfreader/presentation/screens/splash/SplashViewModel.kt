package com.example.pdfreader.presentation.screens.splash

import androidx.lifecycle.ViewModel
import com.example.pdfreader.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    suspend fun getLastOpenedBookId(): Long {
        return try {
            userPreferences.lastOpenedBookId.first()
        } catch (_: Exception) {
            -1L
        }
    }
}
