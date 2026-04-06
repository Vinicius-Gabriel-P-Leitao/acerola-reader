package br.acerola.comic.repository.adapter.local.comic

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import arrow.core.right
import br.acerola.comic.config.preference.ComicDirectoryPreference
import br.acerola.comic.dto.metadata.chapter.ChapterMetadataDto
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.adapter.metadata.mangadex.engine.MangadexComicEngine
import br.acerola.comic.fixtures.MangaDirectoryFixtures
import br.acerola.comic.fixtures.MetadataFixtures
import br.acerola.comic.local.dao.archive.ComicDirectoryDao
import br.acerola.comic.local.dao.metadata.ComicMetadataDao
import br.acerola.comic.local.dao.metadata.relationship.AuthorDao
import br.acerola.comic.local.dao.metadata.relationship.GenreDao
import br.acerola.comic.local.dao.metadata.source.MangadexSourceDao
import br.acerola.comic.adapter.contract.provider.ImageProvider
import br.acerola.comic.adapter.contract.provider.MetadataProvider
import br.acerola.comic.service.artwork.CoverSaver
import br.acerola.comic.service.metadata.MetadataExporter
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
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
class MangadexSourceMangaEngineTest {

    @MockK lateinit var context: Context
    @MockK lateinit var genreDao: GenreDao
    @MockK lateinit var authorDao: AuthorDao
    @MockK lateinit var directoryDao: ComicDirectoryDao
    @MockK lateinit var coverService: CoverSaver
    @MockK lateinit var comicMetadataDao: ComicMetadataDao
    @MockK lateinit var mangadexSourceDao: MangadexSourceDao
    @MockK lateinit var metadataExportService: MetadataExporter
    @MockK lateinit var downloadCoverService: ImageProvider<String>
    @MockK lateinit var mangadexMangaInfoService: MetadataProvider<ComicMetadataDto, String>
    @MockK lateinit var mangadexChapterInfoService: MetadataProvider<ChapterMetadataDto, String>

    private lateinit var repository: MangadexComicEngine
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        repository = MangadexComicEngine(
            genreDao, authorDao, directoryDao, coverService, mangadexSourceDao,
            comicMetadataDao, metadataExportService, context, downloadCoverService
        )
        repository.mangadexSourceMangaInfoService = mangadexMangaInfoService
        repository.mangadexSourceChapterInfoService = mangadexChapterInfoService

        mockkObject(ComicDirectoryPreference)
        mockkStatic(Uri::class)
        mockkStatic(DocumentFile::class)
        every { Uri.parse(any()) } returns mockk()
        every { ComicDirectoryPreference.folderUriFlow(any()) } returns flowOf("content://root")
    }

    @After
    fun tearDown() {
        unmockkObject(ComicDirectoryPreference)
        unmockkStatic(Uri::class)
        unmockkStatic(DocumentFile::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `refreshManga deve buscar metadados e atualizar se encontrar match`() = runTest {
        val mangaId = 1L
        val dir = MangaDirectoryFixtures.createMangaDirectory(id = mangaId, name = "Berserk")
        val fetchResult = listOf(MetadataFixtures.createMangaRemoteInfoDto(title = "Berserk"))

        coEvery { directoryDao.getMangaDirectoryById(mangaId) } returns dir
        coEvery { mangadexMangaInfoService.searchInfo(any(), any(), any(), any(), *anyVararg()) } returns Either.Right(fetchResult)
        every { comicMetadataDao.getComicByDirectoryId(mangaId) } returns flowOf(null)

        coEvery { comicMetadataDao.insert(any()) } returns 2L
        coEvery { mangadexSourceDao.insert(any()) } returns 1L
        coEvery { authorDao.insert(any()) } returns 1L
        coEvery { genreDao.insert(any()) } returns 1L
        coEvery { downloadCoverService.searchMedia(any()) } returns Either.Right(byteArrayOf(0, 1, 2))
        coEvery { coverService.processCover(any(), any(), any(), any(), any(), any()) } returns 1L.right()
        coEvery { metadataExportService.exportMangaMetadata(any(), any()) } returns Either.Right(Unit)

        val result = repository.refreshManga(mangaId)

        assertTrue("Refresh falhou: $result", result.isRight())
        coVerify { mangadexMangaInfoService.searchInfo(any(), any(), any(), any(), *anyVararg()) }
    }
}
