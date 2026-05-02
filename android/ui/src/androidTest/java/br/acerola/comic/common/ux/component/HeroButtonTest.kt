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

class HeroButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_exibir_titulo_e_descricao_corretamente() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.HeroButton(
                    title = "Configurações",
                    description = "Descrição do item",
                    icon = Icons.Default.Settings,
                )
            }
        }

        composeTestRule.onNodeWithText("Configurações").assertIsDisplayed()
        composeTestRule.onNodeWithText("Descrição do item").assertIsDisplayed()
    }

    @Test
    fun deve_executar_callback_de_clique() {
        var clicked = false
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.HeroButton(
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
    fun deve_renderizar_slot_de_acao_lateral() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.HeroButton(
                    title = "Item com Switch",
                    icon = Icons.Default.Settings,
                    action = { Switch(checked = false, onCheckedChange = null) },
                )
            }
        }

        composeTestRule.onNodeWithText("Item com Switch").assertIsDisplayed()
    }

    @Test
    fun deve_exibir_conteudo_inferior_quando_configurado() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.HeroButton(
                    title = "Item com Extra",
                    icon = Icons.Default.Settings,
                    bottomContent = { Text("Conteúdo Inferior") },
                )
            }
        }

        composeTestRule.onNodeWithText("Conteúdo Inferior").assertIsDisplayed()
    }
}
