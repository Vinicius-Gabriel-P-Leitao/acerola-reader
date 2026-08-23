package br.acerola.comic.common.ux.component

import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.comic.common.ux.Acerola
import org.junit.Rule
import org.junit.Test

class ScrimTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun should_render_scrim_without_errors() {
        composeTestRule.setContent {
            Acerola.Component.Scrim()
        }
    }
}
