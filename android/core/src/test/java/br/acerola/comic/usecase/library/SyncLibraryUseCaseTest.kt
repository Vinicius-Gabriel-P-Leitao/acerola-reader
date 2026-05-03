package br.acerola.comic.usecase.library

import android.net.Uri
import arrow.core.Either
import br.acerola.comic.adapter.contract.gateway.ChapterSyncStatusGateway
import br.acerola.comic.adapter.contract.gateway.ComicLibraryScanGateway
import br.acerola.comic.adapter.contract.gateway.ComicRebuildGateway
import br.acerola.comic.dto.archive.ComicDirectoryDto
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncLibraryUseCaseTest {
    @MockK
    lateinit var scanGateway: ComicLibraryScanGateway

    @MockK
    lateinit var rebuildGateway: ComicRebuildGateway

    @MockK
    lateinit var chapterGateway: ChapterSyncStatusGateway

    private lateinit var useCase: SyncLibraryUseCase

    private val scanProgress = MutableStateFlow(-1)
    private val scanIndexing = MutableStateFlow(false)
    private val rebuildProgress = MutableStateFlow(-1)
    private val rebuildIndexing = MutableStateFlow(false)
    private val chapterProgress = MutableStateFlow(-1)
    private val chapterIndexing = MutableStateFlow(false)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { scanGateway.progress } returns scanProgress
        every { scanGateway.isIndexing } returns scanIndexing
        every { rebuildGateway.progress } returns rebuildProgress
        every { rebuildGateway.isIndexing } returns rebuildIndexing
        every { chapterGateway.progress } returns chapterProgress
        every { chapterGateway.isIndexing } returns chapterIndexing

        useCase = SyncLibraryUseCase(
            scanGateway = scanGateway,
            rebuildGateway = rebuildGateway,
            chapterGateway = chapterGateway
        )
    }

    @Test
    fun `isIndexing deve ser true se qualquer gateway estiver indexando`() = runTest {
        assertFalse(useCase.isIndexing.first())

        scanIndexing.value = true
        assertTrue(useCase.isIndexing.first())

        scanIndexing.value = false
        chapterIndexing.value = true
        assertTrue(useCase.isIndexing.first())

        chapterIndexing.value = false
        rebuildIndexing.value = true
        assertTrue(useCase.isIndexing.first())
    }

    @Test
    fun `progress deve refletir o gateway ativo no fluxo de sincronizacao de metadados`() = runTest {
        // Inicial: nada acontecendo
        assertEquals(-1, useCase.progress.first())

        // 1. Inicia sync de manga (MetadataSyncWorker fase 1)
        scanIndexing.value = true
        scanProgress.value = 50
        assertEquals(50, useCase.progress.first())

        // 2. Termina manga, inicia capitulos (MetadataSyncWorker fase 2)
        scanIndexing.value = false
        scanProgress.value = -1
        chapterIndexing.value = true
        chapterProgress.value = 10
        assertEquals(10, useCase.progress.first())

        // 3. Progresso dos capitulos avanca
        chapterProgress.value = 90
        assertEquals(90, useCase.progress.first())

        // 4. Tudo finaliza
        chapterIndexing.value = false
        chapterProgress.value = -1
        assertEquals(-1, useCase.progress.first())
    }

    @Test
    fun `sync deve chamar incrementalScan`() =
        runTest {
            val uri = mockk<Uri>()
            coEvery { scanGateway.incrementalScan(baseUri = uri) } returns Either.Right(value = Unit)

            val result = useCase.sync(baseUri = uri)

            assertTrue(result.isRight())
            coVerify { scanGateway.incrementalScan(baseUri = uri) }
        }

    @Test
    fun `rescan deve chamar refreshLibrary`() =
        runTest {
            val uri = mockk<Uri>()
            coEvery { scanGateway.refreshLibrary(baseUri = uri) } returns Either.Right(value = Unit)

            val result = useCase.rescan(baseUri = uri)

            assertTrue(result.isRight())
            coVerify { scanGateway.refreshLibrary(baseUri = uri) }
        }

    @Test
    fun `deepRescan deve chamar rebuildLibrary`() =
        runTest {
            val uri = mockk<Uri>()
            coEvery { rebuildGateway.rebuildLibrary(baseUri = uri) } returns Either.Right(value = Unit)

            val result = useCase.deepRescan(baseUri = uri)

            assertTrue(result.isRight())
            coVerify { rebuildGateway.rebuildLibrary(baseUri = uri) }
        }
}
