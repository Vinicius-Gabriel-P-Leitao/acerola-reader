package br.acerola.manga.module.config.component

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.manga.common.ux.theme.AcerolaTheme
import br.acerola.manga.common.viewmodel.metadata.MetadataSettingsViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class MetadataExportSettingsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModel = mockk<MetadataSettingsViewModel>(relaxed = true)

    @Test
    fun `MetadataExportSettings_deve_exibir_switch_de_geracao_de_ComicInfo`() {
        every { viewModel.generateComicInfo } returns MutableStateFlow(true)

        composeTestRule.setContent {
            AcerolaTheme {
                MetadataExportSettings(viewModel = viewModel)
            }
        }

        // Verifica se o título e a descrição da configuração aparecem
        composeTestRule.onNodeWithText("Gerar ComicInfo.xml", substring = true).assertIsDisplayed()
    }
}
