package br.acerola.manga.common.layout

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.layout.BottomBar
import br.acerola.manga.common.ux.layout.TopBar
import br.acerola.manga.common.ux.theme.AcerolaTheme
import org.junit.Rule
import org.junit.Test

class NavigationBarsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `BottomBar_deve_exibir_destinos_principais`() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            AcerolaTheme {
                Acerola.Layout.BottomBar(navController = navController)
            }
        }

        // Verifica os labels da navegação inferior (usando strings do sistema se possível, ou aproximadas)
        // HOME, HISTORY, CONFIG
        composeTestRule.onNodeWithText("home", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Histórico", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Configurações", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun `TopBar_deve_exibir_o_titulo_informado`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Layout.TopBar(title = "Teste Titulo")
            }
        }

        composeTestRule.onNodeWithText("Teste Titulo").assertIsDisplayed()
    }
}
