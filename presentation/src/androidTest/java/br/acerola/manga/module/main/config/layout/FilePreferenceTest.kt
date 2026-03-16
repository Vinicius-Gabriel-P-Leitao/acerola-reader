package br.acerola.manga.module.main.config.layout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.manga.config.preference.FileExtension
import br.acerola.manga.module.main.Main
import org.junit.Rule
import org.junit.Test

class FilePreferenceTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_exibir_opcoes_do_enum_file_extension() {
        composeTestRule.setContent {
            Main.Config.Layout.FilePreference(
                selected = FileExtension.CBZ,
                onSelect = {}
            )
        }
        FileExtension.entries.forEach { extension ->
            composeTestRule.onNodeWithText(extension.extension, ignoreCase = true).assertIsDisplayed()
        }
    }
}
