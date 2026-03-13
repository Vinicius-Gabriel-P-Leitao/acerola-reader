package br.acerola.manga.common.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.manga.common.theme.AcerolaTheme
import org.junit.Rule
import org.junit.Test

class FloatingToolTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `FloatingTool_deve_expandir_e_exibir_sub-itens_ao_ser_acionado`() {
        var itemClicked = false
        val items = listOf(
            FloatingToolItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = "Config") },
                label = "Configurações",
                onClick = { itemClicked = true }
            )
        )

        composeTestRule.setContent {
            AcerolaTheme {
                FloatingTool(
                    icon = { Icon(Icons.Default.Add, contentDescription = "Menu") },
                    items = items
                )
            }
        }

        // Garante que o item secundário não está visível inicialmente
        composeTestRule.onNodeWithContentDescription("Menu").assertIsDisplayed()
        composeTestRule.onNodeWithText("Configurações").assertDoesNotExist()

        // Aciona a expansão
        composeTestRule.onNodeWithContentDescription("Menu").performClick()

        // Valida se o item expandido aparece e responde ao clique (usando texto do label)
        composeTestRule.onNodeWithText("Configurações").assertIsDisplayed().performClick()
        
        assert(itemClicked)
    }
}
