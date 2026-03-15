package br.acerola.manga.common.viewmodel.archive

import android.app.Application
import app.cash.turbine.test
import br.acerola.manga.MainDispatcherRule
import br.acerola.manga.config.preference.FileExtension
import br.acerola.manga.config.preference.FilePreferences
import br.acerola.manga.logging.AcerolaLogger
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FilePreferencesViewModelTest {

    @get:Rule
    val coroutineRule = MainDispatcherRule()

    private val application = mockk<Application>(relaxed = true)
    private lateinit var viewModel: FilePreferencesViewModel

    @Before
    fun setup() {
        mockkObject(FilePreferences)
        mockkObject(AcerolaLogger)
        
        every { AcerolaLogger.d(any(), any(), any()) } returns Unit
        every { AcerolaLogger.audit(any(), any(), any(), any()) } returns Unit
        
        every { FilePreferences.fileExtensionFlow(any()) } returns flowOf(FileExtension.CBR)
        coEvery { FilePreferences.saveFileExtension(any(), any()) } returns Unit

        viewModel = FilePreferencesViewModel(application)
    }

    @After
    fun tearDown() {
        unmockkObject(FilePreferences)
        unmockkObject(AcerolaLogger)
    }

    @Test
    fun `deve emitir extensao salva ao inicializar`() = runTest {
        viewModel.selectedExtension.test {
            assertThat(awaitItem()).isEqualTo(FileExtension.CBR)
        }
    }

    @Test
    fun `deve chamar saveFileExtension ao salvar nova extensao`() = runTest {
        viewModel.saveExtension(FileExtension.CBZ)
        
        io.mockk.coVerify { FilePreferences.saveFileExtension(any(), FileExtension.CBZ) }
    }
}
