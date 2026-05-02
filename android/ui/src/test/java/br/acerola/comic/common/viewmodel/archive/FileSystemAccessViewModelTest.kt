package br.acerola.comic.common.viewmodel.archive

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import app.cash.turbine.test
import arrow.core.Either
import br.acerola.comic.MainDispatcherRule
import br.acerola.comic.config.permission.FileSystemAccessManager
import br.acerola.comic.config.preference.ComicDirectoryPreference
import br.acerola.comic.logging.AcerolaLogger
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FileSystemAccessViewModelTest {
    @get:Rule
    val coroutineRule = MainDispatcherRule()

    private val manager = mockk<FileSystemAccessManager>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private lateinit var viewModel: FileSystemAccessViewModel

    @Before
    fun setup() {
        mockkObject(ComicDirectoryPreference)
        mockkObject(AcerolaLogger)
        mockkStatic(DocumentFile::class)

        every { AcerolaLogger.d(any(), any(), any()) } returns Unit
        every { AcerolaLogger.audit(any(), any(), any(), any()) } returns Unit

        every { ComicDirectoryPreference.tutorialShownFlow(any()) } returns flowOf(false)
        coEvery { ComicDirectoryPreference.setTutorialShown(any(), any()) } returns Unit
        coEvery { manager.loadFolderUri() } returns Unit
        every { manager.folderUri } returns null

        // Mock DocumentFile to avoid issues with fromTreeUri
        every { DocumentFile.fromTreeUri(any(), any()) } returns null

        viewModel = FileSystemAccessViewModel(manager, context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `deve salvar uri da pasta e marcar tutorial como visto`() =
        runTest {
            val uri = mockk<Uri>()
            coEvery { manager.saveFolderUri(uri) } returns Either.Right(Unit)

            viewModel.saveFolderUri(uri)

            viewModel.tutorialShown.test {
                assertThat(awaitItem()).isTrue()
            }

            io.mockk.coVerify { manager.saveFolderUri(uri) }
        }

    @Test
    fun `deve carregar pasta salva ao inicializar`() =
        runTest {
            val uri = mockk<Uri>()
            every { manager.folderUri } returns uri

            viewModel.loadSavedFolder()

            assertThat(viewModel.folderUri).isEqualTo(uri)
        }
}
