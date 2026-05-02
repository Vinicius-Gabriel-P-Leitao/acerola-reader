package br.acerola.comic.common.ux.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.rememberNavController
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.theme.AcerolaTheme
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
                Acerola.Component.BottomBar(navController = navController)
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
                Acerola.Component.TopBar(title = "Teste Titulo")
            }
        }

        composeTestRule.onNodeWithText("Teste Titulo").assertIsDisplayed()
    }

    @Test
    fun `SideBar_deve_renderizar_em_modo_paisagem`() {
        composeTestRule.setContent {
            val navController = androidx.navigation.compose.rememberNavController()
            AcerolaTheme {
                Acerola.Component.SideBar(navController = navController)
            }
        }

        // Verifica se ao menos um dos ícones/labels principais está presente na SideBar
        composeTestRule.onNodeWithText("home", ignoreCase = true).assertIsDisplayed()
    }
}
