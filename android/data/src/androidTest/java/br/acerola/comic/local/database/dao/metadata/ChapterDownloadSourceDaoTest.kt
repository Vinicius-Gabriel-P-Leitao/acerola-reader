package br.acerola.comic.local.database.dao.metadata

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import br.acerola.comic.fixtures.MetadataFixtures
import br.acerola.comic.local.database.AcerolaDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ChapterDownloadSourceDaoTest {
    private lateinit var db: AcerolaDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AcerolaDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
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
}
