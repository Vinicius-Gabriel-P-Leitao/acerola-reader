package br.acerola.manga.repository.adapter.local.manga

import arrow.core.Either
import arrow.core.right
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.adapter.metadata.comicinfo.engine.ComicInfoMangaEngine
import br.acerola.manga.fixtures.MangaDirectoryFixtures
import br.acerola.manga.fixtures.MetadataFixtures
import br.acerola.manga.local.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.dao.metadata.MangaMetadataDao
import br.acerola.manga.local.dao.metadata.relationship.AuthorDao
import br.acerola.manga.local.dao.metadata.relationship.GenreDao
import br.acerola.manga.local.dao.metadata.source.ComicInfoSourceDao
import br.acerola.manga.adapter.contract.provider.ImageProvider
import br.acerola.manga.adapter.contract.provider.MetadataProvider
import br.acerola.manga.service.artwork.CoverSaver
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ComicInfoSourceMangaEngineTest {

    @MockK lateinit var genreDao: GenreDao
    @MockK lateinit var authorDao: AuthorDao
    @MockK lateinit var directoryDao: MangaDirectoryDao
    @MockK lateinit var coverService: CoverSaver
    @MockK lateinit var mangaMetadataDao: MangaMetadataDao
    @MockK lateinit var comicInfoSourceDao: ComicInfoSourceDao
    @MockK lateinit var downloadCoverService: ImageProvider<String>
    @MockK lateinit var comicInfoSourceService: MetadataProvider<MangaMetadataDto, String>

    private lateinit var repository: ComicInfoMangaEngine

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        repository = ComicInfoMangaEngine(
            genreDao, authorDao, directoryDao, coverService,
            mangaMetadataDao, comicInfoSourceDao, downloadCoverService
        )
        repository.comicInfoSourceService = comicInfoSourceService
    }

    @Test
    fun `refreshManga deve buscar info local e salvar no banco`() = runTest {
        val mangaId = 1L
        val directory = MangaDirectoryFixtures.createMangaDirectory(id = mangaId, name = "Local Manga")
        val infoFound = MetadataFixtures.createMangaRemoteInfoDto(title = "Local Manga")

        coEvery { directoryDao.getMangaDirectoryById(mangaId) } returns directory
        coEvery {
            comicInfoSourceService.searchInfo(any(), any(), any(), any(), *anyVararg())
        } returns Either.Right(listOf(infoFound))

        every { mangaMetadataDao.getMangaByDirectoryId(mangaId) } returns flowOf(null)

        coEvery { mangaMetadataDao.insert(any()) } returns 100L
        coEvery { comicInfoSourceDao.insert(any()) } returns 1L
        coEvery { authorDao.insert(any()) } returns 1L
        coEvery { genreDao.insert(any()) } returns 1L
        coEvery { downloadCoverService.searchMedia(any()) } returns Either.Right(byteArrayOf(0, 1, 2))
        coEvery { coverService.processCover(any(), any(), any(), any(), any(), any()) } returns 1L.right()

        val result = repository.refreshManga(mangaId)

        assertTrue("Deveria ser sucesso mas foi: $result", result.isRight())
        coVerify { comicInfoSourceService.searchInfo(eq("Local Manga"), any(), any(), any(), *anyVararg()) }
    }

    @Test
    fun `refreshManga deve atualizar registro existente se ja houver`() = runTest {
        val mangaId = 1L
        val existingRemote = MetadataFixtures.createMangaRemoteInfo(id = 50L)
        val infoFound = MetadataFixtures.createMangaRemoteInfoDto()

        coEvery { directoryDao.getMangaDirectoryById(mangaId) } returns MangaDirectoryFixtures.createMangaDirectory(id = mangaId)
        coEvery { comicInfoSourceService.searchInfo(any(), any(), any(), any(), *anyVararg()) } returns Either.Right(listOf(infoFound))
        every { mangaMetadataDao.getMangaByDirectoryId(mangaId) } returns flowOf(existingRemote)

        coEvery { mangaMetadataDao.update(any()) } returns Unit
        coEvery { comicInfoSourceDao.insert(any()) } returns 1L
        coEvery { authorDao.insert(any()) } returns 1L
        coEvery { genreDao.insert(any()) } returns 1L
        coEvery { downloadCoverService.searchMedia(any()) } returns Either.Right(byteArrayOf(0, 1, 2))
        coEvery { coverService.processCover(any(), any(), any(), any(), any(), any()) } returns 1L.right()

        val result = repository.refreshManga(mangaId)

        assertTrue("Deveria ser Right mas foi $result", result.isRight())
    }
}
