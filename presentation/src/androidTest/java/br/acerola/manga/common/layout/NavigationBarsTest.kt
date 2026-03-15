package br.acerola.manga.common.layout

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.layout.NavigationBottomBar
import br.acerola.manga.common.ux.layout.NavigationTopBar
import br.acerola.manga.common.ux.theme.AcerolaTheme
import org.junit.Rule
import org.junit.Test

class NavigationBarsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `NavigationBottomBar_deve_exibir_destinos_principais`() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            AcerolaTheme {
                Acerola.Layout.NavigationBottomBar(navController = navController)
            }
        }

        // Verifica os labels da navegação inferior
        composeTestRule.onNodeWithText("Início", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Histórico", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Config", substring = true).assertIsDisplayed()
    }

    @Test
    fun `NavigationTopBar_deve_exibir_o_titulo_da_rota_atual`() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            AcerolaTheme {
                Acerola.Layout.NavigationTopBar(navController = navController)
            }
        }

        // Como o NavController inicia na rota default, verificamos se o título aparece
        composeTestRule.onNodeWithText("Acerola", substring = true).assertIsDisplayed()
    }
}
