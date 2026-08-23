package br.acerola.comic.module.comic.component

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import br.acerola.comic.module.comic.Comic
import org.junit.Rule
import org.junit.Test

class ComicExternalSyncToggleTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun should_toggle_external_sync_state_when_clicked() {
        var enabled = false
        composeTestRule.setContent {
            Comic.Component.ComicExternalSyncToggle(
                enabled = enabled,
                onToggle = { enabled = it },
            )
        }

        // Clica no switch
        composeTestRule
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .performClick()

        assert(enabled)
    }
}
