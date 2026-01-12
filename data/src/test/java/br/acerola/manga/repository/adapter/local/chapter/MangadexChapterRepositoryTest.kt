package br.acerola.manga.repository.adapter.local.chapter

import arrow.core.Either
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.fixtures.MangaDirectoryFixtures
import br.acerola.manga.fixtures.MetadataFixtures
import br.acerola.manga.local.database.dao.archive.ChapterArchiveDao
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.database.dao.metadata.ChapterDownloadSourceDao
import br.acerola.manga.local.database.dao.metadata.ChapterRemoteInfoDao
import br.acerola.manga.local.database.dao.metadata.MangaRemoteInfoDao
import br.acerola.manga.local.database.entity.archive.ChapterArchive
import br.acerola.manga.repository.port.RemoteInfoOperationsRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MangadexChapterRepositoryTest {

    @MockK
    lateinit var chapterArchiveDao: ChapterArchiveDao

    @MockK
    lateinit var mangaRemoteInfoDao: MangaRemoteInfoDao

    @MockK
    lateinit var directoryDao: MangaDirectoryDao

    @MockK
    lateinit var chapterRemoteInfoDao: ChapterRemoteInfoDao

    @MockK
    lateinit var chapterDownloadSourceDao: ChapterDownloadSourceDao

    @MockK
    lateinit var mangadexChapterInfoService: RemoteInfoOperationsRepository<ChapterRemoteInfoDto, String>

    private lateinit var repository: MangadexChapterRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        repository = MangadexChapterRepository(
            chapterArchiveDao, mangaRemoteInfoDao, directoryDao, chapterRemoteInfoDao, chapterDownloadSourceDao
        )
        repository.mangadexChapterInfoService = mangadexChapterInfoService
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `refreshMangaChapters deve sincronizar capitulos remotos com locais`() = runTest {
        val mangaId = 1L
        val remoteManga = MetadataFixtures.createMangaRemoteInfo(id = mangaId, title = "Naruto", mirrorId = "manga-123")
        val localDir = MangaDirectoryFixtures.createMangaDirectory(id = 10, name = "Naruto")

        // Local: Chapter 1
        val localChapters = listOf(
            ChapterArchive(id = 1, chapter = "1", path = "p", chapterSort = "1", folderPathFk = 10)
        )
        // Remote: Chapter 1
        val remoteChapters = listOf(
            MetadataFixtures.createChapterRemoteInfoDto(
                chapter = "1",
                mangadexVersion = 1
            ).copy(pageUrls = listOf("http://page1.jpg"))
        )

        every { mangaRemoteInfoDao.getMangaById(mangaId) } returns flowOf(remoteManga)
        coEvery {
            mangadexChapterInfoService.searchInfo(
                manga = "manga-123",
                limit = 100,
                onProgress = any()
            )
        } returns Either.Right(remoteChapters)
        coEvery { directoryDao.getMangaDirectoryByName("Naruto") } returns localDir
        every { chapterArchiveDao.getChaptersByMangaDirectory(10) } returns flowOf(localChapters)

        coEvery { chapterRemoteInfoDao.insert(any()) } returns 50L
        coEvery { chapterDownloadSourceDao.insertAll(*anyVararg()) } returns longArrayOf(1)

        val result = repository.refreshMangaChapters(mangaId)

        assertTrue("Falha no refresh: $result", result.isRight())

        coVerify { mangadexChapterInfoService.searchInfo(any(), any(), any(), any()) }
        coVerify { chapterRemoteInfoDao.insert(any()) }
    }

    @Test
    fun `refreshMangaChapters deve falhar se nao encontrar match entre local e remoto`() = runTest {
        val mangaId = 1L
        val remoteManga = MetadataFixtures.createMangaRemoteInfo(id = mangaId)
        val localDir = MangaDirectoryFixtures.createMangaDirectory()

        val localChapters = listOf(ChapterArchive(chapter = "2", chapterSort = "2", folderPathFk = 1, path = ""))
        val remoteChapters = listOf(MetadataFixtures.createChapterRemoteInfoDto(chapter = "1"))

        every { mangaRemoteInfoDao.getMangaById(mangaId) } returns flowOf(remoteManga)
        coEvery { mangadexChapterInfoService.searchInfo(any(), any(), any(), any()) } returns Either.Right(
            remoteChapters
        )
        coEvery { directoryDao.getMangaDirectoryByName(any()) } returns localDir
        every { chapterArchiveDao.getChaptersByMangaDirectory(any()) } returns flowOf(localChapters)

        val result = repository.refreshMangaChapters(mangaId)

        assertTrue(result.isLeft())
        result.onLeft { assertTrue(it is LibrarySyncError.MangadexError) }
    }

    @Test
    fun `observeChapters deve retornar lista com fontes de download`() = runTest {
        val mangaId = 1L
        val chapters = listOf(MetadataFixtures.createChapterRemoteInfo(id = 100))
        val sources = listOf(MetadataFixtures.createChapterDownloadSource(chapterFk = 100))

        every { chapterRemoteInfoDao.getChaptersByMangaRemoteInfo(mangaId) } returns flowOf(chapters)
        every { chapterDownloadSourceDao.getChapterDownloadSourceByRemoteInfoId(listOf(100L)) } returns flowOf(sources)

        // Filtra para pegar o primeiro estado não vazio
        val result = repository.observeChapters(mangaId).first { it.items.isNotEmpty() }

        assertEquals(1, result.items.size)
        assertEquals(1, result.items[0].source.size)
    }
}
