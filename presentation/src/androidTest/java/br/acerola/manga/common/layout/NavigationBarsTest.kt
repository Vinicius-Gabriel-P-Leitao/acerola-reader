package br.acerola.manga.common.layout

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import br.acerola.manga.common.theme.AcerolaTheme
import org.junit.Rule
import org.junit.Test

class NavigationBarsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `NavigationBottomBar_deve_exibir_labels_para_as_abas_principais`() {
        composeTestRule.setContent {
            AcerolaTheme {
                val navController = rememberNavController()
                NavigationBottomBar(navController = navController)
            }
        }

        // Valida labels globais (strings.xml)
        composeTestRule.onNodeWithText("home", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("histórico", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("configurações", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun `NavigationTopBar_deve_exibir_ação_de_navegação_de_retorno`() {
        composeTestRule.setContent {
            AcerolaTheme {
                val navController = rememberNavController()
                NavigationTopBar(navController = navController)
            }
        }

        // Valida botão de voltar por descrição parcial (ignore typo 'so' no resource)
        composeTestRule.onNodeWithContentDescription("volta", substring = true).assertIsDisplayed()
    }
}
