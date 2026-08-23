package br.acerola.comic.usecase.sync

import arrow.core.Either
import br.acerola.comic.pattern.metadata.MetadataSource
import br.acerola.comic.usecase.metadata.SyncComicMetadataUseCase
import br.acerola.comic.worker.contract.SyncType
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import br.acerola.comic.usecase.library.SyncLibraryUseCase as LegacySyncLibraryUseCase

class SyncMetadataUseCaseTest {
    @MockK
    lateinit var syncComicMetadataUseCase: SyncComicMetadataUseCase

    @MockK
    lateinit var anilistSyncUseCase: LegacySyncLibraryUseCase

    @MockK
    lateinit var mangadexSyncUseCase: LegacySyncLibraryUseCase

    @MockK
    lateinit var comicInfoSyncUseCase: LegacySyncLibraryUseCase

    private lateinit var useCase: SyncMetadataUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        useCase =
            SyncMetadataUseCase(
                syncComicMetadataUseCase,
                anilistSyncUseCase,
                mangadexSyncUseCase,
                comicInfoSyncUseCase,
            )
    }

    @Test
    fun `execute with directoryId should call syncComicMetadataUseCase`() =
        runTest {
            coEvery { syncComicMetadataUseCase.syncFromMangadex(42L) } returns Either.Right(Unit)

            val result = useCase.execute(MetadataSource.MANGADEX, SyncType.SYNC, 42L)

            assertTrue(result.isRight())
            coVerify { syncComicMetadataUseCase.syncFromMangadex(42L) }
        }

    @Test
    fun `execute SYNC for library on Mangadex should call mangadexSyncUseCase sync`() =
        runTest {
            coEvery { mangadexSyncUseCase.sync(null) } returns Either.Right(Unit)

            val result = useCase.execute(MetadataSource.MANGADEX, SyncType.SYNC, null)

            assertTrue(result.isRight())
            coVerify { mangadexSyncUseCase.sync(null) }
        }

    @Test
    fun `execute RESCAN for library on Anilist should call anilistSyncUseCase rescan`() =
        runTest {
            coEvery { anilistSyncUseCase.rescan(null) } returns Either.Right(Unit)

            val result = useCase.execute(MetadataSource.ANILIST, SyncType.RESCAN, null)

            assertTrue(result.isRight())
            coVerify { anilistSyncUseCase.rescan(null) }
        }
}
