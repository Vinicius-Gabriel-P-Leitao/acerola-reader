package br.acerola.manga.config.preference

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object ThemePreference {
    private val Context.dataStore by preferencesDataStore(name = "theme_prefs")
    private val DYNAMIC_COLOR_KEY = booleanPreferencesKey(name = "dynamic_color_enabled")

    suspend fun saveDynamicColor(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DYNAMIC_COLOR_KEY] = enabled
        }
    }

    fun dynamicColorFlow(context: Context): Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DYNAMIC_COLOR_KEY] ?: false
    }
}
