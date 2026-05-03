package br.acerola.comic.usecase.metadata

import arrow.core.Either
import br.acerola.comic.adapter.contract.gateway.ChapterSyncGateway
import br.acerola.comic.adapter.contract.gateway.ComicSingleSyncGateway
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncComicMetadataUseCaseTest {
    @MockK lateinit var anilistMangaRepo: ComicSingleSyncGateway

    @MockK lateinit var mangadexMangaRepo: ComicSingleSyncGateway

    @MockK lateinit var mangadexChapterRepo: ChapterSyncGateway

    @MockK lateinit var comicInfoMangaRepo: ComicSingleSyncGateway

    @MockK lateinit var comicInfoChapterRepo: ChapterSyncGateway

    private lateinit var useCase: SyncComicMetadataUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        useCase =
            SyncComicMetadataUseCase(
                anilistMangaRepo,
                mangadexMangaRepo,
                mangadexChapterRepo,
                comicInfoMangaRepo,
                comicInfoChapterRepo,
            )
    }

    @Test
    fun syncFromMangadex_deve_chamar_comic_e_capitulos_em_sequencia() =
        runTest {
            val comicId = 1L
            coEvery { mangadexMangaRepo.refreshManga(comicId) } returns Either.Right(Unit)
            coEvery { mangadexChapterRepo.refreshComicChapters(comicId) } returns Either.Right(Unit)

            val result = useCase.syncFromMangadex(comicId)

            assertTrue(result.isRight())
            coVerify(exactly = 1) { mangadexMangaRepo.refreshManga(comicId) }
            coVerify(exactly = 1) { mangadexChapterRepo.refreshComicChapters(comicId) }
        }

    @Test
    fun syncFromComicInfo_deve_chamar_comic_e_capitulos_em_sequencia() =
        runTest {
            val comicId = 1L
            coEvery { comicInfoMangaRepo.refreshManga(comicId) } returns Either.Right(Unit)
            coEvery { comicInfoChapterRepo.refreshComicChapters(comicId) } returns Either.Right(Unit)

            val result = useCase.syncFromComicInfo(comicId)

            assertTrue(result.isRight())
            coVerify(exactly = 1) { comicInfoMangaRepo.refreshManga(comicId) }
            coVerify(exactly = 1) { comicInfoChapterRepo.refreshComicChapters(comicId) }
        }

    @Test
    fun syncFromMangadex_deve_interromper_se_comic_falhar() =
        runTest {
            val comicId = 1L
            coEvery { mangadexMangaRepo.refreshManga(comicId) } returns Either.Left(mockk())

            val result = useCase.syncFromMangadex(comicId)

            assertTrue(result.isLeft())
            coVerify(exactly = 0) { mangadexChapterRepo.refreshComicChapters(any()) }
        }

    @Test
    fun syncFromAnilist_deve_chamar_refreshManga_no_repositorio_anilist() =
        runTest {
            val comicId = 1L
            coEvery { anilistMangaRepo.refreshManga(comicId) } returns Either.Right(Unit)

            val result = useCase.syncFromAnilist(comicId)

            assertTrue(result.isRight())
            coVerify(exactly = 1) { anilistMangaRepo.refreshManga(comicId) }
        }
}
