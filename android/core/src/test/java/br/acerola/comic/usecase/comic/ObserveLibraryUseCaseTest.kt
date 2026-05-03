package br.acerola.comic.usecase.comic

import br.acerola.comic.adapter.contract.gateway.ComicGateway
import br.acerola.comic.dto.archive.ComicDirectoryDto
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
    lateinit var repository: ComicGateway<ComicDirectoryDto>
    private lateinit var useCase: ObserveLibraryUseCase<ComicDirectoryDto>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { repository.progress } returns MutableStateFlow(value = 0)
        every { repository.isIndexing } returns MutableStateFlow(value = false)
        useCase = ObserveLibraryUseCase(syncGateway = repository, comicRepository = repository)
    }

    @Test
    fun `invoke deve retornar fluxo da biblioteca`() =
        runTest {
            val list = listOf(mockk<ComicDirectoryDto>())
            every { repository.observeLibrary() } returns MutableStateFlow(value = list)

            val result = useCase().first()

            assertEquals(list, result)
            coVerify { repository.observeLibrary() }
        }
}
