package br.acerola.comic.config.preference

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object HomeFilterPreference {
    private val Context.dataStore by preferencesDataStore(name = "home_filter_prefs")

    private val SHOW_HIDDEN_KEY = booleanPreferencesKey(name = "show_hidden")

    suspend fun saveShowHidden(
        context: Context,
        showHidden: Boolean,
    ) {
        context.dataStore.edit { prefs ->
            prefs[SHOW_HIDDEN_KEY] = showHidden
        }
    }

    fun showHiddenFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[SHOW_HIDDEN_KEY] ?: false
        }
}
