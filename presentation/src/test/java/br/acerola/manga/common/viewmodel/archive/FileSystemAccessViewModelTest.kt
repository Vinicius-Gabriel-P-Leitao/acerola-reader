package br.acerola.manga.common.viewmodel.archive

import android.net.Uri
import br.acerola.manga.MainDispatcherRule
import br.acerola.manga.config.permission.FileSystemAccessManager
import br.acerola.manga.logging.AcerolaLogger
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
    private lateinit var viewModel: FileSystemAccessViewModel

    @Before
    fun setup() {
        mockkObject(AcerolaLogger)
        every { AcerolaLogger.d(any(), any(), any()) } returns Unit
        every { AcerolaLogger.audit(any(), any(), any(), any()) } returns Unit

        viewModel = FileSystemAccessViewModel(manager)
    }

    @After
    fun tearDown() {
        unmockkObject(AcerolaLogger)
    }

    @Test
    fun `deve chamar saveFolderUri no manager ao salvar uri`() = runTest {
        val uri = mockk<Uri>()
        viewModel.saveFolderUri(uri)
        
        io.mockk.coVerify { manager.saveFolderUri(uri) }
    }

    @Test
    fun `deve chamar loadFolderUri no manager ao carregar pasta salva`() = runTest {
        viewModel.loadSavedFolder()
        
        io.mockk.coVerify { manager.loadFolderUri() }
    }
}
