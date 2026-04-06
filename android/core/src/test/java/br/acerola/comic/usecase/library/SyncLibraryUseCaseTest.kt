package br.acerola.comic.usecase.library

import android.net.Uri
import arrow.core.Either
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.adapter.contract.gateway.ComicGateway
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncLibraryUseCaseTest {

    @MockK
    lateinit var repository: ComicGateway<ComicDirectoryDto>

    private lateinit var useCase: SyncLibraryUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { repository.progress } returns MutableStateFlow(value = 0)
        every { repository.isIndexing } returns MutableStateFlow(value = false)

        useCase = SyncLibraryUseCase(repository)
    }

    @Test
    fun `sync deve chamar incrementalScan`() = runTest {
        val uri = mockk<Uri>()
        coEvery { repository.incrementalScan(baseUri = uri) } returns Either.Right(value = Unit)

        val result = useCase.sync(baseUri = uri)

        assertTrue(result.isRight())
        coVerify { repository.incrementalScan(baseUri = uri) }
    }

    @Test
    fun `rescan deve chamar refreshLibrary`() = runTest {
        val uri = mockk<Uri>()
        coEvery { repository.refreshLibrary(baseUri = uri) } returns Either.Right(value = Unit)

        val result = useCase.rescan(baseUri = uri)

        assertTrue(result.isRight())
        coVerify { repository.refreshLibrary(baseUri = uri) }
    }

    @Test
    fun `deepRescan deve chamar rebuildLibrary`() = runTest {
        val uri = mockk<Uri>()
        coEvery { repository.rebuildLibrary(baseUri = uri) } returns Either.Right(value = Unit)

        val result = useCase.deepRescan(baseUri = uri)

        assertTrue(result.isRight())
        coVerify { repository.rebuildLibrary(baseUri = uri) }
    }
}
