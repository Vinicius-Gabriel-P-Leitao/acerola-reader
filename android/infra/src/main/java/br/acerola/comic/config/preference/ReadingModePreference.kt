package br.acerola.comic.config.preference

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ReadingMode(
    val key: String,
) {
    HORIZONTAL(key = "HORIZONTAL"),
    VERTICAL(key = "VERTICAL"),
    WEBTOON(key = "WEBTOON"),
    ;

    companion object {
        fun fromKey(key: String?): ReadingMode = entries.firstOrNull { it.key == key } ?: HORIZONTAL
    }
}

object ReadingModePreference {
    private val Context.dataStore by preferencesDataStore(name = "reading_mode_prefs")
    private val READING_MODE_KEY = stringPreferencesKey(name = "reading_mode_type")

    suspend fun saveReadingMode(
        context: Context,
        mode: ReadingMode,
    ) {
        context.dataStore.edit { prefs ->
            prefs[READING_MODE_KEY] = mode.key
        }
    }

    fun readingModeFlow(context: Context): Flow<ReadingMode> =
        context.dataStore.data.map { prefs ->
            ReadingMode.fromKey(key = prefs[READING_MODE_KEY])
        }
}
