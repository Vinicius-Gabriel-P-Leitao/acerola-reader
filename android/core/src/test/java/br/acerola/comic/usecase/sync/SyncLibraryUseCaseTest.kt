package br.acerola.comic.usecase.sync

import android.net.Uri
import arrow.core.Either
import br.acerola.comic.adapter.contract.gateway.ComicLibraryScanGateway
import br.acerola.comic.adapter.contract.gateway.ComicRebuildGateway
import br.acerola.comic.adapter.contract.gateway.ComicSingleSyncGateway
import br.acerola.comic.error.message.LibrarySyncError
import br.acerola.comic.worker.contract.SyncType
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncLibraryUseCaseTest {
    @MockK
    lateinit var singleSync: ComicSingleSyncGateway
    @MockK
    lateinit var scanGateway: ComicLibraryScanGateway
    @MockK
    lateinit var rebuildGateway: ComicRebuildGateway

    private lateinit var useCase: SyncLibraryUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        useCase = SyncLibraryUseCase(singleSync, scanGateway, rebuildGateway)
    }

    @Test
    fun `execute INCREMENTAL deve chamar scanGateway incrementalScan`() = runTest {
        val uri = mockk<Uri>()
        coEvery { scanGateway.incrementalScan(uri) } returns Either.Right(Unit)

        val result = useCase.execute(SyncType.INCREMENTAL, -1L, uri)

        assertTrue(result.isRight())
        coVerify { scanGateway.incrementalScan(uri) }
    }

    @Test
    fun `execute REFRESH deve chamar scanGateway refreshLibrary`() = runTest {
        val uri = mockk<Uri>()
        coEvery { scanGateway.refreshLibrary(uri) } returns Either.Right(Unit)

        val result = useCase.execute(SyncType.REFRESH, -1L, uri)

        assertTrue(result.isRight())
        coVerify { scanGateway.refreshLibrary(uri) }
    }

    @Test
    fun `execute REBUILD deve chamar rebuildGateway rebuildLibrary`() = runTest {
        val uri = mockk<Uri>()
        coEvery { rebuildGateway.rebuildLibrary(uri) } returns Either.Right(Unit)

        val result = useCase.execute(SyncType.REBUILD, -1L, uri)

        assertTrue(result.isRight())
        coVerify { rebuildGateway.rebuildLibrary(uri) }
    }

    @Test
    fun `execute SPECIFIC com id valido deve chamar singleSync refreshManga`() = runTest {
        val uri = mockk<Uri>()
        coEvery { singleSync.refreshManga(42L, uri) } returns Either.Right(Unit)

        val result = useCase.execute(SyncType.SPECIFIC, 42L, uri)

        assertTrue(result.isRight())
        coVerify { singleSync.refreshManga(42L, uri) }
    }

    @Test
    fun `execute SPECIFIC com id invalido deve retornar erro`() = runTest {
        val result = useCase.execute(SyncType.SPECIFIC, -1L, null)

        assertTrue(result.isLeft())
        result.onLeft {
            assertTrue(it is LibrarySyncError.UnexpectedError)
        }
    }
}
