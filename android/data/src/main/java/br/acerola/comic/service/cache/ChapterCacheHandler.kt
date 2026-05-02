package br.acerola.comic.service.cache

import android.util.LruCache
import br.acerola.comic.dto.ChapterDto
import javax.inject.Inject
import javax.inject.Singleton

// TODO: Escrever testes
@Singleton
class ChapterCacheHandler
    @Inject
    constructor() {
        private val cacheSize = 8
        private val cache = LruCache<String, ChapterDto>(cacheSize)

        fun get(key: String): ChapterDto? = cache[key]

        fun put(
            key: String,
            data: ChapterDto,
        ) {
            // Only cache if there's actual data to avoid caching loading/empty states
            if (data.archive.items.isNotEmpty() || data.archive.volumeSections.isNotEmpty()) {
                cache.put(key, data)
            }
        }

        fun remove(key: String) {
            cache.remove(key)
        }

        fun clear() {
            cache.evictAll()
        }

        fun generateKey(
            comicId: Long,
            sortType: String,
            isAsc: Boolean,
            pageSize: Int,
            viewMode: String,
            page: Int,
            suffix: String = "",
        ): String = "${comicId}_${sortType}_${isAsc}_${pageSize}_${viewMode}_${page}_$suffix"
    }
