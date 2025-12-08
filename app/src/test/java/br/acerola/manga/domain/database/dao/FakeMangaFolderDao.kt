package br.acerola.manga.domain.database.dao

import br.acerola.manga.domain.database.dao.database.archive.MangaFolderDao
import br.acerola.manga.domain.model.archive.MangaFolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.Collections

class FakeMangaFolderDao : MangaFolderDao {
    val folders: MutableList<MangaFolder> = Collections.synchronizedList(mutableListOf<MangaFolder>())

    override fun getAllMangasFolders(): Flow<List<MangaFolder>> = flowOf(folders.toList())

    override suspend fun getMangaFolderById(mangaId: Long): MangaFolder? {
        return folders.find { it.id == mangaId }
    }

    override suspend fun insert(entity: MangaFolder): Long {
        folders.add(entity)
        return entity.id
    }

    override suspend fun insertAll(vararg entity: MangaFolder) {
        folders.addAll(entity)
    }

    override suspend fun update(entity: MangaFolder) {
        val index = folders.indexOfFirst { it.id == entity.id }
        if (index != -1) folders[index] = entity
    }

    override suspend fun delete(entity: MangaFolder) {
        folders.remove(entity)
    }
}
