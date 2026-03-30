package br.acerola.manga.common.viewmodel.archive

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import br.acerola.manga.MainDispatcherRule
import br.acerola.manga.config.permission.FileSystemAccessManager
import br.acerola.manga.logging.AcerolaLogger
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FileSystemAccessViewModelTest {

    @get:Rule
    val coroutineRule = MainDispatcherRule()

    private val manager = mockk<FileSystemAccessManager>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private lateinit var viewModel: FileSystemAccessViewModel

    @Before
    fun setup() {
        mockkObject(AcerolaLogger)
        mockkStatic(DocumentFile::class)
        every { AcerolaLogger.d(any(), any(), any()) } returns Unit
        every { AcerolaLogger.audit(any(), any(), any(), any()) } returns Unit
        every { DocumentFile.fromTreeUri(any(), any()) } returns null

        viewModel = FileSystemAccessViewModel(manager, context)
    }

    @After
    fun tearDown() {
        unmockkObject(AcerolaLogger)
        unmockkStatic(DocumentFile::class)
    }

    @Test
    fun `deve chamar saveFolderUri no manager ao salvar uri`() = runTest {
        val uri = mockk<Uri>()
        viewModel.saveFolderUri(uri)
        
        coVerify { manager.saveFolderUri(uri) }
    }

    @Test
    fun `deve chamar loadFolderUri no manager ao carregar pasta salva`() = runTest {
        viewModel.loadSavedFolder()
        
        coVerify { manager.loadFolderUri() }
    }
}
