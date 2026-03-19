package br.acerola.manga.repository.adapter.local.chapter

import arrow.core.Either
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.adapter.mangadex.engine.MangadexChapterEngine
import br.acerola.manga.fixtures.MangaDirectoryFixtures
import br.acerola.manga.fixtures.MetadataFixtures
import br.acerola.manga.local.database.dao.archive.ChapterArchiveDao
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.database.dao.metadata.ChapterDownloadSourceDao
import br.acerola.manga.local.database.dao.metadata.ChapterRemoteInfoDao
import br.acerola.manga.local.database.dao.metadata.MangaRemoteInfoDao
import br.acerola.manga.local.database.entity.archive.ChapterArchive
import br.acerola.manga.local.database.entity.metadata.source.MangadexSource
import br.acerola.manga.adapter.port.RemoteInfoOperationsPort
import br.acerola.manga.service.metadata.MangaMetadataExportService
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MangadexSourceChapterEngineTest {

    @MockK lateinit var chapterArchiveDao: ChapterArchiveDao
    @MockK lateinit var mangaRemoteInfoDao: MangaRemoteInfoDao
    @MockK lateinit var directoryDao: MangaDirectoryDao
    @MockK lateinit var chapterRemoteInfoDao: ChapterRemoteInfoDao
    @MockK lateinit var chapterDownloadSourceDao: ChapterDownloadSourceDao
    @MockK lateinit var metadataExportService: MangaMetadataExportService
    @MockK lateinit var mangadexChapterInfoService: RemoteInfoOperationsPort<ChapterRemoteInfoDto, String>

    private lateinit var repository: MangadexChapterEngine
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        repository = MangadexChapterEngine(
            directoryDao = directoryDao,
            chapterArchiveDao = chapterArchiveDao,
            mangaRemoteInfoDao = mangaRemoteInfoDao,
            chapterRemoteInfoDao = chapterRemoteInfoDao,
            metadataExportService = metadataExportService,
            chapterDownloadSourceDao = chapterDownloadSourceDao
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
        val remoteManga = MetadataFixtures.createMangaRemoteInfo(id = mangaId)
        val mangadexSource = MangadexSource(
            mangadexId = "manga-123",
            anilistId = null,
            amazonUrl = null,
            ebookjapanUrl = null,
            rawUrl = null,
            engtlUrl = null,
            mangaRemoteInfoFk = mangaId
        )
        val relations = MetadataFixtures.createRemoteInfoRelations(
            remoteInfo = remoteManga,
            mangadexSource = mangadexSource
        )
        val localDir = MangaDirectoryFixtures.createMangaDirectory(id = mangaId)

        val localChapters = listOf(ChapterArchive(id = 1, chapter = "1", path = "p", chapterSort = "1", folderPathFk = mangaId))
        val remoteChapters = listOf(MetadataFixtures.createChapterRemoteInfoDto(chapter = "1"))

        every { mangaRemoteInfoDao.getMangaWithRelationsByDirectoryId(mangaId) } returns flowOf(relations)
        coEvery { 
            mangadexChapterInfoService.searchInfo(any(), any(), any(), any(), *anyVararg()) 
        } returns Either.Right(remoteChapters)
        
        coEvery { directoryDao.getMangaDirectoryById(mangaId) } returns localDir
        every { chapterArchiveDao.getChaptersByMangaDirectory(mangaId) } returns flowOf(localChapters)

        coEvery { chapterRemoteInfoDao.insert(any()) } returns 50L
        coEvery { chapterDownloadSourceDao.insertAll(*anyVararg()) } returns longArrayOf(1)
        coEvery { metadataExportService.exportFull(any(), any()) } returns Either.Right(Unit)

        val result = repository.refreshMangaChapters(mangaId)

        assertTrue("Deveria ser Right mas foi: $result", result.isRight())
    }

    @Test
    fun `refreshMangaChapters deve falhar se nao encontrar match entre local e remoto`() = runTest {
        val mangaId = 1L
        val remoteManga = MetadataFixtures.createMangaRemoteInfo(id = mangaId)
        val mangadexSource = MangadexSource(
            mangadexId = "manga-123",
            anilistId = null,
            amazonUrl = null,
            ebookjapanUrl = null,
            rawUrl = null,
            engtlUrl = null,
            mangaRemoteInfoFk = mangaId
        )
        val relations = MetadataFixtures.createRemoteInfoRelations(
            remoteInfo = remoteManga,
            mangadexSource = mangadexSource
        )
        val localDir = MangaDirectoryFixtures.createMangaDirectory(id = mangaId)

        val localChapters = listOf(ChapterArchive(chapter = "2", chapterSort = "2", folderPathFk = mangaId, path = ""))
        val remoteChapters = listOf(MetadataFixtures.createChapterRemoteInfoDto(chapter = "1"))

        every { mangaRemoteInfoDao.getMangaWithRelationsByDirectoryId(mangaId) } returns flowOf(relations)
        coEvery { 
            mangadexChapterInfoService.searchInfo(any(), any(), any(), any(), *anyVararg()) 
        } returns Either.Right(remoteChapters)
        
        coEvery { directoryDao.getMangaDirectoryById(mangaId) } returns localDir
        every { chapterArchiveDao.getChaptersByMangaDirectory(mangaId) } returns flowOf(localChapters)

        val result = repository.refreshMangaChapters(mangaId)

        assertTrue("Deveria ser Left mas foi: $result", result.isLeft())
    }
}
