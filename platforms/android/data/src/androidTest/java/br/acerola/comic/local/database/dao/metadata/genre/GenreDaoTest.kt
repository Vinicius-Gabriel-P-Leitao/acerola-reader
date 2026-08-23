package br.acerola.comic.local.database.dao.metadata.genre

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import br.acerola.comic.fixtures.MetadataFixtures
import br.acerola.comic.local.database.AcerolaDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class GenreDaoTest {
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
