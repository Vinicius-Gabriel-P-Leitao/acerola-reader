package br.acerola.comic.local.database.dao.archive

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import br.acerola.comic.fixtures.MangaDirectoryFixtures
import br.acerola.comic.local.database.AcerolaDatabase
import br.acerola.comic.local.entity.archive.ChapterArchive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@SmallTest
class ChapterArchiveDaoTest {
    private lateinit var db: AcerolaDatabase
    private lateinit var dao: br.acerola.comic.local.dao.archive.ChapterArchiveDao
    private lateinit var directoryDao: br.acerola.comic.local.dao.archive.ComicDirectoryDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room
                .inMemoryDatabaseBuilder(context, AcerolaDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = db.chapterArchiveDao()
        directoryDao = db.comicDirectoryDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun getChaptersByDirectory_Id_should_order_chapters_numerically_and_decimals() =
        runBlocking {
            // Preparação: Cria diretório e capítulos com ordenação mista
            val folderId = directoryDao.insert(MangaDirectoryFixtures.createMangaDirectory(id = 0L))
            val chapters =
                listOf(
                    ChapterArchive(chapter = "10", path = "p1", chapterSort = "10", folderPathFk = folderId),
                    ChapterArchive(chapter = "1", path = "p2", chapterSort = "1", folderPathFk = folderId),
                    ChapterArchive(chapter = "1.5", path = "p3", chapterSort = "1.5", folderPathFk = folderId),
                    ChapterArchive(chapter = "2", path = "p4", chapterSort = "2", folderPathFk = folderId),
                )
            dao.insertAll(*chapters.toTypedArray())

            // Ação
            val result = dao.getChaptersByDirectoryId(folderId).first()

            // Verificação: Ordem esperada: 1, 1.5, 2, 10
            assertEquals("1", result[0].chapter.chapterSort)
            assertEquals("1.5", result[1].chapter.chapterSort)
            assertEquals("2", result[2].chapter.chapterSort)
            assertEquals("10", result[3].chapter.chapterSort)
        }

    @Test
    fun getChaptersByDirectoryPaged_should_return_only_the_page_size() =
        runBlocking {
            // Preparação
            val folderId = directoryDao.insert(MangaDirectoryFixtures.createMangaDirectory(id = 0L))
            val chapters =
                List(10) {
                    ChapterArchive(chapter = it.toString(), path = "p$it", chapterSort = it.toString(), folderPathFk = folderId)
                }
            dao.insertAll(*chapters.toTypedArray())

            // Ação: Página 1 (offset 5), tamanho 5
            val result = dao.getChaptersByDirectoryPaged(folderId, pageSize = 5, offset = 5)

            // Verificação
            assertEquals(5, result.size)
        }

    @Test
    fun getChaptersByDirectoryPagedDesc_should_return_items_in_reverse_order() =
        runBlocking {
            // Preparação
            val folderId = directoryDao.insert(MangaDirectoryFixtures.createMangaDirectory(id = 0L))
            val chapters =
                listOf(
                    ChapterArchive(chapter = "1", path = "p1", chapterSort = "1", folderPathFk = folderId),
                    ChapterArchive(chapter = "2", path = "p2", chapterSort = "2", folderPathFk = folderId),
                    ChapterArchive(chapter = "3", path = "p3", chapterSort = "3", folderPathFk = folderId),
                )
            dao.insertAll(*chapters.toTypedArray())

            // Ação: Pega o primeiro item na ordem DESC
            val result = dao.getChaptersByDirectoryPagedDesc(folderId, pageSize = 1, offset = 0)

            // Verificação: Deve ser o capítulo "3"
            assertEquals(1, result.size)
            assertEquals("3", result[0].chapter.chapterSort)
        }

    @Test
    fun deleteChaptersByMangaDirectoryId_should_remove_only_chapters_of_that_comic() =
        runBlocking {
            // Preparação
            val id1 = directoryDao.insert(MangaDirectoryFixtures.createMangaDirectory(id = 0L, name = "Comic 1"))
            val id2 = directoryDao.insert(MangaDirectoryFixtures.createMangaDirectory(id = 0L, name = "Comic 2"))

            dao.insert(ChapterArchive(chapter = "1", path = "p1", chapterSort = "1", folderPathFk = id1))
            dao.insert(ChapterArchive(chapter = "1", path = "p2", chapterSort = "1", folderPathFk = id2))

            // Ação
            dao.deleteByDirectoryId(id1)

            // Verificação
            assertEquals(0, dao.countByDirectoryId(id1))
            assertEquals(1, dao.countByDirectoryId(id2))
        }
}
