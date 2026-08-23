package br.acerola.comic.module.comic.template

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.acerola.comic.common.ux.theme.AcerolaTheme
import br.acerola.comic.module.comic.Comic
import br.acerola.comic.module.comic.state.MainTab
import org.junit.Rule
import org.junit.Test

class TabsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `ComicTabs_should_display_total_number_of_chapters_in_corresponding_tab`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Comic.Template.Tabs(
                    totalChapters = 150,
                    activeTab = MainTab.CHAPTERS,
                    onTabSelected = {},
                )
            }
        }

        // Verifica se o texto formatado com o número de capítulos aparece
        composeTestRule.onNodeWithText("150", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Capítulos", substring = true).assertIsDisplayed()
    }

    @Test
    fun `ComicTabs_should_call_onTabSelected_when_clicking_a_tab`() {
        var selectedTab: MainTab? = null
        composeTestRule.setContent {
            AcerolaTheme {
                Comic.Template.Tabs(
                    totalChapters = 10,
                    activeTab = MainTab.CHAPTERS,
                    onTabSelected = { selectedTab = it },
                )
            }
        }

        // Clica na aba de configurações (ou a segunda aba)
        composeTestRule.onNodeWithText("Configurações", substring = true).performClick()

        assert(selectedTab == MainTab.SETTINGS)
    }
}
