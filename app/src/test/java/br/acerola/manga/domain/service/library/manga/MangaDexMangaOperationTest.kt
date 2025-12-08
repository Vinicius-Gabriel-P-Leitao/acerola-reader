package br.acerola.manga.domain.service.library.manga

import br.acerola.manga.domain.data.dao.database.FakeMangaFolderDao
import br.acerola.manga.domain.data.dao.database.FakeMangaMetadataDao
import br.acerola.manga.domain.model.metadata.MangaMetadata
import br.acerola.manga.domain.model.relation.MetadataWithRelations
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class MangaDexMangaOperationTest {

    @Test
    fun loadMangas_returnsStateFlowWithMangas() = runBlocking {
        val mangaMetadata = MangaMetadata(
            id = 1,
            name = "One Piece",
            description = "Pirates",
            status = "ongoing",
            romanji = "Wan Pisu",
            publication = 1997,
            mirrorId = "1",
            mangaCoverFk = null,
            mangaAuthorFk = null,
            mangaGenderFk = null
        )

        val relations = MetadataWithRelations(
            metadata = mangaMetadata,
            cover = null,
            author = null,
            gender = null
        )

        val fakeMangaDao = FakeMangaMetadataDao()
        fakeMangaDao.relationsList.add(relations)

        val fakeFolderDao = FakeMangaFolderDao()
        val service = MangaDexMangaOperation(fakeMangaDao, fakeFolderDao)

        val stateFlow = service.loadMangas()
        val collected = stateFlow.first { it.isNotEmpty() }

        assertEquals(1, collected.size)
        assertEquals("One Piece", collected[0].title)
    }
}
