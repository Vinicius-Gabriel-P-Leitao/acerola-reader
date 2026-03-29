package br.acerola.manga.config.preference

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class MangaSortType(val key: String) {
    TITLE(key = "TITLE"),
    CHAPTER_COUNT(key = "CHAPTER_COUNT"),
    LAST_UPDATE(key = "LAST_UPDATE");

    companion object {
        fun fromKey(key: String?): MangaSortType = entries.firstOrNull { it.key == key } ?: TITLE
    }
}

enum class SortDirection(val key: String) {
    ASCENDING(key = "ASCENDING"),
    DESCENDING(key = "DESCENDING");

    companion object {
        fun fromKey(key: String?): SortDirection = entries.firstOrNull { it.key == key } ?: ASCENDING
    }
}

data class HomeSortPreference(
    val type: MangaSortType,
    val direction: SortDirection
)

object MangaSortPreference {

    private val Context.dataStore by preferencesDataStore(name = "manga_sort_prefs")

    private val MANGA_SORT_TYPE_KEY = stringPreferencesKey(name = "manga_sort_type")
    private val MANGA_SORT_DIRECTION_KEY = stringPreferencesKey(name = "manga_sort_direction")

    suspend fun saveSort(context: Context, sort: HomeSortPreference) {
        context.dataStore.edit { prefs ->
            prefs[MANGA_SORT_TYPE_KEY] = sort.type.key
            prefs[MANGA_SORT_DIRECTION_KEY] = sort.direction.key
        }
    }

    fun sortFlow(context: Context): Flow<HomeSortPreference> =
        context.dataStore.data.map { prefs ->
            HomeSortPreference(
                type = MangaSortType.fromKey(key = prefs[MANGA_SORT_TYPE_KEY]),
                direction = SortDirection.fromKey(key = prefs[MANGA_SORT_DIRECTION_KEY])
            )
        }
}
