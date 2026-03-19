package br.acerola.manga.repository.adapter.local.manga

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import br.acerola.manga.config.preference.MangaDirectoryPreference
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.fixtures.MangaDirectoryFixtures
import br.acerola.manga.fixtures.MetadataFixtures
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.database.dao.metadata.MangaRemoteInfoDao
import br.acerola.manga.local.database.dao.metadata.relationship.AuthorDao
import br.acerola.manga.local.database.dao.metadata.relationship.GenreDao
import br.acerola.manga.repository.port.RemoteInfoOperationsRepository
import br.acerola.manga.service.archive.MangaSaveCoverService
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
class MangadexMangaRepositoryTest {

    @MockK lateinit var context: Context
    @MockK lateinit var genreDao: GenreDao
    @MockK lateinit var authorDao: AuthorDao
    @MockK lateinit var directoryDao: MangaDirectoryDao
    @MockK lateinit var coverService: MangaSaveCoverService
    @MockK lateinit var mangaRemoteInfoDao: MangaRemoteInfoDao
    @MockK lateinit var metadataExportService: MangaMetadataExportService
    @MockK lateinit var mangadexMangaInfoService: RemoteInfoOperationsRepository<MangaRemoteInfoDto, String>
    @MockK lateinit var mangadexChapterInfoService: RemoteInfoOperationsRepository<ChapterRemoteInfoDto, String>

    private lateinit var repository: MangadexMangaRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        repository = MangadexMangaRepository(
            context, genreDao, authorDao, directoryDao, coverService, mangaRemoteInfoDao, metadataExportService
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
        coEvery { authorDao.insert(any()) } returns 1L
        coEvery { genreDao.insert(any()) } returns 1L
        coEvery { coverService.processCover(any(), any(), any(), any(), any()) } returns 1L
        coEvery { metadataExportService.exportMangaMetadata(any(), any()) } returns Either.Right(Unit)

        val result = repository.refreshManga(mangaId)

        assertTrue("Refresh falhou: $result", result.isRight())
        coVerify { mangadexMangaInfoService.searchInfo(any(), any(), any(), any(), *anyVararg()) }
    }
}
