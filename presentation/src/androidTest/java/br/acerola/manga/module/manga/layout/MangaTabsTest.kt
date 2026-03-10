package br.acerola.manga.module.manga.layout

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.manga.common.theme.AcerolaTheme
import br.acerola.manga.module.manga.MainTab
import org.junit.Rule
import org.junit.Test

class MangaTabsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `MangaTabs_deve_exibir_o_número_total_de_capítulos_na_aba_correspondente`() {
        composeTestRule.setContent {
            AcerolaTheme {
                MangaTabs(
                    totalChapters = 150,
                    activeTab = MainTab.CHAPTERS,
                    onTabSelected = {}
                )
            }
        }

        // Valida texto formatado: Capítulos (150)
        composeTestRule.onNodeWithText("Capítulos (150)", substring = true).assertIsDisplayed()
    }

    @Test
    fun `clique_em_uma_aba_inativa_deve_disparar_a_troca_de_contexto`() {
        var selectedTab: MainTab? = null

        composeTestRule.setContent {
            AcerolaTheme {
                MangaTabs(
                    totalChapters = 10,
                    activeTab = MainTab.CHAPTERS,
                    onTabSelected = { selectedTab = it }
                )
            }
        }

        // Clica na aba de Configurações
        composeTestRule.onNodeWithText("Configurações").performClick()

        assert(selectedTab == MainTab.SETTINGS)
    }
}
