package br.acerola.comic.service.archive

import br.acerola.comic.util.file.FastFileMetadata
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ChapterIndexerTest {
    private lateinit var indexer: ChapterIndexer

    @Before
    fun setUp() {
        indexer = ChapterIndexer()
    }

    @Test
    fun `buildEntity deve criar ChapterArchive corretamente a partir do FastFileMetadata`() {
        val file =
            FastFileMetadata(
                id = "id1",
                name = "cap01.cbz",
                size = 100L,
                mimeType = "application/x-cbz",
                lastModified = 1000L,
            )
        val comicId = 10L
        val fileUri = "uri/to/file"
        val chapterSort = "1"
        val fastHash = "hash"
        val volumeIdFk = 5L
        val isSpecial = false

        // Mocking the extension function toChapterArchiveEntity if needed,
        // but it's better to test the real mapping if it's pure.
        // Let's check how it's implemented in ChapterIndexer.

        val result =
            indexer.buildEntity(
                file = file,
                comicId = comicId,
                fileUri = fileUri,
                chapterSort = chapterSort,
                fastHash = fastHash,
                volumeIdFk = volumeIdFk,
                isSpecial = isSpecial,
            )

        assertEquals(comicId, result.folderPathFk)
        assertEquals(fileUri, result.path)
        assertEquals(chapterSort, result.chapterSort)
        assertEquals(fastHash, result.fastHash)
        assertEquals(volumeIdFk, result.volumeIdFk)
        assertEquals(isSpecial, result.isSpecial)
        assertEquals("cap01.cbz", result.chapter)
    }
}
