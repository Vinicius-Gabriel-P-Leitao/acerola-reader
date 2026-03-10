package br.acerola.manga.module.reader.layout

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import br.acerola.manga.common.theme.AcerolaTheme
import br.acerola.manga.presentation.R
import org.junit.Rule
import org.junit.Test

class ReaderTopBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun `ReaderTopBar_deve_exibir_título_e_subtítulo_dinâmicos_corretamente`() {
        composeTestRule.setContent {
            AcerolaTheme {
                ReaderTopBar(
                    title = "Capítulo 01",
                    subtitle = "Ordem: 1",
                    isVisible = true,
                    onBackClick = {},
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Capítulo 01").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ordem: 1").assertIsDisplayed()
    }

    @Test
    fun `clique_no_ícone_de_configurações_deve_acionar_a_ação_correspondente`() {
        var settingsClicked = false
        composeTestRule.setContent {
            AcerolaTheme {
                ReaderTopBar(
                    title = "Capítulo 01",
                    subtitle = "Ordem: 1",
                    isVisible = true,
                    onBackClick = {},
                    onSettingsClick = { settingsClicked = true }
                )
            }
        }

        // Aciona botão de configurações
        val settingsDescription = context.getString(R.string.label_config_activity)
        composeTestRule.onNodeWithContentDescription(settingsDescription, ignoreCase = true).performClick()
        assert(settingsClicked)
    }
}
