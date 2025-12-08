package br.acerola.manga.domain.data.dao.database

import br.acerola.manga.domain.data.dao.database.metadata.MangaMetadataDao
import br.acerola.manga.domain.model.metadata.MangaMetadata
import br.acerola.manga.domain.model.relation.MetadataWithRelations
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeMangaMetadataDao : MangaMetadataDao {
    val metadataList = mutableListOf<MangaMetadata>()
    val relationsList = mutableListOf<MetadataWithRelations>()

    override fun getAllMangasMetadata(): Flow<List<MangaMetadata>> = flowOf(metadataList)

    override fun getAllMangasWithRelations(): Flow<List<MetadataWithRelations>> = flowOf(relationsList)

    override fun getMangaMetadataByName(name: String): Flow<MangaMetadata?> {
        return flowOf(metadataList.find { it.name == name })
    }

    override suspend fun insert(entity: MangaMetadata): Long {
        metadataList.add(entity)
        return entity.id
    }

    override suspend fun insertAll(vararg entity: MangaMetadata) {
        metadataList.addAll(entity)
    }

    override suspend fun update(entity: MangaMetadata) {
         val index = metadataList.indexOfFirst { it.id == entity.id }
        if (index != -1) metadataList[index] = entity
    }

    override suspend fun delete(entity: MangaMetadata) {
        metadataList.remove(entity)
    }
}
