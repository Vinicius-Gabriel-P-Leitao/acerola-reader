package br.acerola.manga.domain.service.library.chapter

import br.acerola.manga.domain.database.dao.FakeChapterFileDao
import br.acerola.manga.domain.model.archive.ChapterFile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FileChapterOperationTest {

    @Test
    fun loadChapterByManga_returnsStateFlowWithChapters() = runBlocking {
        val fakeDao = FakeChapterFileDao()
        fakeDao.chapters.add(
            ChapterFile(
                id = 1,
                chapter = "Ch. 1",
                path = "/path/1",
                chapterSort = "1",
                folderPathFk = 1
            )
        )
        fakeDao.chapters.add(
            ChapterFile(
                id = 2,
                chapter = "Ch. 2",
                path = "/path/2",
                chapterSort = "2",
                folderPathFk = 1
            )
        )

        val service = FileChapterOperation(fakeDao)
        val stateFlow = service.loadChapterByManga(1)
        val collected = stateFlow.first { it.total > 0 }

        assertEquals(2, collected.items.size)
        assertEquals("Ch. 1", collected.items[0].name)
    }

    @Test
    fun loadNextPage_returnsPagedChapters() = runBlocking {
        val fakeDao = FakeChapterFileDao()
        fakeDao.chapters.add(
            ChapterFile(
                id = 3,
                chapter = "Ch. 3",
                path = "/path/3",
                chapterSort = "3",
                folderPathFk = 1
            )
        )

        val service = FileChapterOperation(fakeDao)
        val result = service.loadNextPage(folderId = 1, total = 10, page = 0, pageSize = 20)

        assertEquals(1, result.items.size)
        assertEquals("Ch. 3", result.items[0].name)
        assertEquals(0, result.page)
    }
}
