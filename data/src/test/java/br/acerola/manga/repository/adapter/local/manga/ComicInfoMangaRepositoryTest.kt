package br.acerola.manga.repository.adapter.local.manga

import arrow.core.Either
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.fixtures.MangaDirectoryFixtures
import br.acerola.manga.fixtures.MetadataFixtures
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.database.dao.metadata.MangaRemoteInfoDao
import br.acerola.manga.local.database.dao.metadata.relationship.AuthorDao
import br.acerola.manga.local.database.dao.metadata.relationship.GenreDao
import br.acerola.manga.local.database.dao.metadata.source.ComicInfoSourceDao
import br.acerola.manga.repository.port.BinaryOperationsRepository
import br.acerola.manga.repository.port.RemoteInfoOperationsRepository
import br.acerola.manga.service.archive.MangaSaveCoverService
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

class ComicInfoMangaRepositoryTest {

    @MockK lateinit var genreDao: GenreDao
    @MockK lateinit var authorDao: AuthorDao
    @MockK lateinit var directoryDao: MangaDirectoryDao
    @MockK lateinit var coverService: MangaSaveCoverService
    @MockK lateinit var mangaRemoteInfoDao: MangaRemoteInfoDao
    @MockK lateinit var comicInfoSourceDao: ComicInfoSourceDao
    @MockK lateinit var downloadCoverService: BinaryOperationsRepository<String>
    @MockK lateinit var comicInfoService: RemoteInfoOperationsRepository<MangaRemoteInfoDto, String>

    private lateinit var repository: ComicInfoMangaRepository

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        repository = ComicInfoMangaRepository(
            genreDao, authorDao, directoryDao, coverService,
            mangaRemoteInfoDao, comicInfoSourceDao, downloadCoverService
        )
        repository.comicInfoService = comicInfoService
    }

    @Test
    fun `refreshManga deve buscar info local e salvar no banco`() = runTest {
        val mangaId = 1L
        val directory = MangaDirectoryFixtures.createMangaDirectory(id = mangaId, name = "Local Manga")
        val infoFound = MetadataFixtures.createMangaRemoteInfoDto(title = "Local Manga")

        coEvery { directoryDao.getMangaDirectoryById(mangaId) } returns directory
        coEvery {
            comicInfoService.searchInfo(any(), any(), any(), any(), *anyVararg())
        } returns Either.Right(listOf(infoFound))

        every { mangaRemoteInfoDao.getMangaByDirectoryId(mangaId) } returns flowOf(null)

        coEvery { mangaRemoteInfoDao.insert(any()) } returns 100L
        coEvery { comicInfoSourceDao.insert(any()) } returns 1L
        coEvery { authorDao.insert(any()) } returns 1L
        coEvery { genreDao.insert(any()) } returns 1L
        coEvery { downloadCoverService.searchCover(any()) } returns Either.Right(byteArrayOf(0, 1, 2))
        coEvery { coverService.processCover(any(), any(), any(), any(), any(), any()) } returns 1L

        val result = repository.refreshManga(mangaId)

        assertTrue("Deveria ser sucesso mas foi: $result", result.isRight())
        coVerify { comicInfoService.searchInfo(eq("Local Manga"), any(), any(), any(), *anyVararg()) }
    }

    @Test
    fun `refreshManga deve atualizar registro existente se ja houver`() = runTest {
        val mangaId = 1L
        val existingRemote = MetadataFixtures.createMangaRemoteInfo(id = 50L)
        val infoFound = MetadataFixtures.createMangaRemoteInfoDto()

        coEvery { directoryDao.getMangaDirectoryById(mangaId) } returns MangaDirectoryFixtures.createMangaDirectory(id = mangaId)
        coEvery { comicInfoService.searchInfo(any(), any(), any(), any(), *anyVararg()) } returns Either.Right(listOf(infoFound))
        every { mangaRemoteInfoDao.getMangaByDirectoryId(mangaId) } returns flowOf(existingRemote)

        coEvery { mangaRemoteInfoDao.update(any()) } returns Unit
        coEvery { comicInfoSourceDao.insert(any()) } returns 1L
        coEvery { authorDao.insert(any()) } returns 1L
        coEvery { genreDao.insert(any()) } returns 1L
        coEvery { downloadCoverService.searchCover(any()) } returns Either.Right(byteArrayOf(0, 1, 2))
        coEvery { coverService.processCover(any(), any(), any(), any(), any(), any()) } returns 1L

        val result = repository.refreshManga(mangaId)

        assertTrue("Deveria ser Right mas foi $result", result.isRight())
    }
}
