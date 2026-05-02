package br.acerola.comic.common.ux.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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

class GroupedHeroButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_exibir_titulo_e_descricao_no_botao_agrupado() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.GroupedHeroButton(
                    title = "Configurações",
                    description = "Ajuste as preferências",
                    icon = Icons.Default.Settings,
                )
            }
        }

        composeTestRule.onNodeWithText("Configurações").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ajuste as preferências").assertIsDisplayed()
    }

    @Test
    fun deve_executar_callback_de_clique_ao_pressionar_o_botao() {
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
    fun deve_exibir_conteudo_aninhado_quando_fornecido() {
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

        composeTestRule.onNodeWithText("Item Aninhado").assertIsDisplayed()
    }
}
