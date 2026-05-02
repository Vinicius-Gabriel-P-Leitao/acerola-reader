package br.acerola.comic.common.ux.component

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.ui.R
import org.junit.Rule
import org.junit.Test

class LanguagePickerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun deve_abrir_o_seletor_de_idiomas_ao_clicar_no_gatilho() {
        composeTestRule.setContent {
            Acerola.Component.LanguagePicker(
                selectedLanguage = "pt-br",
                onLanguageSelected = {},
                trigger = { onClick ->
                    Text("Selecionar Idioma", modifier = Modifier.clickable { onClick() })
                },
            )
        }

        composeTestRule.onNodeWithText("Selecionar Idioma").performClick()

        composeTestRule.onNodeWithText(context.getString(R.string.lang_pt_br)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.lang_en)).assertIsDisplayed()
    }

    @Test
    fun deve_chamar_o_callback_ao_selecionar_um_novo_idioma() {
        var selected: String? = null
        composeTestRule.setContent {
            Acerola.Component.LanguagePicker(
                selectedLanguage = "pt-br",
                onLanguageSelected = { selected = it },
                trigger = { onClick ->
                    Text("Trigger", modifier = Modifier.clickable { onClick() })
                },
            )
        }

        composeTestRule.onNodeWithText("Trigger").performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.lang_en)).performClick()

        assert(selected == "en")
    }
}
