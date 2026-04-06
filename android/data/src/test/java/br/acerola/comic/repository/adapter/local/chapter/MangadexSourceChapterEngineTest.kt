package br.acerola.comic.repository.adapter.local.chapter

import br.acerola.comic.adapter.contract.provider.MetadataProvider
import br.acerola.comic.adapter.metadata.mangadex.engine.MangadexChapterEngine
import br.acerola.comic.dto.metadata.chapter.ChapterMetadataDto
import br.acerola.comic.local.dao.archive.ChapterArchiveDao
import br.acerola.comic.local.dao.archive.ComicDirectoryDao
import br.acerola.comic.local.dao.metadata.ChapterDownloadSourceDao
import br.acerola.comic.local.dao.metadata.ChapterMetadataDao
import br.acerola.comic.local.dao.metadata.ComicMetadataDao
import br.acerola.comic.service.metadata.MetadataExporter
import io.mockk.MockKAnnotations
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
    @MockK lateinit var comicMetadataDao: ComicMetadataDao
    @MockK lateinit var directoryDao: ComicDirectoryDao
    @MockK lateinit var chapterMetadataDao: ChapterMetadataDao
    @MockK lateinit var chapterDownloadSourceDao: ChapterDownloadSourceDao
    @MockK lateinit var metadataExportService: MetadataExporter
    @MockK lateinit var mangadexChapterInfoService: MetadataProvider<ChapterMetadataDto, String>

    private lateinit var repository: MangadexChapterEngine
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        repository = MangadexChapterEngine(
            directoryDao, chapterArchiveDao, comicMetadataDao,
            chapterMetadataDao, metadataExportService, chapterDownloadSourceDao
        )
        repository.mangadexSourceChapterInfoService = mangadexChapterInfoService
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `refreshMangaChapters deve retornar sucesso se não houver mangadexId`() = runTest {
        every { comicMetadataDao.getComicWithRelationsByDirectoryId(any()) } returns flowOf(null)

        val result = repository.refreshComicChapters(1L)

        assertTrue(result.isRight())
    }
}
