package br.acerola.manga.shared.config

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class FileExtension(val ext: String) {
    CBZ(ext = ".cbz"), CBR(ext = ".cbr");

    companion object {
        fun from(value: String?): FileExtension =
            entries.find { it.ext.equals(other = value, ignoreCase = true) } ?: CBZ

        fun isSupported(ext: String?): Boolean {
            if (ext.isNullOrBlank()) return false
            val cleanExt = ext.substringAfterLast(delimiter = '.').lowercase()
            return entries.any { it.name.equals(other = cleanExt, ignoreCase = true) }
        }
    }
}

object FilePreferences {
    private val Context.dataStore by preferencesDataStore(name = "file_config_prefs")
    private val FILE_EXTENSION_KEY = stringPreferencesKey(name = "preferred_file_extension")

    suspend fun saveFileExtension(context: Context, extension: FileExtension) {
        context.dataStore.edit { prefs ->
            prefs[FILE_EXTENSION_KEY] = extension.ext
        }
    }

    fun fileExtensionFlow(context: Context): Flow<FileExtension> = context.dataStore.data.map { prefs ->
        FileExtension.from(value = prefs[FILE_EXTENSION_KEY])
    }
}