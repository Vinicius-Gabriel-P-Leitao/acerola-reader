package br.acerola.comic.module.main.config.template

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import br.acerola.comic.module.main.Main
import br.acerola.comic.module.main.config.component.SelectComicDirectory
import org.junit.Rule
import org.junit.Test

class SelectComicDirectoryTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_exibir_botao_de_acesso() {
        composeTestRule.setContent {
            Main.Config.Component.SelectComicDirectory(folderName = null, onFolderSelected = {})
        }
        // No FolderAccess.kt o contentDescription é buscado do strings.xml R.string.description_icon_select_folder_comics
        // "Selecionar pasta de quadrinhos"
        composeTestRule.onNodeWithContentDescription("pasta", substring = true, ignoreCase = true).assertIsDisplayed()
    }
}
