package br.acerola.manga.module.manga.layout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.acerola.manga.common.ux.theme.AcerolaTheme
import br.acerola.manga.module.manga.state.MainTab
import br.acerola.manga.module.manga.Manga
import org.junit.Rule
import org.junit.Test

class TabsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `MangaTabs_deve_exibir_o_número_total_de_capítulos_na_tab_correspondente`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Manga.Layout.Tabs(
                    totalChapters = 150,
                    activeTab = MainTab.CHAPTERS,
                    onTabSelected = {}
                )
            }
        }

        // Verifica se o texto formatado com o número de capítulos aparece
        composeTestRule.onNodeWithText("150", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Capítulos", substring = true).assertIsDisplayed()
    }

    @Test
    fun `MangaTabs_deve_chamar_onTabSelected_ao_clicar_em_uma_aba`() {
        var selectedTab: MainTab? = null
        composeTestRule.setContent {
            AcerolaTheme {
                Manga.Layout.Tabs(
                    totalChapters = 10,
                    activeTab = MainTab.CHAPTERS,
                    onTabSelected = { selectedTab = it }
                )
            }
        }

        // Clica na aba de configurações (ou a segunda aba)
        composeTestRule.onNodeWithText("Configurações", substring = true).performClick()

        assert(selectedTab == MainTab.SETTINGS)
    }
}
