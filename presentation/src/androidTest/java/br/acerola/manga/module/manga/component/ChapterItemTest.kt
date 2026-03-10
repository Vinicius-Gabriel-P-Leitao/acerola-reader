package br.acerola.manga.module.manga.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.manga.common.theme.AcerolaTheme
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.dto.metadata.chapter.ChapterFeedDto
import org.junit.Rule
import org.junit.Test

class ChapterItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `ChapterItem_deve_exibir_o_número_do_capítulo_e_o_título_remoto_se_disponíveis`() {
        val fileDto = ChapterFileDto(id = 1, name = "Capítulo 01.cbz", path = "", chapterSort = "1")
        val remoteDto = ChapterFeedDto(id = 1, title = "O Início", chapter = "1", scanlation = "Scan Top", pageCount = 20, source = emptyList())

        composeTestRule.setContent {
            AcerolaTheme {
                ChapterItem(
                    chapterFileDto = fileDto,
                    chapterRemoteInfoDto = remoteDto,
                    onClick = {}
                )
            }
        }

        // Valida número do capítulo (Capitulo 1)
        composeTestRule.onNodeWithText("Capitulo 1", substring = true).assertIsDisplayed()
        // Valida título do capítulo remoto
        composeTestRule.onNodeWithText("O Início").assertIsDisplayed()
        // Valida prefixo de scanlation
        composeTestRule.onNodeWithText("Scan: Scan Top", substring = true).assertIsDisplayed()
    }

    @Test
    fun `ChapterItem_deve_exibir_o_nome_do_arquivo_local_se_não_houver_metadados_remotos`() {
        val fileDto = ChapterFileDto(id = 1, name = "Capítulo Especial.cbz", path = "", chapterSort = "0")
        
        composeTestRule.setContent {
            AcerolaTheme {
                ChapterItem(
                    chapterFileDto = fileDto,
                    chapterRemoteInfoDto = null,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Capítulo Especial.cbz").assertIsDisplayed()
    }
}
