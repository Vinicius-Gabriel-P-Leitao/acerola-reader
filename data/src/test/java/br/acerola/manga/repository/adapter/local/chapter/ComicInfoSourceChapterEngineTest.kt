package br.acerola.manga.repository.adapter.local.chapter

import arrow.core.Either
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.adapter.metadata.comicinfo.engine.ComicInfoChapterEngine
import br.acerola.manga.fixtures.MangaDirectoryFixtures
import br.acerola.manga.fixtures.MetadataFixtures
import br.acerola.manga.local.database.dao.archive.ChapterArchiveDao
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.database.dao.metadata.ChapterDownloadSourceDao
import br.acerola.manga.local.database.dao.metadata.ChapterRemoteInfoDao
import br.acerola.manga.local.database.dao.metadata.MangaRemoteInfoDao
import br.acerola.manga.local.database.entity.archive.ChapterArchive
import br.acerola.manga.adapter.contract.RemoteInfoOperationsPort
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ComicInfoSourceChapterEngineTest {

    @MockK lateinit var directoryDao: MangaDirectoryDao
    @MockK lateinit var chapterArchiveDao: ChapterArchiveDao
    @MockK lateinit var mangaRemoteInfoDao: MangaRemoteInfoDao
    @MockK lateinit var chapterRemoteInfoDao: ChapterRemoteInfoDao
    @MockK lateinit var chapterDownloadSourceDao: ChapterDownloadSourceDao
    @MockK lateinit var comicInfoService: RemoteInfoOperationsPort<ChapterRemoteInfoDto, String>

    private lateinit var repository: ComicInfoChapterEngine

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        repository = ComicInfoChapterEngine(
            directoryDao,
            chapterArchiveDao,
            mangaRemoteInfoDao,
            chapterRemoteInfoDao,
            chapterDownloadSourceDao
        )
        repository.comicInfoService = comicInfoService
    }

    @Test
    fun refreshMangaChapters_deve_iterar_arquivos_locais_e_extrair_metadados() = runTest {
        // Arrange
        val mangaId = 1L
        val directory = MangaDirectoryFixtures.createMangaDirectory(id = mangaId)
        val remoteManga = MetadataFixtures.createMangaRemoteInfo(id = 10L)
        val localChapters = listOf(
            ChapterArchive(id = 1, chapter = "1", path = "path/1.cbz", chapterSort = "1", folderPathFk = mangaId)
        )
        val extractedInfo = MetadataFixtures.createChapterRemoteInfoDto(chapter = "1")

        coEvery { directoryDao.getMangaDirectoryById(mangaId) } returns directory
        every { mangaRemoteInfoDao.getMangaByDirectoryId(mangaId) } returns flowOf(remoteManga)
        every { chapterArchiveDao.getChaptersByMangaDirectory(mangaId) } returns flowOf(localChapters)
        
        coEvery { comicInfoService.searchInfo(manga = "path/1.cbz") } returns Either.Right(listOf(extractedInfo))
        coEvery { chapterRemoteInfoDao.insert(any()) } returns 100L
        coEvery { chapterDownloadSourceDao.insertAll(*anyVararg()) } returns longArrayOf(1)

        // Act
        val result = repository.refreshMangaChapters(mangaId)

        // Assert
        assertTrue(result.isRight())
        coVerify { comicInfoService.searchInfo(manga = "path/1.cbz") }
        coVerify { chapterRemoteInfoDao.insert(match { it.chapter == "1" && it.mangaRemoteInfoFk == 10L }) }
    }

    @Test
    fun refreshMangaChapters_deve_ignorar_capitulo_se_falhar_em_extrair_metadados() = runTest {
        // Arrange
        val mangaId = 1L
        val localChapters = listOf(
            ChapterArchive(chapter = "1", path = "fail.cbz", chapterSort = "1", folderPathFk = mangaId)
        )

        coEvery { directoryDao.getMangaDirectoryById(any()) } returns MangaDirectoryFixtures.createMangaDirectory()
        every { mangaRemoteInfoDao.getMangaByDirectoryId(any()) } returns flowOf(MetadataFixtures.createMangaRemoteInfo())
        every { chapterArchiveDao.getChaptersByMangaDirectory(any()) } returns flowOf(localChapters)
        
        // Simula falha na extração (ex: arquivo sem ComicInfo.xml)
        coEvery { comicInfoService.searchInfo(any()) } returns Either.Left(mockk())

        // Act
        val result = repository.refreshMangaChapters(mangaId)

        // Assert
        assertTrue(result.isRight()) // O processo continua para os outros capítulos
        coVerify(exactly = 0) { chapterRemoteInfoDao.insert(any()) }
    }
}
