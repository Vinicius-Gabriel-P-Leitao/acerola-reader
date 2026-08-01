package br.acerola.comic.module.comic.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.common.ux.theme.AcerolaTheme
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.module.comic.Comic
import org.junit.Rule
import org.junit.Test

class ChapterItemTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `ChapterItem_deve_exibir_o_numero_do_capitulo_e_nome_do_arquivo`() {
        val archive = ChapterFileDto(1L, "capitulo_01.cbz", "/path", "1")

        composeTestRule.setContent {
            AcerolaTheme {
                Comic.Component.ChapterItem(
                    chapterFileDto = archive,
                    onClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Capitulo 1", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("capitulo_01.cbz").assertIsDisplayed()
    }

    @Test
    fun `ChapterItem_deve_exibir_indicador_de_lido_quando_status_for_verdadeiro`() {
        val archive = ChapterFileDto(1L, "capitulo_01.cbz", "/path", "1")

        composeTestRule.setContent {
            AcerolaTheme {
                Comic.Component.ChapterItem(
                    chapterFileDto = archive,
                    isRead = true,
                    onClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Capitulo 1", substring = true).assertIsDisplayed()
    }
}
