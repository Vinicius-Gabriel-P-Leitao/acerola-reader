package br.acerola.manga.config.preference

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ChapterPageSizeType(val key: String) {
    SHORT(key = "25"),
    MEDIUM(key = "50"),
    LARGE(key = "100");

    companion object {

        fun fromKey(key: String?): ChapterPageSizeType = entries.firstOrNull { it.key == key } ?: SHORT
    }
}

object ChapterPerPagePreference {

    private val Context.dataStore by preferencesDataStore(name = "chapter_per_page_prefs")

    private val CHAPTER_PER_PAGE_KEY = stringPreferencesKey(name = "chapter_per_page_type")

    suspend fun saveChapterPerPage(context: Context, size: ChapterPageSizeType) {
        context.dataStore.edit { prefs ->
            prefs[CHAPTER_PER_PAGE_KEY] = size.key
        }
    }

    fun chapterPerPageFlow(context: Context): Flow<ChapterPageSizeType> =
        context.dataStore.data.map { prefs ->
            ChapterPageSizeType.fromKey(key = prefs[CHAPTER_PER_PAGE_KEY])
        }
}
