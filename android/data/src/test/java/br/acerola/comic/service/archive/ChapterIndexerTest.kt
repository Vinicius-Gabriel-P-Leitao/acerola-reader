package br.acerola.comic.service.archive

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import br.acerola.comic.util.file.FastFileMetadata
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ChapterIndexerTest {
    private lateinit var indexer: ChapterIndexer
    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        indexer = ChapterIndexer(context)
        mockkStatic(DocumentFile::class)
    }

    @Test
    fun `buildEntity deve criar ChapterArchive corretamente a partir do FastFileMetadata`() =
        runTest {
            // Checksum computation reads via SAF (`DocumentFile.fromSingleUri`), unavailable in a
            // plain unit test — nulling it out here isolates the field-mapping behavior under test.
            every { DocumentFile.fromSingleUri(context, any()) } returns null

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
            val volumeIdFk = 5L
            val isSpecial = false

            val result =
                indexer.buildEntity(
                    file = file,
                    comicId = comicId,
                    fileUri = fileUri,
                    chapterSort = chapterSort,
                    volumeIdFk = volumeIdFk,
                    isSpecial = isSpecial,
                )

            assertEquals(comicId, result.folderPathFk)
            assertEquals(fileUri, result.path)
            assertEquals(chapterSort, result.chapterSort)
            assertEquals(volumeIdFk, result.volumeIdFk)
            assertEquals(isSpecial, result.isSpecial)
            assertEquals("cap01.cbz", result.chapter)
            assertNull(result.checksum)
        }
}
