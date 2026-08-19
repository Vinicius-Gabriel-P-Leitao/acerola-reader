package br.acerola.comic.local.translator.ui

import br.acerola.comic.config.preference.types.VolumeViewType
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.archive.ChapterPageDto
import org.junit.Assert.assertEquals
import org.junit.Test

class ArchiveUiMapperTest {
    @Test
    fun `toCombinedRegularDto should slice local chapters list by page`() {
        val localChapters =
            listOf(
                ChapterFileDto(id = 1, name = "Ch 1", path = "", chapterSort = "1"),
                ChapterFileDto(id = 2, name = "Ch 1.5", path = "", chapterSort = "1.5"),
            )
        val pageDto = ChapterPageDto(items = localChapters, pageSize = 20, page = 0, total = 2)

        val result =
            pageDto.toCombinedRegularDto(
                page = 0,
                pageSize = 20,
                hasVolumeStructure = false,
                effectiveViewMode = VolumeViewType.CHAPTER,
            )

        assertEquals(2, result.archive.items.size)
        assertEquals("Ch 1", result.archive.items[0].name)
        assertEquals("Ch 1.5", result.archive.items[1].name)
    }
}
