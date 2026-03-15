package br.acerola.manga.module.config

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.manga.common.ux.theme.AcerolaTheme
import br.acerola.manga.common.ux.theme.local.LocalSnackbarHostState
import br.acerola.manga.common.viewmodel.archive.FilePreferencesViewModel
import br.acerola.manga.common.viewmodel.archive.FileSystemAccessViewModel
import br.acerola.manga.common.viewmodel.library.archive.MangaDirectoryViewModel
import br.acerola.manga.common.viewmodel.library.metadata.MangaRemoteInfoViewModel
import br.acerola.manga.common.viewmodel.metadata.MetadataSettingsViewModel
import br.acerola.manga.common.viewmodel.theme.ThemeViewModel
import br.acerola.manga.module.main.Main
import br.acerola.manga.module.main.config.Screen
import io.mockk.mockk
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

        // Verifica títulos das seções
        composeTestRule.onNodeWithText("Aparência", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("arquivos", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Biblioteca", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("MangaDex", ignoreCase = true).assertIsDisplayed()
    }
}
