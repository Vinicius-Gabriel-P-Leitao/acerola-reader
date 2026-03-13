package br.acerola.manga.common.layout

import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import br.acerola.manga.common.theme.AcerolaTheme
import br.acerola.manga.presentation.R
import org.junit.Rule
import org.junit.Test

class SearchBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun `SearchBar_deve_filtrar_a_lista_de_itens_dinamicamente_ao_digitar`() {
        val items = listOf("Naruto", "One Piece", "Bleach")
        val placeholder = "Buscar..."

        composeTestRule.setContent {
            AcerolaTheme {
                SearchBar(
                    items = items,
                    placeholder = placeholder,
                    itemKey = { it },
                    searchKey = { it },
                    itemContent = { Text(text = it) }
                )
            }
        }

        // Ativa a barra de busca
        composeTestRule.onNodeWithText(placeholder).performClick()

        // Filtra por "One"
        composeTestRule.onNodeWithText(placeholder).performTextInput("One")

        // Valida se o filtro funcionou
        composeTestRule.onNodeWithText("One Piece").assertIsDisplayed()
        composeTestRule.onNodeWithText("Naruto").assertDoesNotExist()
    }

    @Test
    fun `SearchBar_deve_exibir_estado_de_erro_quando_nenhum_resultado_é_encontrado`() {
        val items = listOf("Dragon Ball")
        val placeholder = "Buscar..."

        composeTestRule.setContent {
            AcerolaTheme {
                SearchBar(
                    items = items,
                    placeholder = placeholder,
                    itemKey = { it },
                    searchKey = { it },
                    itemContent = { Text(text = it) }
                )
            }
        }

        composeTestRule.onNodeWithText(placeholder).performClick()
        composeTestRule.onNodeWithText(placeholder).performTextInput("Manga Inexistente")

        // Valida mensagem de estado vazio
        val emptyMessage = context.getString(R.string.description_text_search_no_results)
        composeTestRule.onNodeWithText(emptyMessage).assertIsDisplayed()
    }
}
