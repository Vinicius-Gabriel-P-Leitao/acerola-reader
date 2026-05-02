package br.acerola.comic.local.database.dao.metadata.author

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import br.acerola.comic.fixtures.MetadataFixtures
import br.acerola.comic.local.dao.metadata.ComicMetadataDao
import br.acerola.comic.local.dao.metadata.relationship.AuthorDao
import br.acerola.comic.local.database.AcerolaDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@SmallTest
class AuthorDaoTest {
    private lateinit var db: AcerolaDatabase
    private lateinit var authorDao: AuthorDao
    private lateinit var comicDao: ComicMetadataDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AcerolaDatabase::class.java).allowMainThreadQueries().build()
        authorDao = db.authorDao()
        comicDao = db.comicRemoteInfoDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsertAndGetId_com_unique_constraint_retorna_mesmo_id() =
        runBlocking {
            // Arrange
            val comic = MetadataFixtures.createMangaRemoteInfo()
            val comicId = comicDao.insert(comic)
            val author = MetadataFixtures.createAuthor(comicId = comicId, name = "Kishimoto")

            // Act — inserting same author twice should return the same ID
            val id1 = authorDao.upsertAndGetId(author)
            val id2 = authorDao.upsertAndGetId(author)

            // Assert — with UNIQUE(name, comic_remote_info_fk), same author returns same ID
            assertEquals(id1, id2)
        }
}
