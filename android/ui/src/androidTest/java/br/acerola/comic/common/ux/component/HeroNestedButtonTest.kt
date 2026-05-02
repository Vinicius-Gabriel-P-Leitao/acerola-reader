package br.acerola.comic.common.ux.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.theme.AcerolaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HeroNestedButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_exibir_titulo_e_descricao_do_item_aninhado() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.HeroNestedButton(
                    title = "Sincronizar Capítulos",
                    description = "Busca capítulos remotos",
                    icon = Icons.Default.Sync,
                    onClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Sincronizar Capítulos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Busca capítulos remotos").assertIsDisplayed()
    }

    @Test
    fun deve_executar_callback_de_clique_no_item_aninhado() {
        var clicked = false
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.HeroNestedButton(
                    title = "Item Clicável",
                    icon = Icons.Default.Sync,
                    onClick = { clicked = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Item Clicável").performClick()
        assertTrue(clicked)
    }
}
