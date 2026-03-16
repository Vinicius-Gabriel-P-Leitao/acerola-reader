package br.acerola.manga.common.ux.component

import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.manga.common.ux.Acerola
import org.junit.Rule
import org.junit.Test

class DividerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_renderizar_divider() {
        composeTestRule.setContent {
            Acerola.Component.Divider()
        }
    }
}
