package br.acerola.manga.common.layout

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.platform.app.InstrumentationRegistry
import br.acerola.manga.common.ux.theme.AcerolaTheme
import br.acerola.manga.presentation.R
import org.junit.Rule
import org.junit.Test

class NavigationBarsTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun `NavigationBottomBar_deve_exibir_labels_para_as_abas_principais`() {
        composeTestRule.setContent {
            _root_ide_package_.br.acerola.manga.common.ux.theme.AcerolaTheme {
                val navController = rememberNavController()
                _root_ide_package_.br.acerola.manga.common.ux.layout.NavigationBottomBar(navController = navController)
            }
        }

        // Valida labels globais (strings.xml)
        val homeLabel = context.getString(R.string.label_home_activity)
        val historyLabel = context.getString(R.string.label_history_activity)
        val configLabel = context.getString(R.string.label_config_activity)

        composeTestRule.onNodeWithText(homeLabel, ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(historyLabel, ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(configLabel, ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun `NavigationTopBar_deve_exibir_ação_de_navegação_de_retorno`() {
        composeTestRule.setContent {
            _root_ide_package_.br.acerola.manga.common.ux.theme.AcerolaTheme {
                val navController = rememberNavController()
                _root_ide_package_.br.acerola.manga.common.ux.layout.NavigationTopBar(navController = navController)
            }
        }

        // Valida botão de voltar por descrição parcial
        val backDescription = context.getString(R.string.description_icon_navigation_back)
        // Usamos substring=true pois o resource pode ter typos ou ser longo
        composeTestRule.onNodeWithContentDescription(backDescription, substring = true).assertIsDisplayed()
    }
}
