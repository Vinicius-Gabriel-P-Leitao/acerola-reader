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
import br.acerola.manga.local.database.dao.metadata.ChapterDownloadSourceDao
import br.acerola.manga.local.database.dao.metadata.ChapterRemoteInfoDao
import br.acerola.manga.local.database.dao.metadata.MangaRemoteInfoDao
import br.acerola.manga.local.database.dao.metadata.author.AuthorDao
import br.acerola.manga.local.database.dao.metadata.genre.GenreDao
import br.acerola.manga.repository.port.RemoteInfoOperationsRepository
import br.acerola.manga.service.archive.MangaSaveCoverService
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
import io.mockk.verify
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
class MangadexMangaRepositoryTest {

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var genreDao: GenreDao

    @MockK
    lateinit var authorDao: AuthorDao

    @MockK
    lateinit var directoryDao: MangaDirectoryDao

    @MockK
    lateinit var coverService: MangaSaveCoverService

    @MockK
    lateinit var mangaRemoteInfoDao: MangaRemoteInfoDao

    @MockK
    lateinit var chapterRemoteInfoDao: ChapterRemoteInfoDao

    @MockK
    lateinit var chapterDownloadSourceDao: ChapterDownloadSourceDao

    @MockK
    lateinit var mangadexMangaInfoService: RemoteInfoOperationsRepository<MangaRemoteInfoDto, String>

    @MockK
    lateinit var mangadexChapterInfoService: RemoteInfoOperationsRepository<ChapterRemoteInfoDto, String>

    private lateinit var repository: MangadexMangaRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        repository = MangadexMangaRepository(
            context,
            genreDao,
            authorDao,
            directoryDao,
            coverService,
            mangaRemoteInfoDao,
            chapterRemoteInfoDao,
            chapterDownloadSourceDao
        )
        repository.mangadexMangaInfoService = mangadexMangaInfoService
        repository.mangadexChapterInfoService = mangadexChapterInfoService

        mockkObject(MangaDirectoryPreference)
        mockkStatic(Uri::class)
        mockkStatic(DocumentFile::class)
        mockkStatic("androidx.core.net.UriKt")
        every { Uri.parse(any()) } returns mockk()
        every { MangaDirectoryPreference.folderUriFlow(context) } returns flowOf("content://root")
    }

    @After
    fun tearDown() {
        unmockkObject(MangaDirectoryPreference)
        unmockkStatic(Uri::class)
        unmockkStatic(DocumentFile::class)
        unmockkStatic("androidx.core.net.UriKt")
        Dispatchers.resetMain()
    }

    @Test
    fun `refreshManga deve buscar metadados e atualizar se encontrar match`() = runTest {
        // Reforça o mock dentro do escopo do teste
        every { MangaDirectoryPreference.folderUriFlow(any()) } returns flowOf("content://root")

        val mangaId = 1L
        val remoteInfo = MetadataFixtures.createMangaRemoteInfo(id = mangaId, title = "Berserk")
        val dir = MangaDirectoryFixtures.createMangaDirectory(name = "Berserk")
        val fetchResult = listOf(MetadataFixtures.createMangaRemoteInfoDto(title = "Berserk"))

        // Mockando o Flow de retorno
        every { mangaRemoteInfoDao.getMangaById(mangaId) } returns flowOf(remoteInfo)

        coEvery { directoryDao.getMangaDirectoryByName(any()) } returns dir
        // Simplificando o mock para garantir match com chamada padrao sem varargs
        coEvery { mangadexMangaInfoService.searchInfo(any(), any(), any(), any()) } returns Either.Right(fetchResult)

        // Mocks de retorno de ID para inserts
        coEvery { mangaRemoteInfoDao.insert(any()) } returns 2L
        coEvery { authorDao.insert(any()) } returns 1L
        coEvery { genreDao.insert(any()) } returns 1L
        coEvery { coverService.processCover(any(), any(), any(), any(), any()) } returns 1L

        val result = repository.refreshManga(mangaId)

        assertTrue("Refresh falhou: $result", result.isRight())

        coVerify { directoryDao.getMangaDirectoryByName(any()) }

        // Verifica se preference foi chamado
        verify { MangaDirectoryPreference.folderUriFlow(any()) }

        // Verifica se a busca foi realizada
        coVerify { mangadexMangaInfoService.searchInfo(any(), any(), any(), any()) }

        // Verifica se houve tentativa de inserção
        coVerify { mangaRemoteInfoDao.insert(any()) }
    }

    @Test
    fun `incrementalScan deve ignorar mangas ja existentes no remoto`() = runTest {
        val localDirs = listOf(MangaDirectoryFixtures.createMangaDirectory(name = "One Piece"))
        val remoteInfos = listOf(MetadataFixtures.createMangaRemoteInfo(title = "One Piece"))

        every { directoryDao.getAllMangaDirectory() } returns flowOf(localDirs)
        every { mangaRemoteInfoDao.getAllMangaRemoteInfo() } returns flowOf(remoteInfos)

        val result = repository.incrementalScan(null)

        assertTrue(result.isRight())
        coVerify(exactly = 0) { mangadexMangaInfoService.searchInfo(any(), any(), any(), any()) }
    }

    @Test
    fun `observeLibrary deve retornar DTOs com relacionamentos`() = runTest {
        val relations = listOf(MetadataFixtures.createRemoteInfoRelations())
        every { mangaRemoteInfoDao.getAllMangasWithRelations() } returns flowOf(relations)

        val result = repository.observeLibrary().first { it.isNotEmpty() }

        assertEquals(1, result.size)
        assertEquals("Naruto", result[0].title)
    }
}