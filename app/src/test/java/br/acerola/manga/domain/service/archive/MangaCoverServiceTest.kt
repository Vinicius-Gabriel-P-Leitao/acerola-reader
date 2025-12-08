package br.acerola.manga.domain.service.archive

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import br.acerola.manga.domain.database.dao.FakeMangaDexDownloadDao
import br.acerola.manga.domain.database.dao.FakeMangaFolderDao
import br.acerola.manga.domain.database.dao.database.metadata.cover.CoverDao
import br.acerola.manga.domain.model.archive.MangaFolder
import br.acerola.manga.domain.model.metadata.cover.Cover
import br.acerola.manga.domain.service.api.mangadex.MangaDexFetchCoverService
import br.acerola.manga.shared.dto.metadata.CoverDto
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class MangaCoverServiceTest {

    private lateinit var fakeFolderDao: FakeMangaFolderDao
    private lateinit var fakeCoverDao: FakeCoverDao
    private lateinit var fakeDownloadDao: FakeMangaDexDownloadDao
    private lateinit var fetchCoverService: MangaDexFetchCoverService
    private lateinit var service: MangaCoverService
    private lateinit var context: Context

    class FakeCoverDao : CoverDao {
        val covers = mutableListOf<Cover>()
        override suspend fun insert(entity: Cover): Long {
            covers.add(entity)
            return entity.id
        }

        override suspend fun insertAll(vararg entity: Cover) {
            covers.addAll(entity)
        }

        override suspend fun update(entity: Cover) {
            val idx = covers.indexOfFirst { it.id == entity.id }
            if (idx != -1) covers[idx] = entity
        }

        override suspend fun delete(entity: Cover) {
            covers.remove(entity)
        }

        override suspend fun getCoverByMirrorId(mirrorId: String): Cover? = covers.find { it.mirrorId == mirrorId }
    }

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        fakeFolderDao = FakeMangaFolderDao()
        fakeCoverDao = FakeCoverDao()
        fakeDownloadDao = FakeMangaDexDownloadDao()
        fakeDownloadDao.responseBytes = byteArrayOf(1, 2, 3)
        
        fetchCoverService = MangaDexFetchCoverService(fakeDownloadDao)
        service = MangaCoverService(context, fakeCoverDao, fakeFolderDao, fetchCoverService)

        mockkStatic(DocumentFile::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(DocumentFile::class)
    }

    @Test
    fun processCover_success_downloadsAndSavesCover() = runBlocking {
        val folderId = 1L
        val mangaFolder = MangaFolder(id = folderId, name = "Manga1", path = "content://tree/manga1", cover = null, banner = null, lastModified = 0, chapterTemplate = null)
        fakeFolderDao.folders.add(mangaFolder)
        
        val coverDto = CoverDto(id = "cover1", fileName = "cover.jpg", url = "http://example.com/cover.jpg")
        val rootUri = mockk<Uri>()
        
        val mockRoot = mockk<DocumentFile>(relaxed = true)
        val mockMangaDir = mockk<DocumentFile>(relaxed = true)
        val mockNewFile = mockk<DocumentFile>(relaxed = true)
        
        every { DocumentFile.fromTreeUri(any(), any()) } returns mockRoot
        every { mockRoot.exists() } returns true
        every { mockRoot.findFile("Manga1") } returns null
        every { mockRoot.createDirectory("Manga1") } returns mockMangaDir
        every { mockMangaDir.canWrite() } returns true
        every { mockMangaDir.findFile("cover.png") } returns null
        every { mockMangaDir.createFile("image/png", "cover.png") } returns mockNewFile

        val mockUri = mockk<Uri>()
        every { mockUri.toString() } returns "content://tree/manga1/cover.png"
        every { mockNewFile.uri } returns mockUri

        val resultId = service.processCover(rootUri, folderId, coverDto, "Manga1")

        assertEquals(1, fakeCoverDao.covers.size)
        assertEquals("cover1", fakeCoverDao.covers[0].mirrorId)
        
        val updatedFolder = fakeFolderDao.getMangaFolderById(folderId)
        assertNotNull(updatedFolder?.cover)
        assertEquals("content://tree/manga1/cover.png", updatedFolder?.cover)
    }
}
