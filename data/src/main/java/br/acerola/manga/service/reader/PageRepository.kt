package br.acerola.manga.service.reader

import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.service.cache.PageCacheService
import br.acerola.manga.service.reader.port.ChapterSourceService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PageRepository @Inject constructor(
    private val factory: ChapterSourceFactory, private val cache: PageCacheService
) {

    private lateinit var source: ChapterSourceService

    fun openChapter(chapter: ChapterFileDto) {
        source = factory.create(chapter)
        cache.clear()
    }

    suspend fun pageCount(): Int = source.pageCount()

    suspend fun loadPage(index: Int): ByteArray {
        cache.get(index)?.let { return it }

        val bytes = source.openPage(index).use { it.readBytes() }
        cache.put(index, bytes)

        return bytes
    }

    fun prefetchWindow(center: Int) {
        val range = (center + 1)..(center + 3)

        range.forEach { index ->
            CoroutineScope(Dispatchers.IO).launch {
                if (cache.get(index) == null) {
                    loadPage(index)
                }
            }
        }
    }
}
