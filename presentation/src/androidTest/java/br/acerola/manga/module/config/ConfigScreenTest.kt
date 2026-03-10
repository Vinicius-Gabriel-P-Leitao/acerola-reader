package br.acerola.manga.module.config

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import br.acerola.manga.common.theme.AcerolaTheme
import br.acerola.manga.common.viewmodel.archive.FilePreferencesViewModel
import br.acerola.manga.common.viewmodel.archive.FileSystemAccessViewModel
import br.acerola.manga.common.viewmodel.library.archive.MangaDirectoryViewModel
import br.acerola.manga.common.viewmodel.library.metadata.MangaRemoteInfoViewModel
import br.acerola.manga.common.viewmodel.metadata.MetadataSettingsViewModel
import br.acerola.manga.presentation.R
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class ConfigScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

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
        every { fsAccessVM.folderUri } returns null

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
        composeTestRule.onNodeWithText(context.getString(R.string.title_text_archive_configs_in_app)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.label_library_context)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.title_text_mangadex_configs_in_app)).assertIsDisplayed()
        
        // Valida presença de componentes fundamentais
        composeTestRule.onNodeWithText(context.getString(R.string.title_text_config_select_path_manga)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.title_preference_metadata_comic_info)).assertIsDisplayed()
    }
}
