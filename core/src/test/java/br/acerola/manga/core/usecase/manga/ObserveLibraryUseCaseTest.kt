package br.acerola.manga.core.usecase.manga

import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.adapter.contract.MangaPort
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ObserveLibraryUseCaseTest {

    @MockK
    lateinit var repository: MangaPort<MangaDirectoryDto>
    private lateinit var useCase: ObserveLibraryUseCase<MangaDirectoryDto>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { repository.progress } returns MutableStateFlow(value = 0)
        every { repository.isIndexing } returns MutableStateFlow(value = false)
        useCase = ObserveLibraryUseCase(mangaRepository = repository)
    }

    @Test
    fun `invoke deve retornar fluxo da biblioteca`() = runTest {
        val list = listOf(mockk<MangaDirectoryDto>())
        every { repository.observeLibrary() } returns MutableStateFlow(value = list)

        val result = useCase().first()

        assertEquals(list, result)
        coVerify { repository.observeLibrary() }
    }
}
