package br.acerola.manga.module.main.config

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import br.acerola.manga.common.ux.theme.AcerolaTheme
import br.acerola.manga.common.ux.theme.local.LocalSnackbarHostState
import br.acerola.manga.common.viewmodel.archive.FilePreferencesViewModel
import br.acerola.manga.common.viewmodel.archive.FileSystemAccessViewModel
import br.acerola.manga.common.viewmodel.library.archive.MangaDirectoryViewModel
import br.acerola.manga.common.viewmodel.library.metadata.MangaRemoteInfoViewModel
import br.acerola.manga.common.viewmodel.metadata.MetadataSettingsViewModel
import br.acerola.manga.common.viewmodel.theme.ThemeViewModel
import br.acerola.manga.config.preference.FileExtension
import br.acerola.manga.error.UserMessage
import br.acerola.manga.module.main.Main
import br.acerola.manga.module.main.config.Screen
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ConfigScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Mocks dos ViewModels necessários
    private val filePrefsVM = mockk<FilePreferencesViewModel>(relaxed = true)
    private val fsAccessVM = mockk<FileSystemAccessViewModel>(relaxed = true)
    private val mangaDirVM = mockk<MangaDirectoryViewModel>(relaxed = true)
    private val mangaDexVM = mockk<MangaRemoteInfoViewModel>(relaxed = true)
    private val metadataVM = mockk<MetadataSettingsViewModel>(relaxed = true)
    private val themeVM = mockk<ThemeViewModel>(relaxed = true)

    @Before
    fun setUp() {
        val emptyEvents = MutableSharedFlow<UserMessage>().asSharedFlow()
        
        every { themeVM.useDynamicColor } returns MutableStateFlow(false)
        every { themeVM.uiEvents } returns emptyEvents
        
        every { filePrefsVM.selectedExtension } returns MutableStateFlow(FileExtension.CBZ)
        every { filePrefsVM.uiEvents } returns emptyEvents
        
        every { metadataVM.generateComicInfo } returns MutableStateFlow(true)
        every { metadataVM.uiEvents } returns emptyEvents
        
        every { mangaDirVM.isIndexing } returns MutableStateFlow(false)
        every { mangaDirVM.progress } returns MutableStateFlow(-1)
        every { mangaDirVM.uiEvents } returns emptyEvents
        
        every { mangaDexVM.isIndexing } returns MutableStateFlow(false)
        every { mangaDexVM.progress } returns MutableStateFlow(-1)
        every { mangaDexVM.uiEvents } returns emptyEvents

        every { fsAccessVM.uiEvents } returns emptyEvents
    }

    @Test
    fun `ConfigScreen_deve_exibir_todas_as_seções_de_configuração`() {
        composeTestRule.setContent {
            AcerolaTheme {
                CompositionLocalProvider(LocalSnackbarHostState provides SnackbarHostState()) {
                    Main.Config.Layout.Screen(
                        filePreferencesViewModel = filePrefsVM,
                        fileSystemAccessViewModel = fsAccessVM,
                        mangaDirectoryViewModel = mangaDirVM,
                        mangaDexViewModel = mangaDexVM,
                        metadataSettingsViewModel = metadataVM,
                        themeViewModel = themeVM
                    )
                }
            }
        }

        // Verifica títulos das seções com textos completos para evitar ambiguidades de substring
        composeTestRule.onNodeWithText("Aparência", ignoreCase = true)
            .performScrollTo()
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Configuração dos arquivos", ignoreCase = true)
            .performScrollTo()
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Biblioteca", ignoreCase = true)
            .performScrollTo()
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Configuração do mangadex", ignoreCase = true)
            .performScrollTo()
            .assertIsDisplayed()
    }
}
