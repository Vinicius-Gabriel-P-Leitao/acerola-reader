package br.acerola.comic.local.database.dao.metadata.chapter

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
class ChapterMetadataDaoTest {
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
}
