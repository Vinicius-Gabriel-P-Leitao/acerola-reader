package br.acerola.manga.common.ux.layout

import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.manga.common.ux.Acerola
import org.junit.Rule
import org.junit.Test

class StatusBarProtectionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_renderizar_status_bar_protection() {
        composeTestRule.setContent {
            Acerola.Layout.StatusBarProtection()
        }
    }
}
