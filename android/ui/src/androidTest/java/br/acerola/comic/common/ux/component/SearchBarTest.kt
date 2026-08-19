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
    fun should_display_placeholder_and_react_to_text_input() {
        composeTestRule.setContent {
            Acerola.Component.SearchBar<String>(
                query = "",
                onQueryChange = {},
                onSearch = {},
                expanded = false,
                onExpandedChange = {},
                items = emptyList(),
                placeholder = "Search...",
                itemKey = { it },
                itemContent = {},
            )
        }

        composeTestRule.onNodeWithText("Search...").assertIsDisplayed()
    }

    @Test
    fun should_show_clear_button_when_there_is_text_and_expanded() {
        composeTestRule.setContent {
            Acerola.Component.SearchBar<String>(
                query = "Test text",
                onQueryChange = {},
                onSearch = {},
                expanded = true,
                onExpandedChange = {},
                items = emptyList(),
                placeholder = "Search...",
                itemKey = { it },
                itemContent = {},
            )
        }

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.common_clear)).assertIsDisplayed()
    }

    @Test
    fun should_show_no_results_message_when_list_is_empty_and_there_is_query() {
        composeTestRule.setContent {
            Acerola.Component.SearchBar<String>(
                query = "NadaEncontrado",
                onQueryChange = {},
                onSearch = {},
                expanded = true,
                onExpandedChange = {},
                items = emptyList(),
                placeholder = "Search...",
                itemKey = { it },
                itemContent = {},
            )
        }

        composeTestRule.onNodeWithText(context.getString(R.string.common_no_results)).assertIsDisplayed()
    }
}
