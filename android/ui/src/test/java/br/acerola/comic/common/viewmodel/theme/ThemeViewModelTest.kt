package br.acerola.comic.common.viewmodel.theme

import android.app.Application
import app.cash.turbine.test
import br.acerola.comic.MainDispatcherRule
import br.acerola.comic.config.preference.ThemePreference
import br.acerola.comic.config.preference.types.AppTheme
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
        every { ThemePreference.themeFlow(any()) } returns flowOf(AppTheme.CATPPUCCIN)
        coEvery { ThemePreference.saveTheme(any(), any()) } returns Unit

        viewModel = ThemeViewModel(application)
    }

    @After
    fun tearDown() {
        unmockkObject(ThemePreference)
    }

    @Test
    fun `deve emitir tema atual ao inicializar`() =
        runTest {
            viewModel.currentTheme.test {
                assertThat(awaitItem()).isEqualTo(AppTheme.CATPPUCCIN)
            }
        }

    @Test
    fun `deve chamar saveTheme ao alterar configuracao`() =
        runTest {
            viewModel.setTheme(AppTheme.NORD)
            io.mockk.coVerify { ThemePreference.saveTheme(any(), AppTheme.NORD) }
        }
}
