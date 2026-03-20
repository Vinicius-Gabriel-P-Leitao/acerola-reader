package br.acerola.manga.local.database.dao.metadata.author

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import br.acerola.manga.fixtures.MetadataFixtures
import br.acerola.manga.local.database.dao.metadata.MangaRemoteInfoDao
import br.acerola.manga.local.database.dao.metadata.relationship.AuthorDao
import br.acerola.manga.local.database.database.DatabaseAcerola
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

    private lateinit var db: DatabaseAcerola
    private lateinit var authorDao: AuthorDao
    private lateinit var mangaDao: MangaRemoteInfoDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DatabaseAcerola::class.java).allowMainThreadQueries().build()
        authorDao = db.authorDao()
        mangaDao = db.mangaMangaRemoteInfoDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertOrGetId_com_unique_constraint_retorna_mesmo_id() = runBlocking {
        // Arrange
        val manga = MetadataFixtures.createMangaRemoteInfo()
        val mangaId = mangaDao.insert(manga)
        val author = MetadataFixtures.createAuthor(mangaId = mangaId, name = "Kishimoto")

        // Act — inserting same author twice should return the same ID
        val id1 = authorDao.insertOrGetId(author)
        val id2 = authorDao.insertOrGetId(author)

        // Assert — with UNIQUE(name, manga_remote_info_fk), same author returns same ID
        assertEquals(id1, id2)
    }
}
