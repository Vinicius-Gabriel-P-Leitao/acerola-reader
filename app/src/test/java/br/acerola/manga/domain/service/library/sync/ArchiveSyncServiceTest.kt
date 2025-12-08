package br.acerola.manga.domain.service.library.sync

import android.content.Context
import android.net.Uri
import br.acerola.manga.domain.builder.ArchiveBuilder
import br.acerola.manga.domain.database.dao.FakeChapterFileDao
import br.acerola.manga.domain.database.dao.FakeMangaFolderDao
import br.acerola.manga.domain.model.archive.MangaFolder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ArchiveSyncServiceTest {

    private lateinit var context: Context
    private lateinit var fakeFolderDao: FakeMangaFolderDao
    private lateinit var fakeChapterDao: FakeChapterFileDao
    private lateinit var service: ArchiveSyncService

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        fakeFolderDao = FakeMangaFolderDao()
        fakeChapterDao = FakeChapterFileDao()
        service = ArchiveSyncService(context, fakeFolderDao, fakeChapterDao)

        mockkObject(ArchiveBuilder)
    }

    @After
    fun tearDown() {
        unmockkObject(ArchiveBuilder)
    }

    @Test
    fun syncMangas_addsNewFolders() = runBlocking {
        val baseUri = mockk<Uri>()
        val newFolder = MangaFolder(
            id = 0,
            name = "NewManga",
            path = "path/new",
            cover = null,
            banner = null,
            lastModified = 1000,
            chapterTemplate = null
        )

        every { ArchiveBuilder.buildLibrary(any(), any()) } returns listOf(newFolder)
        service.syncMangas(baseUri)

        assertEquals(1, fakeFolderDao.folders.size)
        assertEquals("NewManga", fakeFolderDao.folders[0].name)
    }

    @Test
    fun syncMangas_updatesExistingFolders() = runBlocking {
        val baseUri = mockk<Uri>()
        val existingFolder = MangaFolder(
            id = 1,
            name = "Manga1",
            path = "path/1",
            cover = null,
            banner = null,
            lastModified = 1000,
            chapterTemplate = null
        )
        fakeFolderDao.folders.add(existingFolder)

        val updatedFolder = existingFolder.copy(lastModified = 2000)
        every { ArchiveBuilder.buildLibrary(any(), any()) } returns listOf(updatedFolder)

        service.syncMangas(baseUri)

        assertEquals(1, fakeFolderDao.folders.size)
        assertEquals(2000L, fakeFolderDao.folders[0].lastModified)
    }
}
