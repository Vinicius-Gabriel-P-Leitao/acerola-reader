package br.acerola.comic.common.ux.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.theme.AcerolaTheme
import org.junit.Rule
import org.junit.Test

class FloatingToolTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `FloatingTool_deve_exibir_itens_ao_ser_clicado`() {
        var itemClicked = false
        val items = listOf(
            FloatingToolItem(
                label = "Ação 1",
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = { itemClicked = true }
            )
        )

        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.FloatingTool(
                    icon = { Icon(Icons.Default.Add, contentDescription = "Abrir") },
                    items = items
                )
            }
        }

        // 1. O item não deve estar visível inicialmente
        composeTestRule.onNodeWithText("Ação 1").assertDoesNotExist()

        // 2. Clica no botão principal para expandir
        composeTestRule.onNodeWithContentDescription("Abrir").performClick()

        // 3. Agora o item deve aparecer
        composeTestRule.onNodeWithText("Ação 1").assertIsDisplayed().performClick()

        assert(itemClicked)
    }
}
