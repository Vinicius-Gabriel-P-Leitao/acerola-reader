package br.acerola.manga.repository.adapter.local.chapter

import br.acerola.manga.adapter.metadata.comicinfo.engine.ComicInfoChapterEngine
import br.acerola.manga.dto.metadata.chapter.ChapterMetadataDto
import br.acerola.manga.local.dao.archive.ChapterArchiveDao
import br.acerola.manga.local.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.dao.metadata.ChapterDownloadSourceDao
import br.acerola.manga.local.dao.metadata.ChapterMetadataDao
import br.acerola.manga.local.dao.metadata.MangaMetadataDao
import br.acerola.manga.adapter.contract.provider.MetadataProvider
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ComicInfoSourceChapterEngineTest {

    @MockK lateinit var directoryDao: MangaDirectoryDao
    @MockK lateinit var chapterArchiveDao: ChapterArchiveDao
    @MockK lateinit var mangaMetadataDao: MangaMetadataDao
    @MockK lateinit var chapterMetadataDao: ChapterMetadataDao
    @MockK lateinit var chapterDownloadSourceDao: ChapterDownloadSourceDao
    @MockK lateinit var comicInfoSourceService: MetadataProvider<ChapterMetadataDto, String>

    private lateinit var repository: ComicInfoChapterEngine

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        repository = ComicInfoChapterEngine(
            directoryDao, chapterArchiveDao, mangaMetadataDao,
            chapterMetadataDao, chapterDownloadSourceDao
        )
        repository.comicInfoSourceService = comicInfoSourceService
    }

    @Test
    fun `refreshMangaChapters deve retornar erro se nao houver registro remoto`() = runTest {
        coEvery { directoryDao.getMangaDirectoryById(any()) } returns mockk(relaxed = true)
        every { mangaMetadataDao.getMangaByDirectoryId(any()) } returns flowOf(null)

        val result = repository.refreshMangaChapters(1L)

        assertTrue("Deveria retornar erro mas foi $result", result.isLeft())
    }
}
