package br.acerola.comic.service.reader

import arrow.core.right
import br.acerola.comic.fixtures.ComicFixtures
import br.acerola.comic.service.cache.BitmapCacheHandler
import br.acerola.comic.service.reader.contract.PageSource
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class ReaderProcessorTest {
    private val factory = mockk<ChapterSourceFactory>()
    private val bitmapCache = mockk<BitmapCacheHandler>(relaxed = true)
    private val source = mockk<PageSource>(relaxed = true)

    private lateinit var repository: ReaderProcessor

    @Before
    fun setup() {
        repository = ReaderProcessor(factory, bitmapCache)
        every { factory.create(any()) } returns source.right()
    }

    @Test
    fun `given a chapter, when opening the chapter, should clear cache and initialize source`() {
        // Preparação (Organizar)
        val chapter = ComicFixtures.createChapterFileDto()

        // Ação (Agir)
        val result = repository.openChapter(chapter)

        // Verificação (Aferir)
        assertThat(result.isRight()).isTrue()
        verify { bitmapCache.clear() }
    }
}
