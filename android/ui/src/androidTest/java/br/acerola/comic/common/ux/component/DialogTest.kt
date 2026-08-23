package br.acerola.comic.common.ux.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.font.FontWeight
import br.acerola.comic.common.ux.Acerola
import org.junit.Rule
import org.junit.Test

class DialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun should_display_dialog_with_title_buttons_and_content() {
        composeTestRule.setContent {
            Acerola.Component.Dialog(
                show = true,
                onDismiss = {},
                title = "Dialog Title",
                confirmButtonContent = {
                    Acerola.Component.DialogButton(
                        text = "Confirm",
                        onClick = {},
                    )
                },
                dismissButtonContent = {
                    Acerola.Component.DialogButton(
                        text = "Cancel",
                        onClick = {},
                    )
                },
            ) {
                Text("Dialog Content")
            }
        }
        composeTestRule.onNodeWithText("Dialog Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dialog Content").assertIsDisplayed()
    }

    @Test
    fun should_display_discard_dialog_according_to_specification() {
        composeTestRule.setContent {
            Acerola.Component.Dialog(
                show = true,
                onDismiss = {},
                title = "Discard changes",
                confirmButtonContent = {
                    Acerola.Component.DialogButton(
                        text = "Discard",
                        onClick = {},
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        fontWeight = FontWeight.Bold,
                    )
                },
                dismissButtonContent = {
                    Acerola.Component.DialogButton(
                        text = "Cancel",
                        onClick = {},
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            ) {
                Text("Are you sure? Changes will be lost.")
            }
        }

        composeTestRule.onNodeWithText("Discard changes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Are you sure? Changes will be lost.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Discard").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun should_display_new_pattern_dialog_with_correct_description() {
        composeTestRule.setContent {
            Acerola.Component.Dialog(
                show = true,
                onDismiss = {},
                title = "New Pattern",
                confirmButtonContent = {
                    Acerola.Component.DialogButton(
                        text = "Add",
                        onClick = {},
                        fontWeight = FontWeight.Bold,
                    )
                },
                dismissButtonContent = {
                    Acerola.Component.DialogButton(
                        text = "Cancel",
                        onClick = {},
                    )
                },
            ) {
                Text(
                    "{volume} representa o número do volume (usado para organização em pastas). {chapter} é o número do capítulo (obrigatório). {decimal} indica variação decimal do capítulo (ex: .5). Utilize * como wildcard para ignorar qualquer trecho de texto.",
                )
            }
        }

        composeTestRule.onNodeWithText("New Pattern").assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                "{volume} representa o número do volume (usado para organização em pastas). {chapter} é o número do capítulo (obrigatório). {decimal} indica variação decimal do capítulo (ex: .5). Utilize * como wildcard para ignorar qualquer trecho de texto.",
            ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Add").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }
}
