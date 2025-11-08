package br.acerola.manga.ui.common.component

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.acerola.manga.ui.common.theme.AcerolaTheme
import org.junit.Rule
import org.junit.Test

class ModalTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testModal_confirmButton() {
        var confirmed = false
        var dismissedFromAction = false

        composeTestRule.setContent {
            var showModal by remember { mutableStateOf(true) }
            if (showModal) {
                AcerolaTheme {
                    Modal(
                        show = true,
                        onDismiss = { /* This is for dismissing by back press or scrim click */ },
                        title = "Test Modal",
                        content = { Text("This is the content.") },
                        dismissButtonContent = {
                            SmartButton(
                                type = ButtonType.TEXT,
                                onClick = {
                                    dismissedFromAction = true
                                    showModal = false
                                },
                                text = "Dismiss"
                            )
                        },
                        confirmButtonContent = {
                            SmartButton(
                                type = ButtonType.TEXT,
                                onClick = {
                                    confirmed = true
                                    showModal = false
                                },
                                text = "Confirm"
                            )
                        }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Confirm").performClick()
        assert(confirmed)
        assert(!dismissedFromAction)
        composeTestRule.onNodeWithText("Test Modal").assertDoesNotExist()
    }

    @Test
    fun testModal_dismissButton() {
        var confirmed = false
        var dismissedFromAction = false

        composeTestRule.setContent {
            var showModal by remember { mutableStateOf(true) }
            if (showModal) {
                AcerolaTheme {
                    Modal(
                        show = true,
                        onDismiss = { /* This is for dismissing by back press or scrim click */ },
                        title = "Test Modal",
                        content = { Text("This is the content.") },
                        dismissButtonContent = {
                            SmartButton(
                                type = ButtonType.TEXT,
                                onClick = {
                                    dismissedFromAction = true
                                    showModal = false
                                },
                                text = "Dismiss"
                            )
                        },
                        confirmButtonContent = {
                            SmartButton(
                                type = ButtonType.TEXT,
                                onClick = {
                                    confirmed = true
                                    showModal = false
                                },
                                text = "Confirm"
                            )
                        }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Dismiss").performClick()
        assert(dismissedFromAction)
        assert(!confirmed)
        composeTestRule.onNodeWithText("Test Modal").assertDoesNotExist()
    }
}
