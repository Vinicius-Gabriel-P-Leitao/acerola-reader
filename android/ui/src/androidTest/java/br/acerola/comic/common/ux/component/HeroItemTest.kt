package br.acerola.comic.common.ux.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Switch
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

class HeroItemTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_exibir_titulo_do_item() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.HeroItem(
                    title = "Configurações",
                    icon = Icons.Default.Settings,
                )
            }
        }

        composeTestRule.onNodeWithText("Configurações").assertIsDisplayed()
    }

    @Test
    fun deve_exibir_descricao_quando_fornecida() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.HeroItem(
                    title = "Configurações",
                    description = "Descrição do item",
                    icon = Icons.Default.Settings,
                )
            }
        }

        composeTestRule.onNodeWithText("Descrição do item").assertIsDisplayed()
    }

    @Test
    fun nao_deve_exibir_descricao_quando_nula() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.HeroItem(
                    title = "Sem Descrição",
                    description = null,
                    icon = Icons.Default.Settings,
                )
            }
        }

        composeTestRule.onNodeWithText("Sem Descrição").assertIsDisplayed()
    }

    @Test
    fun deve_invocar_onClick_ao_clicar_no_item() {
        var clicked = false

        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.HeroItem(
                    title = "Item Clicável",
                    icon = Icons.Default.Settings,
                    onClick = { clicked = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Item Clicável").performClick()

        assertTrue(clicked)
    }

    @Test
    fun deve_renderizar_slot_de_action() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.HeroItem(
                    title = "Item com Action",
                    icon = Icons.Default.Settings,
                    action = { Switch(checked = false, onCheckedChange = null) },
                )
            }
        }

        composeTestRule.onNodeWithText("Item com Action").assertIsDisplayed()
    }

    @Test
    fun deve_renderizar_bottomContent_quando_fornecido() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.HeroItem(
                    title = "Item com Conteúdo Extra",
                    icon = Icons.Default.Settings,
                    bottomContent = { Text("Conteúdo Inferior") },
                )
            }
        }

        composeTestRule.onNodeWithText("Item com Conteúdo Extra").assertIsDisplayed()
        composeTestRule.onNodeWithText("Conteúdo Inferior").assertIsDisplayed()
    }

    @Test
    fun deve_renderizar_icone_via_slot_composable() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.HeroItem(
                    title = "Item com Slot de Ícone",
                    icon = { Text("IC") },
                )
            }
        }

        composeTestRule.onNodeWithText("Item com Slot de Ícone").assertIsDisplayed()
        composeTestRule.onNodeWithText("IC").assertIsDisplayed()
    }
}
