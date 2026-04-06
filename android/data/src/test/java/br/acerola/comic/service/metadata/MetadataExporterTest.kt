package br.acerola.comic.service.metadata

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import br.acerola.comic.error.message.LibrarySyncError
import br.acerola.comic.fixtures.MangaDirectoryFixtures
import br.acerola.comic.fixtures.MetadataFixtures
import br.acerola.comic.local.dao.archive.ComicDirectoryDao
import br.acerola.comic.local.dao.metadata.ComicMetadataDao
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.OutputStream

class MetadataExporterTest {

    @MockK lateinit var context: Context
    @MockK lateinit var parserService: ComicInfoParser
    @MockK lateinit var directoryDao: ComicDirectoryDao
    @MockK lateinit var remoteInfoDao: ComicMetadataDao

    private lateinit var service: MetadataExporter

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        service = MetadataExporter(context, parserService, directoryDao, remoteInfoDao)
        mockkStatic(Uri::class)
        mockkStatic(DocumentFile::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
        unmockkStatic(DocumentFile::class)
    }

    @Test
    fun `exportMangaMetadata deve escrever ComicInfo se pasta existir e tiver permissao`() = runTest {
        val mangaId = 1L
        val directory = MangaDirectoryFixtures.createMangaDirectory(id = mangaId, path = "content://comic")
        val remoteInfo = MetadataFixtures.createMangaRemoteInfoDto()
        val remoteInfoEntity = MetadataFixtures.createMangaRemoteInfo(mangaDirectoryFk = mangaId, hasComicInfo = false)
        val mockUri = mockk<Uri>()
        val mockFolder = mockk<DocumentFile>()
        val mockFile = mockk<DocumentFile>()
        val mockOutputStream = mockk<OutputStream>(relaxed = true)
        val mockResolver = mockk<ContentResolver>()

        coEvery { directoryDao.getMangaDirectoryById(mangaId) } returns directory
        every { Uri.parse(any()) } returns mockUri
        every { DocumentFile.fromTreeUri(context, any()) } returns mockFolder
        every { mockFolder.exists() } returns true
        every { mockFolder.canWrite() } returns true
        every { mockFolder.findFile(any()) } returns null
        every { mockFolder.createFile(any(), any()) } returns mockFile
        every { mockFile.uri } returns mockUri

        every { parserService.serialize(remoteInfo) } returns "<Xml></Xml>"
        every { context.contentResolver } returns mockResolver
        every { mockResolver.openOutputStream(any()) } returns mockOutputStream
        
        every { remoteInfoDao.getComicByDirectoryId(mangaId) } returns flowOf(remoteInfoEntity)
        coEvery { remoteInfoDao.update(any()) } returns Unit

        val result = service.exportMangaMetadata(mangaId, remoteInfo)

        assertTrue("Deveria retornar sucesso mas foi $result", result.isRight())
        coVerify { remoteInfoDao.update(match { it.hasComicInfo }) }
    }

    @Test
    fun `exportMangaMetadata deve retornar DiskIOFailure se falhar ao criar arquivo`() = runTest {
        val mangaId = 1L
        val directory = MangaDirectoryFixtures.createMangaDirectory(id = mangaId)
        val remoteInfo = MetadataFixtures.createMangaRemoteInfoDto()
        val mockFolder = mockk<DocumentFile>()
        
        coEvery { directoryDao.getMangaDirectoryById(mangaId) } returns directory
        every { Uri.parse(any()) } returns mockk()
        every { DocumentFile.fromTreeUri(context, any()) } returns mockFolder
        every { mockFolder.exists() } returns true
        every { mockFolder.canWrite() } returns true
        every { mockFolder.findFile(any()) } returns null
        
        every { parserService.serialize(any()) } returns ""
        // Simula falha ao criar arquivo
        every { mockFolder.createFile(any(), any()) } returns null

        val result = service.exportMangaMetadata(mangaId, remoteInfo)

        assertTrue("Deveria retornar erro de disco mas foi $result", result.isLeft())
        result.onLeft { assertTrue(it is LibrarySyncError.DiskIOFailure) }
    }
}
