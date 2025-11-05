package br.acerola.manga.shared.config

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object FileExtensions {
    val comicBookFormats = listOf(".cbz", ".cbr")
}

object FilePreferences {
    private val Context.dataStore by preferencesDataStore(name = "saved_file_prefs")
    private val SAVED_FILE = stringPreferencesKey(name = "saved_file")

    suspend fun saveFileExtension(context: Context, uri: String) {
        context.dataStore.edit { prefs ->
            prefs[SAVED_FILE] = uri
        }
    }

    fun fileExtensionFlow(context: Context): Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[SAVED_FILE] }
}
