package br.acerola.comic.service.reader

import br.acerola.comic.__fixtures__.ComicFixtures
import br.acerola.comic.service.cache.BitmapCacheHandler
import br.acerola.comic.service.reader.contract.PageSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat
import arrow.core.right

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
    fun `dado um capitulo, quando abrir o capitulo, deve limpar o cache e inicializar o source`() {
        // Arrange (Organizar)
        val chapter = ComicFixtures.createChapterFileDto()

        // Act (Agir)
        val result = repository.openChapter(chapter)

        // Assert (Aferir)
        assertThat(result.isRight()).isTrue()
        verify { bitmapCache.clear() }
    }
}
