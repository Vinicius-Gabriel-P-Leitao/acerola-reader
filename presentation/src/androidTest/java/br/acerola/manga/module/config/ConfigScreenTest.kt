package br.acerola.manga.module.config

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.manga.common.theme.AcerolaTheme
import br.acerola.manga.common.viewmodel.archive.FilePreferencesViewModel
import br.acerola.manga.common.viewmodel.archive.FileSystemAccessViewModel
import br.acerola.manga.common.viewmodel.library.archive.MangaDirectoryViewModel
import br.acerola.manga.common.viewmodel.library.metadata.MangaRemoteInfoViewModel
import br.acerola.manga.common.viewmodel.metadata.MetadataSettingsViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class ConfigScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val filePrefsVM = mockk<FilePreferencesViewModel>(relaxed = true)
    private val fsAccessVM = mockk<FileSystemAccessViewModel>(relaxed = true)
    private val mangaDirVM = mockk<MangaDirectoryViewModel>(relaxed = true)
    private val mangaDexVM = mockk<MangaRemoteInfoViewModel>(relaxed = true)
    private val metadataSettingsVM = mockk<MetadataSettingsViewModel>(relaxed = true)

    @Test
    fun `ConfigScreen_deve_exibir_todas_as_seções_obrigatórias_corretamente`() {
        // Configura estados iniciais para os stubs
        every { mangaDirVM.progress } returns MutableStateFlow(-1)
        every { metadataSettingsVM.generateComicInfo } returns MutableStateFlow(true)
        every { filePrefsVM.selectedExtension } returns MutableStateFlow(br.acerola.manga.config.preference.FileExtension.CBZ)

        composeTestRule.setContent {
            AcerolaTheme {
                ConfigScreen(
                    filePreferencesViewModel = filePrefsVM,
                    fileSystemAccessViewModel = fsAccessVM,
                    mangaDirectoryViewModel = mangaDirVM,
                    mangaDexViewModel = mangaDexVM,
                    metadataSettingsViewModel = metadataSettingsVM
                )
            }
        }

        // Valida títulos de seções baseados no strings.xml
        composeTestRule.onNodeWithText("Configuração dos arquivos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Biblioteca").assertIsDisplayed()
        composeTestRule.onNodeWithText("Configuração do mangadex").assertIsDisplayed()
        
        // Valida presença de componentes fundamentais
        composeTestRule.onNodeWithText("Pasta dos mangás").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gerar ComicInfo.xml").assertIsDisplayed()
    }
}
