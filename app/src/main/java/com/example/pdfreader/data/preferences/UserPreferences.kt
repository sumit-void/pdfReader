package com.example.pdfreader.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "paperback_preferences")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val FONT_SIZE = floatPreferencesKey("font_size")
        val PAGE_TURN_STYLE = stringPreferencesKey("page_turn_style")
        val READING_DIRECTION = stringPreferencesKey("reading_direction")
        val BRIGHTNESS = floatPreferencesKey("brightness")
        val KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
        val LAST_OPENED_BOOK_ID = longPreferencesKey("last_opened_book_id")
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val LIBRARY_VIEW_MODE = stringPreferencesKey("library_view_mode")
        val LIBRARY_SORT_MODE = stringPreferencesKey("library_sort_mode")
    }

    private val dataStore = context.dataStore

    val theme: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading theme preference")
                emit(emptyPreferences())
            } else throw exception
        }
        .map { prefs -> prefs[Keys.THEME] ?: "LIGHT" }

    val fontSize: Flow<Float> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[Keys.FONT_SIZE] ?: 16f }

    val pageTurnStyle: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[Keys.PAGE_TURN_STYLE] ?: "CURL" }

    val readingDirection: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[Keys.READING_DIRECTION] ?: "LTR" }

    val brightness: Flow<Float> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[Keys.BRIGHTNESS] ?: -1f }

    val keepScreenAwake: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[Keys.KEEP_SCREEN_AWAKE] ?: false }

    val lastOpenedBookId: Flow<Long> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[Keys.LAST_OPENED_BOOK_ID] ?: -1L }

    val isFirstLaunch: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[Keys.IS_FIRST_LAUNCH] ?: true }

    val libraryViewMode: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[Keys.LIBRARY_VIEW_MODE] ?: "GRID" }

    val librarySortMode: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[Keys.LIBRARY_SORT_MODE] ?: "RECENT" }

    suspend fun setTheme(theme: String) {
        dataStore.edit { prefs -> prefs[Keys.THEME] = theme }
    }

    suspend fun setFontSize(size: Float) {
        dataStore.edit { prefs -> prefs[Keys.FONT_SIZE] = size }
    }

    suspend fun setPageTurnStyle(style: String) {
        dataStore.edit { prefs -> prefs[Keys.PAGE_TURN_STYLE] = style }
    }

    suspend fun setReadingDirection(direction: String) {
        dataStore.edit { prefs -> prefs[Keys.READING_DIRECTION] = direction }
    }

    suspend fun setBrightness(brightness: Float) {
        dataStore.edit { prefs -> prefs[Keys.BRIGHTNESS] = brightness }
    }

    suspend fun setKeepScreenAwake(awake: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.KEEP_SCREEN_AWAKE] = awake }
    }

    suspend fun setLastOpenedBookId(bookId: Long) {
        dataStore.edit { prefs -> prefs[Keys.LAST_OPENED_BOOK_ID] = bookId }
    }

    suspend fun setFirstLaunch(isFirst: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.IS_FIRST_LAUNCH] = isFirst }
    }

    suspend fun setLibraryViewMode(mode: String) {
        dataStore.edit { prefs -> prefs[Keys.LIBRARY_VIEW_MODE] = mode }
    }

    suspend fun setLibrarySortMode(mode: String) {
        dataStore.edit { prefs -> prefs[Keys.LIBRARY_SORT_MODE] = mode }
    }
}
