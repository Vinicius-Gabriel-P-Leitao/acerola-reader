package br.acerola.comic.service.archive

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import br.acerola.comic.local.dao.archive.VolumeArchiveDao
import br.acerola.comic.local.entity.archive.VolumeArchive
import br.acerola.comic.util.file.ContentQueryHelper
import br.acerola.comic.util.file.FastFileMetadata
import br.acerola.comic.util.sort.SortNormalizer
import br.acerola.comic.util.sort.SortResult
import br.acerola.comic.util.sort.SortType
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class VolumeSyncServiceTest {
    @MockK
    lateinit var volumeArchiveDao: VolumeArchiveDao

    @MockK
    lateinit var context: Context

    private lateinit var service: VolumeSyncService

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        service = VolumeSyncService(volumeArchiveDao, context)
        mockkObject(SortNormalizer)
        mockkObject(ContentQueryHelper)
        mockkStatic(Uri::class)
        mockkStatic(DocumentsContract::class)
        mockkStatic(DocumentFile::class)

        every { DocumentsContract.getDocumentId(any()) } returns "doc_id"
        every { DocumentsContract.buildDocumentUriUsingTree(any(), any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkObject(SortNormalizer)
        unmockkObject(ContentQueryHelper)
        unmockkStatic(Uri::class)
        unmockkStatic(DocumentsContract::class)
        unmockkStatic(DocumentFile::class)
    }

    @Test
    fun `sync deve inserir novos volumes e retornar mapa de caminhos para ids`() =
        runTest {
            val comicId = 1L
            val subFolder = FastFileMetadata("vol1_id", 0L, "Vol. 01", DocumentsContract.Document.MIME_TYPE_DIR, 1000L)
            val subFolders = listOf(subFolder)
            val folderUri = mockk<Uri>(relaxed = true)
            val mockUri = mockk<Uri>(relaxed = true)

            every { folderUri.toString() } returns "folder/uri"
            every { Uri.parse(any()) } returns mockUri

            every { SortNormalizer.normalize(any(), any(), any()) } returns SortResult(SortType.VOLUME, 1, 0, false, "1")
            coEvery { volumeArchiveDao.getVolumesListByDirectoryId(comicId) } returns emptyList()
            coEvery { volumeArchiveDao.insert(any()) } returns 101L

            every { ContentQueryHelper.listFiles(any(), any(), any()) } returns arrow.core.Either.Right(emptyList())
            val volDoc = mockk<DocumentFile>(relaxed = true)
            every { DocumentFile.fromSingleUri(any(), any()) } returns volDoc
            every { volDoc.listFiles() } returns emptyArray()

            val result =
                service.sync(
                    comicId = comicId,
                    subFolders = subFolders,
                    volumeTemplates = emptyList(),
                    baseUri = null,
                    folderUri = folderUri,
                )

            assertEquals(1, result.size)
            assertEquals(101L, result.values.first())
            coVerify { volumeArchiveDao.insert(any()) }
        }

    @Test
    fun `sync deve deletar volumes que nao existem mais no disco`() =
        runTest {
            val comicId = 1L
            val subFolders = emptyList<FastFileMetadata>()
            val folderUri = mockk<Uri>(relaxed = true)

            val staleVolume =
                VolumeArchive(
                    id = 50L,
                    name = "Stale",
                    path = "stale/path",
                    volumeSort = "99",
                    comicDirectoryFk = comicId,
                )
            coEvery { volumeArchiveDao.getVolumesListByDirectoryId(comicId) } returns listOf(staleVolume)
            coEvery { volumeArchiveDao.delete(any()) } returns Unit

            service.sync(
                comicId = comicId,
                subFolders = subFolders,
                volumeTemplates = emptyList(),
                baseUri = null,
                folderUri = folderUri,
            )

            coVerify { volumeArchiveDao.delete(staleVolume) }
        }
}
