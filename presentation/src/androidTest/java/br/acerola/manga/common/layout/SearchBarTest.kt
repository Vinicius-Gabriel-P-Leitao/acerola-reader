package br.acerola.manga.common.layout

import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.component.SearchBar
import br.acerola.manga.common.ux.theme.AcerolaTheme
import org.junit.Rule
import org.junit.Test

class SearchBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `SearchBar_deve_filtrar_itens_ao_digitar_na_busca`() {
        val items = listOf("Dragon Ball", "Naruto", "One Piece")

        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.SearchBar(
                    items = items,
                    placeholder = "Buscar...",
                    itemKey = { it },
                    searchKey = { it }
                ) { item ->
                    Text(text = item)
                }
            }
        }

        // 1. Verifica se todos aparecem inicialmente
        composeTestRule.onNodeWithText("Dragon Ball").assertIsDisplayed()
        composeTestRule.onNodeWithText("Naruto").assertIsDisplayed()

        // 2. Digita \"One\" na busca
        composeTestRule.onNodeWithText("Buscar...").performTextInput("One")

        // 3. Verifica se apenas One Piece aparece
        composeTestRule.onNodeWithText("One Piece").assertIsDisplayed()
        composeTestRule.onNodeWithText("Naruto").assertDoesNotExist()
    }

    @Test
    fun `SearchBar_deve_exibir_mensagem_de_vazio_quando_não_houver_resultados`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Acerola.Component.SearchBar(
                    items = listOf("Bleach"),
                    placeholder = "Buscar...",
                    itemKey = { it },
                    searchKey = { it }
                ) { Text(it) }
            }
        }

        composeTestRule.onNodeWithText("Buscar...").performTextInput("Inexistente")
        composeTestRule.onNodeWithText("Nenhum resultado encontrado", substring = true).assertIsDisplayed()
    }
}
