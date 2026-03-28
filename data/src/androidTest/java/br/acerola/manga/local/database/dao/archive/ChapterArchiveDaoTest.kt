package br.acerola.manga.local.database.dao.archive

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import br.acerola.manga.fixtures.MangaDirectoryFixtures
import br.acerola.manga.local.database.AcerolaDatabase
import br.acerola.manga.local.entity.archive.ChapterArchive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@SmallTest
class ChapterArchiveDaoTest {

    private lateinit var db: AcerolaDatabase
    private lateinit var dao: br.acerola.manga.local.dao.archive.ChapterArchiveDao
    private lateinit var directoryDao: br.acerola.manga.local.dao.archive.MangaDirectoryDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AcerolaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.chapterArchiveDao()
        directoryDao = db.mangaDirectoryDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun getChaptersByMangaDirectory_deve_ordenar_capitulos_numericamente_e_decimais() = runBlocking {
        // Arrange: Cria diretório e capítulos com ordenação mista
        val folderId = directoryDao.insert(MangaDirectoryFixtures.createMangaDirectory())
        val chapters = listOf(
            ChapterArchive(chapter = "10", path = "p1", chapterSort = "10", folderPathFk = folderId),
            ChapterArchive(chapter = "1", path = "p2", chapterSort = "1", folderPathFk = folderId),
            ChapterArchive(chapter = "1.5", path = "p3", chapterSort = "1.5", folderPathFk = folderId),
            ChapterArchive(chapter = "2", path = "p4", chapterSort = "2", folderPathFk = folderId)
        )
        dao.insertAll(*chapters.toTypedArray())

        // Act
        val result = dao.getChaptersByMangaDirectory(folderId).first()

        // Assert: Ordem esperada: 1, 1.5, 2, 10
        assertEquals("1", result[0].chapterSort)
        assertEquals("1.5", result[1].chapterSort)
        assertEquals("2", result[2].chapterSort)
        assertEquals("10", result[3].chapterSort)
    }

    @Test
    fun getChaptersPaged_deve_retornar_apenas_o_tamanho_da_pagina() = runBlocking {
        // Arrange
        val folderId = directoryDao.insert(MangaDirectoryFixtures.createMangaDirectory())
        val chapters = List(10) { 
            ChapterArchive(chapter = it.toString(), path = "p$it", chapterSort = it.toString(), folderPathFk = folderId) 
        }
        dao.insertAll(*chapters.toTypedArray())

        // Act: Página 1 (offset 5), tamanho 5
        val result = dao.getChaptersPaged(folderId, pageSize = 5, offset = 5)

        // Assert
        assertEquals(5, result.size)
    }

    @Test
    fun deleteChaptersByMangaDirectoryId_deve_remover_apenas_capitulos_daquele_manga() = runBlocking {
        // Arrange
        val id1 = directoryDao.insert(MangaDirectoryFixtures.createMangaDirectory(name = "Manga 1"))
        val id2 = directoryDao.insert(MangaDirectoryFixtures.createMangaDirectory(name = "Manga 2"))
        
        dao.insert(ChapterArchive(chapter = "1", path = "p1", chapterSort = "1", folderPathFk = id1))
        dao.insert(ChapterArchive(chapter = "1", path = "p2", chapterSort = "1", folderPathFk = id2))

        // Act
        dao.deleteChaptersByMangaDirectoryId(id1)

        // Assert
        assertEquals(0, dao.countChaptersByMangaDirectory(id1))
        assertEquals(1, dao.countChaptersByMangaDirectory(id2))
    }
}
