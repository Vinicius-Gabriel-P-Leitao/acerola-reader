package br.acerola.manga.domain.service.library.manga

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import br.acerola.manga.domain.data.dao.database.FakeChapterFileDao
import br.acerola.manga.domain.data.dao.database.FakeMangaFolderDao
import br.acerola.manga.domain.model.archive.MangaFolder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FolderMangaOperationTest {

    private lateinit var context: Context
    private lateinit var fakeFolderDao: FakeMangaFolderDao
    private lateinit var fakeChapterDao: FakeChapterFileDao
    private lateinit var service: FolderMangaOperation

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        fakeFolderDao = FakeMangaFolderDao()
        fakeChapterDao = FakeChapterFileDao()
        service = FolderMangaOperation(context, fakeFolderDao, fakeChapterDao)

        mockkStatic(DocumentFile::class)
        mockkStatic(Uri::class)
        mockkStatic("androidx.core.net.UriKt")

        every { Uri.parse(any()) } answers {
            val arg = firstArg<String>()
            val mockUri = mockk<Uri>()
            every { mockUri.toString() } returns arg
            mockUri
        }
        every { any<String>().toUri() } answers {
            val arg = firstArg<String>()
            val mockUri = mockk<Uri>()
            every { mockUri.toString() } returns arg
            mockUri
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(DocumentFile::class)
        unmockkStatic(Uri::class)
        unmockkStatic("androidx.core.net.UriKt")
    }

    @Test
    fun rescanChaptersByManga_success_updatesChapters() = runBlocking {
        // Arrange
        val mangaId = 1L
        val mangaFolder = MangaFolder(
            id = mangaId,
            name = "Manga1",
            path = "content://tree/manga1",
            cover = null,
            banner = null,
            lastModified = 1000,
            chapterTemplate = "{value}.cbz"
        )
        fakeFolderDao.folders.add(mangaFolder)

        val mockRoot = mockk<DocumentFile>(relaxed = true)
        val mockFile1 = mockk<DocumentFile>(relaxed = true)

        every { DocumentFile.fromTreeUri(any(), any()) } returns mockRoot
        every { mockRoot.lastModified() } returns 2000 // Newer
        every { mockRoot.listFiles() } returns arrayOf(mockFile1)

        every { mockFile1.isFile } returns true
        every { mockFile1.name } returns "1.0.cbz"
        val mockUri1 = mockk<Uri>()
        every { mockUri1.toString() } returns "content://tree/manga1/1.0.cbz"
        every { mockFile1.uri } returns mockUri1

        service.rescanChaptersByManga(mangaId)

        assertEquals(1, fakeChapterDao.chapters.size)
        assertEquals("1.0.cbz", fakeChapterDao.chapters[0].chapter)
    }

    @Test
    fun loadMangas_returnsDtoList() = runBlocking {
        val mangaFolder = MangaFolder(
            id = 1L,
            name = "Manga1",
            path = "path",
            cover = null,
            banner = null,
            lastModified = 0,
            chapterTemplate = null
        )

        fakeFolderDao.folders.add(mangaFolder)
        val result = service.loadMangas().first { it.isNotEmpty() }

        assertEquals(1, result.size)
        assertEquals("Manga1", result[0].name)
    }
}
