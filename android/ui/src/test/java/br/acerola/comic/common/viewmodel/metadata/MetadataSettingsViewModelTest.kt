package br.acerola.comic.common.viewmodel.metadata

import android.content.Context
import app.cash.turbine.test
import br.acerola.comic.MainDispatcherRule
import br.acerola.comic.config.preference.MetadataPreference
import br.acerola.comic.logging.AcerolaLogger
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
class MetadataSettingsViewModelTest {
    @get:Rule
    val coroutineRule = MainDispatcherRule()

    private val context = mockk<Context>(relaxed = true)
    private lateinit var viewModel: MetadataSettingsViewModel

    @Before
    fun setup() {
        mockkObject(MetadataPreference)
        mockkObject(AcerolaLogger)

        every { AcerolaLogger.d(any(), any(), any()) } returns Unit
        every { AcerolaLogger.audit(any(), any(), any(), any()) } returns Unit

        every { MetadataPreference.generateComicInfoFlow(any()) } returns flowOf(true)
        coEvery { MetadataPreference.saveGenerateComicInfo(any(), any()) } returns Unit

        viewModel = MetadataSettingsViewModel(context)
    }

    @After
    fun tearDown() {
        unmockkObject(MetadataPreference)
        unmockkObject(AcerolaLogger)
    }

    @Test
    fun `deve emitir preferencia de comicinfo ao inicializar`() =
        runTest {
            viewModel.generateComicInfo.test {
                assertThat(awaitItem()).isTrue()
            }
        }

    @Test
    fun `deve chamar saveGenerateComicInfo ao alterar configuracao`() =
        runTest {
            viewModel.setGenerateComicInfo(false)
            io.mockk.coVerify { MetadataPreference.saveGenerateComicInfo(any(), false) }
        }
}
