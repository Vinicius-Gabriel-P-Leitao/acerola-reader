package br.acerola.manga.service.reader

import arrow.core.Either
import arrow.core.right
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.error.message.ChapterError
import br.acerola.manga.service.cache.PageCacheService
import br.acerola.manga.service.reader.port.ChapterSourceService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PageRepository @Inject constructor(
    private val factory: ChapterSourceFactory,
    private val cache: PageCacheService
) {
    private lateinit var source: ChapterSourceService

    fun openChapter(chapter: ChapterFileDto): Either<ChapterError, Unit> {
        return factory.create(chapter).map { newSource ->
            if (::source.isInitialized) {
                source.close()
            }
            source = newSource
            cache.clear()
        }
    }

    suspend fun pageCount(): Int = source.pageCount()

    suspend fun loadPage(index: Int): Either<ChapterError, ByteArray> {
        cache.get(index).onRight { return it.right() }

        return source.openPage(index)
            .map { stream ->
                stream.use { it.readBytes() }
            }.onRight { bytes ->
                cache.put(index, data = bytes)
            }
    }

    fun prefetchWindow(center: Int) {
        val range = (center + 1)..(center + 3)

        range.forEach { index ->
            CoroutineScope(context = Dispatchers.IO).launch {
                if (cache.get(index).isLeft()) {
                    loadPage(index)
                }
            }
        }
    }
}