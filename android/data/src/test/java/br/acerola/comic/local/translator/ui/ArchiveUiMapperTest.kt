package br.acerola.comic.local.translator.ui

import br.acerola.comic.config.preference.types.VolumeViewType
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.archive.ChapterPageDto
import br.acerola.comic.dto.metadata.chapter.ChapterFeedDto
import br.acerola.comic.dto.metadata.chapter.ChapterRemoteInfoPageDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ArchiveUiMapperTest {

    @Test
    fun `toCombinedRegularDto deve garantir mapeamento 1 para 1 entre capitulos locais e remotos`() {
        // Cenário: 2 capítulos locais (1 e 1.5)
        // Metadados: Apenas para o capítulo 1
        val localChapters = listOf(
            ChapterFileDto(id = 1, name = "Ch 1", path = "", chapterSort = "1"),
            ChapterFileDto(id = 2, name = "Ch 1.5", path = "", chapterSort = "1.5")
        )
        val pageDto = ChapterPageDto(items = localChapters, pageSize = 20, page = 0, total = 2)

        val remoteMetadata = listOf(
            ChapterFeedDto(id = 100, title = "Metadata 1", chapter = "1", pageCount = 10, scanlation = "", source = emptyList())
        )
        val remotePage = ChapterRemoteInfoPageDto(items = remoteMetadata, pageSize = 20, page = 0, total = 1)

        val result = pageDto.toCombinedRegularDto(
            remoteAll = remotePage,
            page = 0,
            pageSize = 20,
            hasVolumeStructure = false,
            effectiveViewMode = VolumeViewType.CHAPTER
        )

        // Validação
        assertEquals(2, result.archive.items.size)
        assertEquals(2, result.remoteInfo?.items?.size)
        
        // O primeiro deve ter o metadado real
        assertEquals(100L, result.remoteInfo!!.items[0].id)
        assertEquals("Metadata 1", result.remoteInfo!!.items[0].title)
        
        // O segundo deve ter um metadado "dummy" (id -1)
        assertEquals(-1L, result.remoteInfo!!.items[1].id)
        assertEquals("1.5", result.remoteInfo!!.items[1].chapter)
    }

    @Test
    fun `toCombinedRegularDto deve lidar com normalizacao de strings no mapeamento`() {
        // Cenário: Local usa "00001.0000", Remoto usa "1"
        val localChapters = listOf(
            ChapterFileDto(id = 1, name = "Ch 1", path = "", chapterSort = "00001.0000")
        )
        val pageDto = ChapterPageDto(items = localChapters, pageSize = 20, page = 0, total = 1)

        val remoteMetadata = listOf(
            ChapterFeedDto(id = 100, title = "Metadata 1", chapter = "1", pageCount = 10, scanlation = "", source = emptyList())
        )
        val remotePage = ChapterRemoteInfoPageDto(items = remoteMetadata, pageSize = 20, page = 0, total = 1)

        val result = pageDto.toCombinedRegularDto(
            remoteAll = remotePage,
            page = 0,
            pageSize = 20,
            hasVolumeStructure = false,
            effectiveViewMode = VolumeViewType.CHAPTER
        )

        // Validação: deve ter encontrado mesmo com formatos diferentes
        assertEquals(1, result.remoteInfo?.items?.size)
        assertEquals(100L, result.remoteInfo!!.items[0].id)
    }
}
