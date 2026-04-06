package br.acerola.comic.service.reader

import arrow.core.Either
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.error.message.ChapterError
import br.acerola.comic.service.reader.extract.CbrPageResolver
import br.acerola.comic.service.reader.extract.CbzPageResolver
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import javax.inject.Provider

class ChapterSourceFactoryTest {

    private lateinit var cbzProvider: Provider<CbzPageResolver>
    private lateinit var cbrProvider: Provider<CbrPageResolver>
    private lateinit var factory: ChapterSourceFactory

    private val cbzService = mockk<CbzPageResolver>()
    private val cbrService = mockk<CbrPageResolver>()

    @Before
    fun setUp() {
        cbzProvider = mockk { every { get() } returns cbzService }
        cbrProvider = mockk { every { get() } returns cbrService }
        factory = ChapterSourceFactory(cbzProvider, cbrProvider)
    }

    @Test
    fun create_deve_retornar_CBZ_quando_extensao_for_cbz() {
        val chapter = ChapterFileDto(1, "ch", "file.cbz", "1")
        every { cbzService.open(chapter) } returns Either.Right(cbzService)

        val result = factory.create(chapter)

        assertTrue(result.isRight())
        result.onRight { assertTrue(it is CbzPageResolver) }
    }

    @Test
    fun create_deve_retornar_CBR_quando_extensao_for_cbr() {
        val chapter = ChapterFileDto(1, "ch", "file.cbr", "1")
        every { cbrService.open(chapter) } returns Either.Right(cbrService)

        val result = factory.create(chapter)

        assertTrue(result.isRight())
        result.onRight { assertTrue(it is CbrPageResolver) }
    }

    @Test
    fun create_deve_retornar_erro_para_formato_nao_suportado() {
        val chapter = ChapterFileDto(1, "ch", "file.pdf", "1")

        val result = factory.create(chapter)

        assertTrue(result.isLeft())
        result.onLeft { assertTrue(it is ChapterError.UnsupportedFormat) }
    }
}
