package br.acerola.comic.config.preference

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object MetadataPreference {
    private val Context.dataStore by preferencesDataStore(name = "metadata_prefs")
    private val GENERATE_COMIC_INFO_KEY = booleanPreferencesKey(name = "generate_comic_info_by_default")
    private val METADATA_LANGUAGE_KEY = stringPreferencesKey(name = "metadata_language")

    suspend fun saveGenerateComicInfo(
        context: Context,
        generate: Boolean,
    ) {
        context.dataStore.edit { prefs ->
            prefs[GENERATE_COMIC_INFO_KEY] = generate
        }
    }

    suspend fun saveMetadataLanguage(
        context: Context,
        language: String,
    ) {
        context.dataStore.edit { prefs ->
            prefs[METADATA_LANGUAGE_KEY] = language
        }
    }

    fun generateComicInfoFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[GENERATE_COMIC_INFO_KEY] ?: true
        }

    fun metadataLanguageFlow(context: Context): Flow<String?> =
        context.dataStore.data.map { prefs ->
            prefs[METADATA_LANGUAGE_KEY]
        }
}
