package br.acerola.manga.domain.database.dao

import br.acerola.manga.domain.database.dao.database.archive.ChapterFileDao
import br.acerola.manga.domain.model.archive.ChapterFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeChapterFileDao : ChapterFileDao {
    val chapters = mutableListOf<ChapterFile>()

    override suspend fun deleteChaptersByFolderId(folderId: Long) {
        chapters.removeAll { it.folderPathFk == folderId }
    }

    override fun getAllChapterFiles(): Flow<List<ChapterFile>> = flowOf(chapters)

    override fun getChaptersFileById(chapterId: Long): Flow<ChapterFile?> {
        return flowOf(chapters.find { it.id == chapterId })
    }

    override suspend fun countChaptersByFolder(folderId: Long): Int {
        return chapters.count { it.folderPathFk == folderId }
    }

    override fun getChaptersByFolder(folderId: Long): Flow<List<ChapterFile>> {
        return flowOf(chapters.filter { it.folderPathFk == folderId }.sortedBy { it.chapterSort })
    }

    override fun getChaptersPaged(folderId: Long, pageSize: Int, offset: Int): Flow<List<ChapterFile>> {
        val filtered = chapters.filter { it.folderPathFk == folderId }
            .sortedBy { it.chapterSort }
        
        val end = (offset + pageSize).coerceAtMost(filtered.size)
        val result = if (offset < filtered.size) filtered.subList(offset, end) else emptyList()
        return flowOf(result)
    }

    override suspend fun insert(entity: ChapterFile): Long {
        chapters.add(entity)
        return entity.id
    }

    override suspend fun insertAll(vararg entity: ChapterFile) {
        chapters.addAll(entity)
    }

    override suspend fun update(entity: ChapterFile) {
        val index = chapters.indexOfFirst { it.id == entity.id }
        if (index != -1) chapters[index] = entity
    }

    override suspend fun delete(entity: ChapterFile) {
        chapters.remove(entity)
    }
}
