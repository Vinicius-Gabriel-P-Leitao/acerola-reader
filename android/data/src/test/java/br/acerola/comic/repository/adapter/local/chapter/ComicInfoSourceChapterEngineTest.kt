package br.acerola.comic.repository.adapter.local.chapter

import br.acerola.comic.adapter.metadata.comicinfo.engine.ComicInfoChapterEngine
import br.acerola.comic.dto.metadata.chapter.ChapterMetadataDto
import br.acerola.comic.local.dao.archive.ChapterArchiveDao
import br.acerola.comic.local.dao.archive.ComicDirectoryDao
import br.acerola.comic.local.dao.metadata.ChapterDownloadSourceDao
import br.acerola.comic.local.dao.metadata.ChapterMetadataDao
import br.acerola.comic.local.dao.metadata.ComicMetadataDao
import br.acerola.comic.adapter.contract.provider.MetadataProvider
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

    @MockK lateinit var directoryDao: ComicDirectoryDao
    @MockK lateinit var chapterArchiveDao: ChapterArchiveDao
    @MockK lateinit var comicMetadataDao: ComicMetadataDao
    @MockK lateinit var chapterMetadataDao: ChapterMetadataDao
    @MockK lateinit var chapterDownloadSourceDao: ChapterDownloadSourceDao
    @MockK lateinit var comicInfoSourceService: MetadataProvider<ChapterMetadataDto, String>

    private lateinit var repository: ComicInfoChapterEngine

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        repository = ComicInfoChapterEngine(
            directoryDao, chapterArchiveDao, comicMetadataDao,
            chapterMetadataDao, chapterDownloadSourceDao
        )
        repository.comicInfoSourceService = comicInfoSourceService
    }

    @Test
    fun `refreshMangaChapters deve retornar erro se nao houver registro remoto`() = runTest {
        coEvery { directoryDao.getMangaDirectoryById(any()) } returns mockk(relaxed = true)
        every { comicMetadataDao.getComicByDirectoryId(any()) } returns flowOf(null)

        val result = repository.refreshComicChapters(1L)

        assertTrue("Deveria retornar erro mas foi $result", result.isLeft())
    }
}
