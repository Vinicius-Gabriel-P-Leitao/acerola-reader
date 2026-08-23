package br.acerola.comic.local.database.dao.archive

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import br.acerola.comic.fixtures.MangaDirectoryFixtures
import br.acerola.comic.local.database.AcerolaDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ComicDirectoryDaoTest {
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
    fun testMangaDirectoryDao() =
        runBlocking {
            val dao = db.comicDirectoryDao()
            val comic = MangaDirectoryFixtures.createMangaDirectory(name = "Test Comic")
            dao.insert(comic)
            val result = dao.getAllDirectories().first().find { it.name == "Test Comic" }
            assertNotNull(result)
            assertEquals("Test Comic", result?.name)
        }
}
