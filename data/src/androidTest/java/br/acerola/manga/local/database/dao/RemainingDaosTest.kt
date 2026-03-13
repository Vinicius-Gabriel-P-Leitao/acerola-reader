package br.acerola.manga.local.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import br.acerola.manga.fixtures.MangaDirectoryFixtures
import br.acerola.manga.fixtures.MetadataFixtures
import br.acerola.manga.local.database.database.DatabaseAcerola
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

    private lateinit var db: DatabaseAcerola

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DatabaseAcerola::class.java).allowMainThreadQueries().build()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun testMangaDirectoryDao() = runBlocking {
        val dao = db.mangaDirectoryDao()
        val manga = MangaDirectoryFixtures.createMangaDirectory(name = "Test Manga")

        dao.insert(manga)
        val result = dao.getMangaDirectoryByName("Test Manga")

        assertNotNull(result)
        assertEquals("Test Manga", result?.name)
    }

    @Test
    fun testChapterRemoteInfoDao() = runBlocking {
        val mangaDao = db.mangaMangaRemoteInfoDao()
        val chapterDao = db.chapterRemoteInfoDao()

        val mangaId = mangaDao.insert(MetadataFixtures.createMangaRemoteInfo())
        val chapter = MetadataFixtures.createChapterRemoteInfo(mangaRemoteInfoFk = mangaId, chapter = "5")

        chapterDao.insert(chapter)
        val result = chapterDao.getChaptersByMangaRemoteInfo(mangaId).first()

        assertEquals(1, result.size)
        assertEquals("5", result[0].chapter)
    }

    @Test
    fun testChapterDownloadSourceDao() = runBlocking {
        val mangaDao = db.mangaMangaRemoteInfoDao()
        val chapterDao = db.chapterRemoteInfoDao()
        val sourceDao = db.chapterDownloadSourceDao()

        val mangaId = mangaDao.insert(MetadataFixtures.createMangaRemoteInfo())
        val chapterId = chapterDao.insert(MetadataFixtures.createChapterRemoteInfo(mangaRemoteInfoFk = mangaId))
        val source = MetadataFixtures.createChapterDownloadSource(chapterFk = chapterId, pageNumber = 1)

        sourceDao.insert(source)
        val result = sourceDao.getChapterDownloadSourceByRemoteInfoId(listOf(chapterId)).first()

        assertEquals(1, result.size)
        assertEquals(1, result[0].pageNumber)
    }

    @Test
    fun testCoverDao() = runBlocking {
        val mangaDao = db.mangaMangaRemoteInfoDao()
        val coverDao = db.coverDao()

        val mangaId = mangaDao.insert(MetadataFixtures.createMangaRemoteInfo())
        val cover = MetadataFixtures.createCover(mangaId = mangaId, mirrorId = "cov-99")

        coverDao.insert(cover)
        val result = coverDao.getCoverByMirrorId("cov-99")

        assertNotNull(result)
        assertEquals("cov-99", result?.mirrorId)
    }

    @Test
    fun testGenreDao() = runBlocking {
        val mangaDao = db.mangaMangaRemoteInfoDao()
        val genreDao = db.genreDao()

        val mangaId = mangaDao.insert(MetadataFixtures.createMangaRemoteInfo())
        val genre = MetadataFixtures.createGenre(mangaId = mangaId, mirrorId = "gen-99")

        genreDao.insert(genre)
        val result = genreDao.getIdByMirrorId("gen-99")

        assertNotNull(result)
    }
}