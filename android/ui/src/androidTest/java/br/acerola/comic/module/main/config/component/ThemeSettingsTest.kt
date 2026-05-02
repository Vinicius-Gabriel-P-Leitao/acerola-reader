package br.acerola.comic.module.main.config.component

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import br.acerola.comic.config.preference.types.AppTheme
import br.acerola.comic.module.main.Main
import br.acerola.comic.ui.R
import org.junit.Rule
import org.junit.Test

class ThemeSettingsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun deve_exibir_lista_de_temas_e_permitir_selecao() {
        var selectedTheme: AppTheme? = null

        composeTestRule.setContent {
            Main.Config.Component.ThemeSettings(
                currentTheme = AppTheme.CATPPUCCIN,
                onThemeChange = { selectedTheme = it },
            )
        }

        val nordLabel = context.getString(R.string.title_settings_nord_theme)

        // Tenta clicar diretamente, se falhar tenta fazer o scroll na lista
        try {
            composeTestRule.onNodeWithText(nordLabel, ignoreCase = true).performClick()
        } catch (e: AssertionError) {
            composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasText(nordLabel, ignoreCase = true))
            composeTestRule.onNodeWithText(nordLabel, ignoreCase = true).performClick()
        }

        assert(selectedTheme == AppTheme.NORD)
    }
}
