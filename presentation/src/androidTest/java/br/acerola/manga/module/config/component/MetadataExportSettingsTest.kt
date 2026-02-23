package br.acerola.manga.module.config.component

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.manga.common.theme.AcerolaTheme
import br.acerola.manga.common.viewmodel.metadata.MetadataSettingsViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class MetadataExportSettingsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModel = mockk<MetadataSettingsViewModel>(relaxed = true)

    @Test
    fun `MetadataExportSettings_deve_alternar_o_estado_do_switch_e_notificar_o_ViewModel`() {
        val state = MutableStateFlow(true)
        every { viewModel.generateComicInfo } returns state

        composeTestRule.setContent {
            AcerolaTheme {
                MetadataExportSettings(viewModel = viewModel)
            }
        }

        // Valida textos informativos
        composeTestRule.onNodeWithText("Gerar ComicInfo.xml").assertIsDisplayed()

        // Aciona o switch (buscando por ação de clique em componente sem label de texto direta)
        composeTestRule.onNode(hasSetTextAction().not() and hasClickAction()).performClick()

        // Valida interação com o ViewModel
        verify { viewModel.setGenerateComicInfo(any()) }
    }
}
