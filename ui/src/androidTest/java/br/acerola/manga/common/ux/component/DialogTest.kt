package br.acerola.manga.common.ux.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.font.FontWeight
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.component.Dialog
import br.acerola.manga.common.ux.component.DialogButton
import org.junit.Rule
import org.junit.Test

class DialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_exibir_dialog_com_titulo_botoes_e_conteudo() {
        composeTestRule.setContent {
            Acerola.Component.Dialog(
                show = true,
                onDismiss = {},
                title = "Titulo Dialog",
                confirmButtonContent = {
                    Acerola.Component.DialogButton(
                        text = "Confirmar",
                        onClick = {}
                    )
                },
                dismissButtonContent = {
                    Acerola.Component.DialogButton(
                        text = "Cancelar",
                        onClick = {}
                    )
                }
            ) {
                Text("Conteudo Dialog")
            }
        }
        composeTestRule.onNodeWithText("Titulo Dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirmar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancelar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Conteudo Dialog").assertIsDisplayed()
    }

    @Test
    fun deve_exibir_dialog_de_descarte_conforme_especificacao() {
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
                        fontWeight = FontWeight.Bold
                    )
                },
                dismissButtonContent = {
                    Acerola.Component.DialogButton(
                        text = "Cancel",
                        onClick = {},
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            ) {
                Text("Are you sure? Changes will be lost.")
            }
        }

        composeTestRule.onNodeWithText("Discard changes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Are you sure? Changes will be lost.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Discard").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }
}
