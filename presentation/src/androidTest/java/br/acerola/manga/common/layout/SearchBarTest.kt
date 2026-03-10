package br.acerola.manga.common.layout

import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.manga.common.theme.AcerolaTheme
import org.junit.Rule
import org.junit.Test

class SearchBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `SearchBar_deve_filtrar_a_lista_de_itens_dinamicamente_ao_digitar`() {
        val items = listOf("Naruto", "One Piece", "Bleach")

        composeTestRule.setContent {
            AcerolaTheme {
                SearchBar(
                    items = items,
                    placeholder = "Buscar...",
                    itemKey = { it },
                    searchKey = { it },
                    itemContent = { Text(text = it) }
                )
            }
        }

        // Ativa a barra de busca
        composeTestRule.onNodeWithText("Buscar...").performClick()

        // Filtra por "One"
        composeTestRule.onNodeWithText("Buscar...").performTextInput("One")

        // Valida se o filtro funcionou
        composeTestRule.onNodeWithText("One Piece").assertIsDisplayed()
        composeTestRule.onNodeWithText("Naruto").assertDoesNotExist()
    }

    @Test
    fun `SearchBar_deve_exibir_estado_de_erro_quando_nenhum_resultado_é_encontrado`() {
        val items = listOf("Dragon Ball")

        composeTestRule.setContent {
            AcerolaTheme {
                SearchBar(
                    items = items,
                    placeholder = "Buscar...",
                    itemKey = { it },
                    searchKey = { it },
                    itemContent = { Text(text = it) }
                )
            }
        }

        composeTestRule.onNodeWithText("Buscar...").performClick()
        composeTestRule.onNodeWithText("Buscar...").performTextInput("Manga Inexistente")

        // Valida mensagem de estado vazio
        composeTestRule.onNodeWithText("Nenhum resultado encontrado").assertIsDisplayed()
    }
}
