package br.acerola.comic.config.preference

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object ThemePreference {
    private val Context.dataStore by preferencesDataStore(name = "theme_prefs")

    private val SELECTED_THEME_KEY = stringPreferencesKey(name = "selected_app_theme")

    suspend fun saveTheme(
        context: Context,
        theme: AppTheme,
    ) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_THEME_KEY] = theme.key
        }
    }

    fun themeFlow(context: Context): Flow<AppTheme> =
        context.dataStore.data.map { prefs ->
            val savedTheme = prefs[SELECTED_THEME_KEY]
            AppTheme.fromKey(savedTheme)
        }
}
