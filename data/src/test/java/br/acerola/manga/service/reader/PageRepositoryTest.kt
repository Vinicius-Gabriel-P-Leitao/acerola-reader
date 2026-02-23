package br.acerola.manga.service.reader

import arrow.core.Either
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.error.message.ChapterError
import br.acerola.manga.service.cache.PageCacheService
import br.acerola.manga.service.reader.port.ChapterSourceService
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

class PageRepositoryTest {

    @MockK
    lateinit var factory: ChapterSourceFactory

    @MockK
    lateinit var cache: PageCacheService

    @MockK
    lateinit var sourceService: ChapterSourceService

    private lateinit var repository: PageRepository

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        repository = PageRepository(factory, cache)
    }

    @Test
    fun `openChapter deve inicializar source e limpar cache`() {
        // Arrange
        val chapter = ChapterFileDto(1, "ch1", "path.cbz", "1")
        every { factory.create(chapter) } returns Either.Right(sourceService)
        every { cache.clear() } just runs
        // Simula fechamento de source anterior se houvesse (mas aqui é primeira vez)
        
        // Act
        val result = repository.openChapter(chapter)

        // Assert
        assertTrue(result.isRight())
        verify { cache.clear() }
        // Verifica se factory foi chamado
        verify { factory.create(chapter) }
    }

    @Test
    fun `loadPage deve retornar do cache se disponivel`() = runTest {
        // Arrange
        val pageData = byteArrayOf(1, 2, 3)
        every { cache.get(0) } returns Either.Right(pageData)

        // Act
        val result = repository.loadPage(0)

        // Assert
        assertTrue(result.isRight())
        result.onRight {
            assertArrayEquals(pageData, it)
        }
        // Garante que não foi buscar no source
        coVerify(exactly = 0) { sourceService.openPage(any()) }
    }

    @Test
    fun `loadPage deve buscar no source e atualizar cache se nao estiver cacheado`() = runTest {
        // Arrange: Primeiro abre o capítulo para inicializar o source
        val chapter = ChapterFileDto(1, "ch1", "path.cbz", "1")
        every { factory.create(chapter) } returns Either.Right(sourceService)
        every { cache.clear() } just runs
        repository.openChapter(chapter)

        val pageData = byteArrayOf(9, 8, 7)
        val stream = ByteArrayInputStream(pageData)

        every { cache.get(1) } returns Either.Left(ChapterError.UnexpectedError(Exception("Cache miss")))
        coEvery { sourceService.openPage(1) } returns Either.Right(stream)
        every { cache.put(1, any()) } just runs

        // Act
        val result = repository.loadPage(1)

        // Assert
        assertTrue(result.isRight())
        result.onRight {
            assertArrayEquals(pageData, it)
        }
        verify { cache.put(1, any()) }
    }

    @Test
    fun `pageCount deve delegar para o source`() = runTest {
        val chapter = ChapterFileDto(1, "ch1", "path.cbz", "1")
        every { factory.create(chapter) } returns Either.Right(sourceService)
        every { cache.clear() } just runs
        repository.openChapter(chapter)

        coEvery { sourceService.pageCount() } returns 25

        val count = repository.pageCount()

        assertEquals(25, count)
    }
}
