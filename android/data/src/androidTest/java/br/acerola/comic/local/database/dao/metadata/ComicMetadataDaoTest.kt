package br.acerola.comic.local.database.dao.metadata

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import br.acerola.comic.fixtures.MetadataFixtures
import br.acerola.comic.local.dao.metadata.ComicMetadataDao
import br.acerola.comic.local.dao.metadata.relationship.AuthorDao
import br.acerola.comic.local.dao.metadata.relationship.CoverDao
import br.acerola.comic.local.dao.metadata.relationship.GenreDao
import br.acerola.comic.local.database.AcerolaDatabase
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
class ComicMetadataDaoTest {
    private lateinit var db: AcerolaDatabase

    private lateinit var comicDao: ComicMetadataDao
    private lateinit var authorDao: AuthorDao
    private lateinit var genreDao: GenreDao
    private lateinit var coverDao: CoverDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AcerolaDatabase::class.java).allowMainThreadQueries().build()
        comicDao = db.comicRemoteInfoDao()
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
    fun getAllComicsWithRelations_DeveRetornarMangaComAutoresGenerosECapa() =
        runBlocking {
            // Arrange
            val comic = MetadataFixtures.createMangaRemoteInfo(title = "Comic Test")
            val comicId = comicDao.insert(comic)

            val author = MetadataFixtures.createAuthor(comicId = comicId, name = "Author 1")
            val genre = MetadataFixtures.createGenre(comicId = comicId, genre = "Action")
            val cover = MetadataFixtures.createCover(comicId = comicId, url = "url")

            authorDao.insert(author)
            genreDao.insert(genre)
            coverDao.insert(cover)

            // Act
            val result = comicDao.observeAllComicsWithRelations().first()

            // Assert
            assertTrue(result.isNotEmpty())
            val relations = result[0]

            assertEquals("Comic Test", relations.remoteInfo.title)

            assertEquals(1, relations.author.size)
            assertEquals("Author 1", relations.author[0].name)

            assertEquals(1, relations.genre.size)
            assertEquals("Action", relations.genre[0].genre)

            assertEquals(1, relations.cover.size)
            assertEquals("url", relations.cover[0].url)
        }

    @Test
    fun deleteManga_DeveRemoverRelacoesEmCascata() =
        runBlocking {
            // Arrange
            val comic = MetadataFixtures.createMangaRemoteInfo()
            val comicId = comicDao.insert(comic)

            val author = MetadataFixtures.createAuthor(comicId = comicId)
            val genre = MetadataFixtures.createGenre(comicId = comicId)

            authorDao.insert(author)
            genreDao.insert(genre)

            // Act
            comicDao.delete(comic.copy(id = comicId))

            // Assert
            val result = comicDao.observeAllComicsWithRelations().first()
            assertTrue(result.isEmpty())
        }
}
