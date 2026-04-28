package br.acerola.comic.repository.adapter.local.comic

import android.net.Uri
import arrow.core.Either
import arrow.core.right
import br.acerola.comic.adapter.contract.provider.ImageProvider
import br.acerola.comic.adapter.contract.provider.MetadataProvider
import br.acerola.comic.adapter.metadata.comicinfo.engine.ComicInfoComicEngine
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.fixtures.MangaDirectoryFixtures
import br.acerola.comic.fixtures.MetadataFixtures
import br.acerola.comic.local.dao.archive.ComicDirectoryDao
import br.acerola.comic.local.dao.metadata.ComicMetadataDao
import br.acerola.comic.local.dao.metadata.relationship.AuthorDao
import br.acerola.comic.local.dao.metadata.relationship.GenreDao
import br.acerola.comic.local.dao.metadata.source.ComicInfoSourceDao
import br.acerola.comic.service.artwork.CoverSaver
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ComicInfoSourceMangaEngineTest {
    private val genreDao = mockk<GenreDao>(relaxed = true)
    private val authorDao = mockk<AuthorDao>(relaxed = true)
    private val directoryDao = mockk<ComicDirectoryDao>(relaxed = true)
    private val coverService = mockk<CoverSaver>(relaxed = true)
    private val comicMetadataDao = mockk<ComicMetadataDao>(relaxed = true)
    private val comicInfoSourceDao = mockk<ComicInfoSourceDao>(relaxed = true)
    private val downloadCoverService = mockk<ImageProvider<String>>(relaxed = true)
    private val comicInfoSourceService = mockk<MetadataProvider<ComicMetadataDto, String>>(relaxed = true)

    private lateinit var repository: ComicInfoComicEngine

    @Before
    fun setUp() {
        mockkStatic(Uri::class)

        val uriMock = mockk<Uri>(relaxed = true)
        every { Uri.parse(any<String>()) } returns uriMock

        repository =
            ComicInfoComicEngine(
                genreDao = genreDao,
                authorDao = authorDao,
                directoryDao = directoryDao,
                coverService = coverService,
                comicMetadataDao = comicMetadataDao,
                comicInfoSourceDao = comicInfoSourceDao,
                downloadCoverService = downloadCoverService,
            )
        repository.comicInfoSourceService = comicInfoSourceService
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun `refreshManga deve buscar info local e salvar no banco`() =
        runTest {
            val mangaId = 1L
            val directory = MangaDirectoryFixtures.createMangaDirectory(id = 1L, name = "Local Manga")
            val infoFound = MetadataFixtures.createMangaRemoteInfoDto(title = "Local Manga")

            coEvery { directoryDao.getDirectoryById(1L) } returns directory
            coEvery {
                comicInfoSourceService.searchInfo(any(), any(), any(), any(), *anyVararg())
            } returns Either.Right(listOf(infoFound))

            coEvery {
                comicMetadataDao.upsertComicWithRelationsTransaction(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns 100L

            coEvery { downloadCoverService.searchMedia(any()) } returns Either.Right(byteArrayOf(0, 1, 2))
            coEvery { coverService.processCover(any(), any(), any(), any(), any(), any()) } returns 1L.right()

            val result = repository.refreshManga(mangaId)

            assertTrue("Deveria ser sucesso mas foi: $result", result.isRight())
            coVerify { comicInfoSourceService.searchInfo(eq("Local Manga"), any(), any(), any(), *anyVararg()) }
        }
}
