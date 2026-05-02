package br.acerola.comic.common.ux.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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

class GroupedHeroItemTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_exibir_titulo_do_item_principal() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.GroupedHeroButton(
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
                Acerola.Component.GroupedHeroButton(
                    title = "Configurações",
                    description = "Ajuste as preferências",
                    icon = Icons.Default.Settings,
                )
            }
        }

        composeTestRule.onNodeWithText("Ajuste as preferências").assertIsDisplayed()
    }

    @Test
    fun deve_invocar_onClick_ao_clicar_no_item() {
        var clicked = false

        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.GroupedHeroButton(
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
    fun deve_exibir_item_aninhado_quando_nestedItem_fornecido() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.GroupedHeroButton(
                    title = "Item Principal",
                    icon = Icons.Default.Settings,
                    nestedItem = {
                        Acerola.Component.HeroNestedButton(
                            title = "Item Aninhado",
                            icon = Icons.Default.Sync,
                            onClick = {},
                        )
                    },
                )
            }
        }

        composeTestRule.onNodeWithText("Item Principal").assertIsDisplayed()
        composeTestRule.onNodeWithText("Item Aninhado").assertIsDisplayed()
    }

    @Test
    fun nao_deve_exibir_item_aninhado_quando_nestedItem_e_nulo() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.GroupedHeroButton(
                    title = "Item Sem Aninhado",
                    icon = Icons.Default.Settings,
                    nestedItem = null,
                )
            }
        }

        composeTestRule.onNodeWithText("Item Sem Aninhado").assertIsDisplayed()
        composeTestRule.onNodeWithText("Item Aninhado").assertDoesNotExist()
    }

    @Test
    fun deve_renderizar_slot_de_action() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.GroupedHeroButton(
                    title = "Item com Action",
                    icon = Icons.Default.Settings,
                    action = { Text("Badge") },
                )
            }
        }

        composeTestRule.onNodeWithText("Item com Action").assertIsDisplayed()
        composeTestRule.onNodeWithText("Badge").assertIsDisplayed()
    }

    @Test
    fun deve_renderizar_icone_via_slot_composable() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.GroupedHeroButton(
                    title = "Item com Slot de Ícone",
                    icon = { Text("IC") },
                )
            }
        }

        composeTestRule.onNodeWithText("Item com Slot de Ícone").assertIsDisplayed()
        composeTestRule.onNodeWithText("IC").assertIsDisplayed()
    }
}
