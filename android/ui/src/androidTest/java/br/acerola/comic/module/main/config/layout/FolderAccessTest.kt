package br.acerola.comic.module.main.config.layout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import br.acerola.comic.module.main.Main
import org.junit.Rule
import org.junit.Test

class FolderAccessTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_exibir_botao_de_acesso() {
        composeTestRule.setContent {
            Main.Config.Layout.ComicDirectoryAccess(onFolderSelected = {})
        }
        // No FolderAccess.kt o contentDescription é buscado do strings.xml R.string.description_icon_select_folder_mangas
        // "Selecionar pasta de mangás"
        composeTestRule.onNodeWithContentDescription("pasta", substring = true, ignoreCase = true).assertIsDisplayed()
    }
}
