package br.acerola.comic.module.reader.template

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.common.ux.theme.AcerolaTheme
import br.acerola.comic.module.reader.Reader
import org.junit.Rule
import org.junit.Test

class TopBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `ReaderTopBar_deve_exibir_titulo_e_subtitulo_corretamente`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Reader.Template.TopBar(
                    title = "Solo Leveling",
                    subtitle = "Capítulo 150",
                    isVisible = true,
                    onBackClick = {},
                    onSettingsClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Solo Leveling").assertIsDisplayed()
        composeTestRule.onNodeWithText("Capítulo 150").assertIsDisplayed()
    }

    @Test
    fun `ReaderTopBar_deve_ficar_oculta_quando_isVisible_for_falso`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Reader.Template.TopBar(
                    title = "Qualquer",
                    subtitle = "Coisa",
                    isVisible = false,
                    onBackClick = {},
                    onSettingsClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Qualquer").assertDoesNotExist()
    }
}
