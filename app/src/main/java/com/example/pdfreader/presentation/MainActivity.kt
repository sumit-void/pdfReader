package com.example.pdfreader.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.example.pdfreader.data.preferences.UserPreferences
import com.example.pdfreader.domain.model.AppTheme
import com.example.pdfreader.presentation.navigation.PaperbackNavGraph
import com.example.pdfreader.presentation.theme.PaperbackTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main entry point for the Paperback PDF Reader app.
 * Uses Jetpack Compose for the entire UI with Hilt for DI.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeString by userPreferences.theme.collectAsState(initial = "LIGHT")
            val appTheme = try {
                AppTheme.valueOf(themeString)
            } catch (_: Exception) {
                AppTheme.LIGHT
            }

            PaperbackTheme(appTheme = appTheme) {
                val navController = rememberNavController()
                PaperbackNavGraph(navController = navController)
            }
        }
    }
}
