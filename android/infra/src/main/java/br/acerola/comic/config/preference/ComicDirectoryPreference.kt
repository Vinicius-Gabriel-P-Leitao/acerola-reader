package br.acerola.comic.config.preference

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object ComicDirectoryPreference {
    private val Context.dataStore by preferencesDataStore(name = "folder_prefs")
    private val FOLDER_URI = stringPreferencesKey(name = "folder_uri")
    private val TUTORIAL_SHOWN = booleanPreferencesKey(name = "tutorial_shown")

    suspend fun saveFolderUri(
        context: Context,
        uri: String,
    ) {
        context.dataStore.edit { prefs ->
            prefs[FOLDER_URI] = uri
        }
    }

    suspend fun clearFolderUri(context: Context) {
        context.dataStore.edit { prefs ->
            prefs.remove(key = FOLDER_URI)
        }
    }

    fun folderUriFlow(context: Context): Flow<String?> = context.dataStore.data.map { prefs -> prefs[FOLDER_URI] }

    suspend fun setTutorialShown(
        context: Context,
        shown: Boolean,
    ) {
        context.dataStore.edit { prefs ->
            prefs[TUTORIAL_SHOWN] = shown
        }
    }

    fun tutorialShownFlow(context: Context): Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[TUTORIAL_SHOWN] ?: false }
}
