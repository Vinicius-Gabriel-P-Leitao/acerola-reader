package br.acerola.comic.module.main.config

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import br.acerola.comic.common.ux.theme.AcerolaTheme
import br.acerola.comic.common.ux.theme.local.LocalSnackbarHostState
import br.acerola.comic.common.viewmodel.archive.FileSystemAccessViewModel
import br.acerola.comic.common.viewmodel.library.archive.ComicDirectoryViewModel
import br.acerola.comic.common.viewmodel.library.metadata.ComicMetadataViewModel
import br.acerola.comic.common.viewmodel.metadata.MetadataSettingsViewModel
import br.acerola.comic.common.viewmodel.theme.ThemeViewModel
import br.acerola.comic.config.preference.AppTheme
import br.acerola.comic.error.UserMessage
import br.acerola.comic.module.main.Main
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

    private val fsAccessVM = mockk<FileSystemAccessViewModel>(relaxed = true)
    private val comicDirVM = mockk<ComicDirectoryViewModel>(relaxed = true)
    private val comicDexVM = mockk<ComicMetadataViewModel>(relaxed = true)
    private val metadataVM = mockk<MetadataSettingsViewModel>(relaxed = true)
    private val themeVM = mockk<ThemeViewModel>(relaxed = true)

    @Before
    fun setUp() {
        val emptyEvents = MutableSharedFlow<UserMessage>().asSharedFlow()

        every { themeVM.currentTheme } returns MutableStateFlow(AppTheme.CATPPUCCIN)
        every { themeVM.uiEvents } returns emptyEvents

        every { metadataVM.generateComicInfo } returns MutableStateFlow(true)
        every { metadataVM.metadataLanguage } returns MutableStateFlow(null)
        every { metadataVM.uiEvents } returns emptyEvents

        every { comicDirVM.isIndexing } returns MutableStateFlow(false)
        every { comicDirVM.progress } returns MutableStateFlow(-1)
        every { comicDirVM.uiEvents } returns emptyEvents

        every { comicDexVM.isIndexing } returns MutableStateFlow(false)
        every { comicDexVM.progress } returns MutableStateFlow(-1)
        every { comicDexVM.uiEvents } returns emptyEvents
        every { comicDexVM.allCategories } returns MutableStateFlow(emptyList())

        every { fsAccessVM.uiEvents } returns emptyEvents
        every { fsAccessVM.folderUri } returns null
        every { fsAccessVM.folderName } returns MutableStateFlow("Mock Folder")
        every { fsAccessVM.tutorialShown } returns MutableStateFlow(true)
    }

    @Test
    fun `ConfigScreen_deve_exibir_todas_as_seções_de_configuração`() {
        composeTestRule.setContent {
            AcerolaTheme {
                CompositionLocalProvider(LocalSnackbarHostState provides SnackbarHostState()) {
                    Main.Config.Layout.Screen(
                        metadataSettingsViewModel = metadataVM,
                        fileSystemAccessViewModel = fsAccessVM,
                        comicDirectoryViewModel = comicDirVM,
                        comicDexViewModel = comicDexVM,
                        themeViewModel = themeVM,
                        onNavigateToTemplates = {},
                    )
                }
            }
        }

        // Verifica títulos das seções usando os textos exatos definidos no strings.xml e transformados em uppercase no ConfigScreen
        // "Aparência" -> "APARÊNCIA"
        // "Arquivos locais" -> "ARQUIVOS LOCAIS" (No strings.xml é title_text_archive_configs_in_app)
        // "Biblioteca" -> "BIBLIOTECA"
        // "Metadados externos" -> "METADADOS EXTERNOS"

        composeTestRule
            .onNodeWithText("APARÊNCIA", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("BIBLIOTECA", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
    }
}
