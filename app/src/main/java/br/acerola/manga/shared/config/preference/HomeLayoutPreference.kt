package br.acerola.manga.shared.config.preference

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class HomeLayoutType(val key: String) {
    LIST(key = "LIST"),
    GRID(key = "GRID");

    companion object {
        fun fromKey(key: String?): HomeLayoutType =
            entries.firstOrNull { it.key == key } ?: LIST
    }
}

object HomeLayoutPreferences {

    private val Context.dataStore by preferencesDataStore(name = "home_layout_prefs")

    private val HOME_LAYOUT_KEY = stringPreferencesKey(name = "home_layout_type")

    suspend fun saveLayout(context: Context, layout: HomeLayoutType) {
        context.dataStore.edit { prefs ->
            prefs[HOME_LAYOUT_KEY] = layout.key
        }
    }

    fun layoutFlow(context: Context): Flow<HomeLayoutType> =
        context.dataStore.data.map { prefs ->
            HomeLayoutType.fromKey(prefs[HOME_LAYOUT_KEY])
        }
}
