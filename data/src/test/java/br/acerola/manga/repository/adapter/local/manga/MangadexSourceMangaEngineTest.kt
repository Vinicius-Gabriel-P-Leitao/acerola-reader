package br.acerola.manga.repository.adapter.local.manga

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import br.acerola.manga.config.preference.MangaDirectoryPreference
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.adapter.metadata.mangadex.engine.MangadexMangaEngine
import br.acerola.manga.fixtures.MangaDirectoryFixtures
import br.acerola.manga.fixtures.MetadataFixtures
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.database.dao.metadata.MangaRemoteInfoDao
import br.acerola.manga.local.database.dao.metadata.relationship.AuthorDao
import br.acerola.manga.local.database.dao.metadata.relationship.GenreDao
import br.acerola.manga.local.database.dao.metadata.source.MangadexSourceDao
import br.acerola.manga.adapter.contract.ImageFetchPort
import br.acerola.manga.adapter.contract.RemoteInfoOperationsPort
import br.acerola.manga.service.artwork.MangaSaveCoverService
import br.acerola.manga.service.metadata.MangaMetadataExportService
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
    @MockK lateinit var directoryDao: MangaDirectoryDao
    @MockK lateinit var coverService: MangaSaveCoverService
    @MockK lateinit var mangaRemoteInfoDao: MangaRemoteInfoDao
    @MockK lateinit var mangadexSourceDao: MangadexSourceDao
    @MockK lateinit var metadataExportService: MangaMetadataExportService
    @MockK lateinit var downloadCoverService: ImageFetchPort<String>
    @MockK lateinit var mangadexMangaInfoService: RemoteInfoOperationsPort<MangaRemoteInfoDto, String>
    @MockK lateinit var mangadexChapterInfoService: RemoteInfoOperationsPort<ChapterRemoteInfoDto, String>

    private lateinit var repository: MangadexMangaEngine
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        repository = MangadexMangaEngine(
            genreDao, authorDao, directoryDao, coverService, mangadexSourceDao,
            mangaRemoteInfoDao, context, metadataExportService, downloadCoverService
        )
        repository.mangadexMangaInfoService = mangadexMangaInfoService
        repository.mangadexChapterInfoService = mangadexChapterInfoService

        mockkObject(MangaDirectoryPreference)
        mockkStatic(Uri::class)
        mockkStatic(DocumentFile::class)
        every { Uri.parse(any()) } returns mockk()
        every { MangaDirectoryPreference.folderUriFlow(any()) } returns flowOf("content://root")
    }

    @After
    fun tearDown() {
        unmockkObject(MangaDirectoryPreference)
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
        every { mangaRemoteInfoDao.getMangaByDirectoryId(mangaId) } returns flowOf(null)

        coEvery { mangaRemoteInfoDao.insert(any()) } returns 2L
        coEvery { mangadexSourceDao.insert(any()) } returns 1L
        coEvery { authorDao.insert(any()) } returns 1L
        coEvery { genreDao.insert(any()) } returns 1L
        coEvery { downloadCoverService.searchCover(any()) } returns Either.Right(byteArrayOf(0, 1, 2))
        coEvery { coverService.processCover(any(), any(), any(), any(), any(), any()) } returns 1L
        coEvery { metadataExportService.exportMangaMetadata(any(), any()) } returns Either.Right(Unit)

        val result = repository.refreshManga(mangaId)

        assertTrue("Refresh falhou: $result", result.isRight())
        coVerify { mangadexMangaInfoService.searchInfo(any(), any(), any(), any(), *anyVararg()) }
    }
}
