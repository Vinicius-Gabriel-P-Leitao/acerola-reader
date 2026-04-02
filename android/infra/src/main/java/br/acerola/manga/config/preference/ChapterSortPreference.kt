package br.acerola.manga.config.preference

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ChapterSortType(val key: String) {
    NUMBER(key = "NUMBER"),
    LAST_UPDATE(key = "LAST_UPDATE");

    companion object {
        fun fromKey(key: String?): ChapterSortType = entries.firstOrNull { it.key == key } ?: NUMBER
    }
}

data class ChapterSortPreferenceData(
    val type: ChapterSortType,
    val direction: SortDirection
)

object ChapterSortPreference {

    private val Context.dataStore by preferencesDataStore(name = "chapter_sort_prefs")

    private val CHAPTER_SORT_TYPE_KEY = stringPreferencesKey(name = "chapter_sort_type")
    private val CHAPTER_SORT_DIRECTION_KEY = stringPreferencesKey(name = "chapter_sort_direction")

    suspend fun saveSort(context: Context, sort: ChapterSortPreferenceData) {
        context.dataStore.edit { prefs ->
            prefs[CHAPTER_SORT_TYPE_KEY] = sort.type.key
            prefs[CHAPTER_SORT_DIRECTION_KEY] = sort.direction.key
        }
    }

    fun sortFlow(context: Context): Flow<ChapterSortPreferenceData> =
        context.dataStore.data.map { prefs ->
            ChapterSortPreferenceData(
                type = ChapterSortType.fromKey(key = prefs[CHAPTER_SORT_TYPE_KEY]),
                direction = SortDirection.fromKey(key = prefs[CHAPTER_SORT_DIRECTION_KEY])
            )
        }
}
