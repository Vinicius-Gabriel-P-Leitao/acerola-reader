package br.acerola.manga.shared.config

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object FolderPreferences {
    private val Context.dataStore by preferencesDataStore(name = "folder_prefs")
    private val FOLDER_URI = stringPreferencesKey("folder_uri")

    suspend fun saveFolderUri(context: Context, uri: String) {
        context.dataStore.edit { prefs ->
            prefs[FOLDER_URI] = uri
        }
    }

    fun folderUriFlow(context: Context): Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[FOLDER_URI] }
}
