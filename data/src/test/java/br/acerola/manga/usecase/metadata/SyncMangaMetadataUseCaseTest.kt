package br.acerola.manga.usecase.metadata

import arrow.core.Either
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.engine.port.ChapterPort
import br.acerola.manga.engine.port.MangaPort
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

    @MockK lateinit var anilistMangaRepo: MangaPort<MangaRemoteInfoDto>
    @MockK lateinit var mangadexMangaRepo: MangaPort<MangaRemoteInfoDto>
    @MockK lateinit var mangadexChapterRepo: ChapterPort<ChapterRemoteInfoPageDto>
    @MockK lateinit var comicInfoMangaRepo: MangaPort<MangaRemoteInfoDto>
    @MockK lateinit var comicInfoChapterRepo: ChapterPort<ChapterRemoteInfoPageDto>

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
}
