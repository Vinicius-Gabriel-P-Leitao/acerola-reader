package br.acerola.comic.usecase.metadata

import arrow.core.Either
import br.acerola.comic.adapter.contract.gateway.ComicSingleSyncGateway
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncComicMetadataUseCaseTest {
    @MockK lateinit var anilistMangaRepo: ComicSingleSyncGateway

    @MockK lateinit var mangadexMangaRepo: ComicSingleSyncGateway

    @MockK lateinit var comicInfoMangaRepo: ComicSingleSyncGateway

    private lateinit var useCase: SyncComicMetadataUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        useCase =
            SyncComicMetadataUseCase(
                anilistMangaRepo,
                mangadexMangaRepo,
                comicInfoMangaRepo,
            )
    }

    @Test
    fun syncFromMangadex_should_call_refreshManga() =
        runTest {
            val comicId = 1L
            coEvery { mangadexMangaRepo.refreshManga(comicId) } returns Either.Right(Unit)

            val result = useCase.syncFromMangadex(comicId)

            assertTrue(result.isRight())
            coVerify(exactly = 1) { mangadexMangaRepo.refreshManga(comicId) }
        }

    @Test
    fun syncFromComicInfo_should_call_refreshManga() =
        runTest {
            val comicId = 1L
            coEvery { comicInfoMangaRepo.refreshManga(comicId) } returns Either.Right(Unit)

            val result = useCase.syncFromComicInfo(comicId)

            assertTrue(result.isRight())
            coVerify(exactly = 1) { comicInfoMangaRepo.refreshManga(comicId) }
        }

    @Test
    fun syncFromAnilist_should_call_refreshManga_on_anilist_repository() =
        runTest {
            val comicId = 1L
            coEvery { anilistMangaRepo.refreshManga(comicId) } returns Either.Right(Unit)

            val result = useCase.syncFromAnilist(comicId)

            assertTrue(result.isRight())
            coVerify(exactly = 1) { anilistMangaRepo.refreshManga(comicId) }
        }
}
