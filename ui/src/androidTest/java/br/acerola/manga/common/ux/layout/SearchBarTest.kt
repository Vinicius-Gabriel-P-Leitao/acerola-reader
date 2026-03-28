package br.acerola.manga.common.ux.layout

import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
    fun `SearchBar_deve_exibir_itens_quando_ativo`() {
        val items = listOf("Dragon Ball", "Naruto", "One Piece")

        composeTestRule.setContent {
            var query by remember { mutableStateOf("") }
            var active by remember { mutableStateOf(false) }
            
            AcerolaTheme {
                Acerola.Component.SearchBar<String>(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = {},
                    active = active,
                    onActiveChange = { active = it },
                    items = items.filter { it.contains(query, ignoreCase = true) },
                    placeholder = "Buscar...",
                    itemKey = { it }
                ) { item ->
                    Text(text = item)
                }
            }
        }

        // Clica no SearchBar para ativar
        composeTestRule.onNodeWithText("Buscar...").performClick()

        // Verifica se Dragon Ball aparece na lista
        composeTestRule.onNodeWithText("Dragon Ball").assertIsDisplayed()

        // Digita "One" na busca
        composeTestRule.onNodeWithText("Buscar...").performTextInput("One")

        // Verifica se apenas One Piece aparece
        composeTestRule.onNodeWithText("One Piece").assertIsDisplayed()
        composeTestRule.onNodeWithText("Naruto").assertDoesNotExist()
    }

    @Test
    fun `SearchBar_deve_exibir_mensagem_de_vazio_quando_não_houver_resultados`() {
        composeTestRule.setContent {
            var query by remember { mutableStateOf("") }
            var active by remember { mutableStateOf(true) }

            AcerolaTheme {
                Acerola.Component.SearchBar<String>(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = {},
                    active = active,
                    onActiveChange = { active = it },
                    items = emptyList(),
                    placeholder = "Buscar...",
                    itemKey = { it }
                ) { Text(it) }
            }
        }

        composeTestRule.onNodeWithText("Nenhum resultado encontrado", substring = true).assertIsDisplayed()
    }
}
