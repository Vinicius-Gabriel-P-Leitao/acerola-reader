package br.acerola.manga.common.viewmodel.theme

import android.app.Application
import app.cash.turbine.test
import br.acerola.manga.MainDispatcherRule
import br.acerola.manga.config.preference.ThemePreference
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
class ThemeViewModelTest {

    @get:Rule
    val coroutineRule = MainDispatcherRule()

    private val application = mockk<Application>(relaxed = true)
    private lateinit var viewModel: ThemeViewModel

    @Before
    fun setup() {
        mockkObject(ThemePreference)
        every { ThemePreference.dynamicColorFlow(any()) } returns flowOf(true)
        coEvery { ThemePreference.saveDynamicColor(any(), any()) } returns Unit

        viewModel = ThemeViewModel(application)
    }

    @After
    fun tearDown() {
        unmockkObject(ThemePreference)
    }

    @Test
    fun `deve emitir estado de cor dinamica ao inicializar`() = runTest {
        viewModel.useDynamicColor.test {
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `deve chamar saveDynamicColor ao alterar configuracao`() = runTest {
        viewModel.setDynamicColor(false)
        io.mockk.coVerify { ThemePreference.saveDynamicColor(any<android.content.Context>(), false) }
    }
}
