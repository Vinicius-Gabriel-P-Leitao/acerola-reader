package br.acerola.manga.local.database.dao.metadata

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import br.acerola.manga.fixtures.MetadataFixtures
import br.acerola.manga.local.database.database.DatabaseAcerola
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@SmallTest
class MangaRemoteInfoDaoTest {

    private lateinit var db: DatabaseAcerola
    private lateinit var mangaDao: MangaRemoteInfoDao
    private lateinit var authorDao: br.acerola.manga.local.database.dao.metadata.author.AuthorDao
    private lateinit var genreDao: br.acerola.manga.local.database.dao.metadata.genre.GenreDao
    private lateinit var coverDao: br.acerola.manga.local.database.dao.metadata.cover.CoverDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DatabaseAcerola::class.java)
            .allowMainThreadQueries()
            .build()
        mangaDao = db.mangaMangaRemoteInfoDao()
        authorDao = db.authorDao()
        genreDao = db.genreDao()
        coverDao = db.coverDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun getAllMangasWithRelations_DeveRetornarMangaComAutoresGenerosECapa() = runBlocking {
        // Arrange
        val manga = MetadataFixtures.createMangaRemoteInfo(mirrorId = "m1", title = "Manga Test")
        val mangaId = mangaDao.insert(manga)

        val author = MetadataFixtures.createAuthor(mangaId = mangaId, name = "Author 1", mirrorId = "a1")
        val genre = MetadataFixtures.createGenre(mangaId = mangaId, genre = "Action", mirrorId = "g1")
        val cover = MetadataFixtures.createCover(mangaId = mangaId, url = "url", mirrorId = "c1")

        authorDao.insert(author)
        genreDao.insert(genre)
        coverDao.insert(cover)

        // Act
        val result = mangaDao.getAllMangasWithRelations().first()

        // Assert
        assertTrue(result.isNotEmpty())
        val relations = result[0]

        assertEquals("Manga Test", relations.remoteInfo.title)

        assertEquals(1, relations.author.size)
        assertEquals("Author 1", relations.author[0].name)

        assertEquals(1, relations.genre.size)
        assertEquals("Action", relations.genre[0].genre)

        assertEquals(1, relations.cover.size)
        assertEquals("url", relations.cover[0].url)
    }

    @Test
    fun deleteManga_DeveRemoverRelacoesEmCascata() = runBlocking {
        // Arrange
        val manga = MetadataFixtures.createMangaRemoteInfo(mirrorId = "m1")
        val mangaId = mangaDao.insert(manga)

        val author = MetadataFixtures.createAuthor(mangaId = mangaId)
        val genre = MetadataFixtures.createGenre(mangaId = mangaId)

        authorDao.insert(author)
        genreDao.insert(genre)

        // Act
        mangaDao.delete(manga.copy(id = mangaId))

        // Assert
        val result = mangaDao.getAllMangasWithRelations().first()
        assertTrue(result.isEmpty())
    }
}