package br.acerola.comic.common.ux.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.theme.AcerolaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HeroNestedItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_exibir_titulo_do_item_aninhado() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.HeroNestedItem(
                    title = "Sincronizar Capítulos",
                    icon = Icons.Default.Sync,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Sincronizar Capítulos").assertIsDisplayed()
    }

    @Test
    fun deve_exibir_descricao_quando_fornecida() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.HeroNestedItem(
                    title = "Sincronizar Capítulos",
                    description = "Busca capítulos remotos",
                    icon = Icons.Default.Sync,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Busca capítulos remotos").assertIsDisplayed()
    }

    @Test
    fun nao_deve_exibir_descricao_quando_nula() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.HeroNestedItem(
                    title = "Sem Descrição",
                    description = null,
                    icon = Icons.Default.Sync,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Sem Descrição").assertIsDisplayed()
    }

    @Test
    fun deve_invocar_onClick_ao_clicar_no_item_aninhado() {
        var clicked = false

        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.HeroNestedItem(
                    title = "Item Clicável",
                    icon = Icons.Default.Sync,
                    onClick = { clicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Item Clicável").performClick()

        assertTrue(clicked)
    }

    @Test
    fun deve_renderizar_icone_via_slot_composable() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.HeroNestedItem(
                    title = "Item com Slot",
                    onClick = {},
                    icon = { Text("IC") }
                )
            }
        }

        composeTestRule.onNodeWithText("Item com Slot").assertIsDisplayed()
        composeTestRule.onNodeWithText("IC").assertIsDisplayed()
    }
}
