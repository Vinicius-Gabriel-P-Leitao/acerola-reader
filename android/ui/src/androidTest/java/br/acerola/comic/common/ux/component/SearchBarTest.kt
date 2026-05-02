package br.acerola.comic.common.ux.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.ui.R
import org.junit.Rule
import org.junit.Test

class SearchBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun deve_exibir_placeholder_e_reagir_a_entrada_de_texto() {
        composeTestRule.setContent {
            Acerola.Component.SearchBar<String>(
                query = "",
                onQueryChange = {},
                onSearch = {},
                expanded = false,
                onExpandedChange = {},
                items = emptyList(),
                placeholder = "Buscar...",
                itemKey = { it },
                itemContent = {},
            )
        }

        composeTestRule.onNodeWithText("Buscar...").assertIsDisplayed()
    }

    @Test
    fun deve_mostrar_botao_de_limpar_quando_houver_texto_e_estiver_expandido() {
        composeTestRule.setContent {
            Acerola.Component.SearchBar<String>(
                query = "Texto de teste",
                onQueryChange = {},
                onSearch = {},
                expanded = true,
                onExpandedChange = {},
                items = emptyList(),
                placeholder = "Buscar...",
                itemKey = { it },
                itemContent = {},
            )
        }

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.common_clear)).assertIsDisplayed()
    }

    @Test
    fun deve_mostrar_mensagem_de_nenhum_resultado_quando_a_lista_estiver_vazia_e_houver_query() {
        composeTestRule.setContent {
            Acerola.Component.SearchBar<String>(
                query = "NadaEncontrado",
                onQueryChange = {},
                onSearch = {},
                expanded = true,
                onExpandedChange = {},
                items = emptyList(),
                placeholder = "Buscar...",
                itemKey = { it },
                itemContent = {},
            )
        }

        composeTestRule.onNodeWithText(context.getString(R.string.common_no_results)).assertIsDisplayed()
    }
}
