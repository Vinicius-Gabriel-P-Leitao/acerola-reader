package br.acerola.manga.repository.adapter.local.chapter

import br.acerola.manga.adapter.contract.provider.MetadataProvider
import br.acerola.manga.adapter.metadata.mangadex.engine.MangadexChapterEngine
import br.acerola.manga.dto.metadata.chapter.ChapterMetadataDto
import br.acerola.manga.local.dao.archive.ChapterArchiveDao
import br.acerola.manga.local.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.dao.metadata.ChapterDownloadSourceDao
import br.acerola.manga.local.dao.metadata.ChapterMetadataDao
import br.acerola.manga.local.dao.metadata.MangaMetadataDao
import br.acerola.manga.service.metadata.MetadataExporter
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
    @MockK lateinit var mangaMetadataDao: MangaMetadataDao
    @MockK lateinit var directoryDao: MangaDirectoryDao
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
            directoryDao, chapterArchiveDao, mangaMetadataDao,
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
        every { mangaMetadataDao.getMangaWithRelationsByDirectoryId(any()) } returns flowOf(null)

        val result = repository.refreshMangaChapters(1L)

        assertTrue(result.isRight())
    }
}
