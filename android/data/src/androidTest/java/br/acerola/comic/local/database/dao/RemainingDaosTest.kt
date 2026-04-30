package br.acerola.comic.local.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import br.acerola.comic.fixtures.MangaDirectoryFixtures
import br.acerola.comic.fixtures.MetadataFixtures
import br.acerola.comic.local.database.AcerolaDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@SmallTest
class RemainingDaosTest {
    private lateinit var db: AcerolaDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AcerolaDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun testMangaDirectoryDao() =
        runBlocking {
            val dao = db.comicDirectoryDao()
            val comic = MangaDirectoryFixtures.createMangaDirectory(name = "Test Comic")

            dao.insert(comic)
            val result = dao.getAllDirectories().first().find { it.name == "Test Comic" }

            assertNotNull(result)
            assertEquals("Test Comic", result?.name)
        }

    @Test
    fun testChapterMetadataDao() =
        runBlocking {
            val comicDao = db.comicRemoteInfoDao()
            val chapterDao = db.chapterRemoteInfoDao()

            val comicId = comicDao.insert(MetadataFixtures.createMangaRemoteInfo())
            val chapter = MetadataFixtures.createChapterRemoteInfo(comicRemoteInfoFk = comicId, chapter = "5")

            chapterDao.insert(chapter)
            val result = chapterDao.observeChaptersByMetadataId(comicId).first()

            assertEquals(1, result.size)
            assertEquals("5", result[0].chapter)
        }

    @Test
    fun testChapterDownloadSourceDao() =
        runBlocking {
            val comicDao = db.comicRemoteInfoDao()
            val chapterDao = db.chapterRemoteInfoDao()
            val sourceDao = db.chapterDownloadSourceDao()

            val comicId = comicDao.insert(MetadataFixtures.createMangaRemoteInfo())
            val chapterId = chapterDao.insert(MetadataFixtures.createChapterRemoteInfo(comicRemoteInfoFk = comicId))
            val source = MetadataFixtures.createChapterDownloadSource(chapterFk = chapterId, pageNumber = 1)

            sourceDao.insert(source)
            val result = sourceDao.observeChapterDownloadSourcesByChapterIds(listOf(chapterId)).first()

            assertEquals(1, result.size)
            assertEquals(1, result[0].pageNumber)
        }

    @Test
    fun testCoverDao() =
        runBlocking {
            val comicDao = db.comicRemoteInfoDao()
            val coverDao = db.coverDao()

            val comicId = comicDao.insert(MetadataFixtures.createMangaRemoteInfo())
            val cover = MetadataFixtures.createCover(comicId = comicId, fileName = "cover.jpg")

            coverDao.insert(cover)
            val result = coverDao.getByFileNameAndMetadataId(fileName = "cover.jpg", comicRemoteInfoFk = comicId)

            assertNotNull(result)
            assertEquals("cover.jpg", result?.fileName)
        }

    @Test
    fun testGenreDao() =
        runBlocking {
            val comicDao = db.comicRemoteInfoDao()
            val genreDao = db.genreDao()

            val comicId = comicDao.insert(MetadataFixtures.createMangaRemoteInfo())
            val genre = MetadataFixtures.createGenre(comicId = comicId, genre = "Shonen")

            genreDao.insert(genre)
            val result = genreDao.getIdByNameAndMetadataId(genre = "Shonen", comicRemoteInfoFk = comicId)

            assertNotNull(result)
        }
}
