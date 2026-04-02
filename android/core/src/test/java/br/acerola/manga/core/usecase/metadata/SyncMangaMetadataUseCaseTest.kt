package br.acerola.manga.core.usecase.metadata

import arrow.core.Either
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.adapter.contract.gateway.ChapterGateway
import br.acerola.manga.adapter.contract.gateway.MangaGateway
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncMangaMetadataUseCaseTest {

    @MockK lateinit var anilistMangaRepo: MangaGateway<MangaMetadataDto>
    @MockK lateinit var mangadexMangaRepo: MangaGateway<MangaMetadataDto>
    @MockK lateinit var mangadexChapterRepo: ChapterGateway<ChapterRemoteInfoPageDto>
    @MockK lateinit var comicInfoMangaRepo: MangaGateway<MangaMetadataDto>
    @MockK lateinit var comicInfoChapterRepo: ChapterGateway<ChapterRemoteInfoPageDto>

    private lateinit var useCase: SyncMangaMetadataUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        useCase = SyncMangaMetadataUseCase(
            anilistMangaRepo,
            mangadexMangaRepo,
            mangadexChapterRepo,
            comicInfoMangaRepo,
            comicInfoChapterRepo
        )
    }

    @Test
    fun syncFromMangadex_deve_chamar_manga_e_capitulos_em_sequencia() = runTest {
        val mangaId = 1L
        coEvery { mangadexMangaRepo.refreshManga(mangaId) } returns Either.Right(Unit)
        coEvery { mangadexChapterRepo.refreshMangaChapters(mangaId) } returns Either.Right(Unit)

        val result = useCase.syncFromMangadex(mangaId)

        assertTrue(result.isRight())
        coVerify(exactly = 1) { mangadexMangaRepo.refreshManga(mangaId) }
        coVerify(exactly = 1) { mangadexChapterRepo.refreshMangaChapters(mangaId) }
    }

    @Test
    fun syncFromComicInfo_deve_chamar_manga_e_capitulos_em_sequencia() = runTest {
        val mangaId = 1L
        coEvery { comicInfoMangaRepo.refreshManga(mangaId) } returns Either.Right(Unit)
        coEvery { comicInfoChapterRepo.refreshMangaChapters(mangaId) } returns Either.Right(Unit)

        val result = useCase.syncFromComicInfo(mangaId)

        assertTrue(result.isRight())
        coVerify(exactly = 1) { comicInfoMangaRepo.refreshManga(mangaId) }
        coVerify(exactly = 1) { comicInfoChapterRepo.refreshMangaChapters(mangaId) }
    }

    @Test
    fun syncFromMangadex_deve_interromper_se_manga_falhar() = runTest {
        val mangaId = 1L
        coEvery { mangadexMangaRepo.refreshManga(mangaId) } returns Either.Left(mockk())

        val result = useCase.syncFromMangadex(mangaId)

        assertTrue(result.isLeft())
        coVerify(exactly = 0) { mangadexChapterRepo.refreshMangaChapters(any()) }
    }

    @Test
    fun syncFromAnilist_deve_chamar_refreshManga_no_repositorio_anilist() = runTest {
        val mangaId = 1L
        coEvery { anilistMangaRepo.refreshManga(mangaId) } returns Either.Right(Unit)

        val result = useCase.syncFromAnilist(mangaId)

        assertTrue(result.isRight())
        coVerify(exactly = 1) { anilistMangaRepo.refreshManga(mangaId) }
    }
}
