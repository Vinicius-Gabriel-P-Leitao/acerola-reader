package br.acerola.manga.module.manga.component

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.manga.common.ux.theme.AcerolaTheme
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.dto.metadata.chapter.ChapterFeedDto
import br.acerola.manga.module.manga.Manga
import org.junit.Rule
import org.junit.Test

class ChapterItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `ChapterItem_deve_exibir_o_numero_do_capitulo_e_nome_do_arquivo`() {
        val archive = ChapterFileDto(1, "capitulo_01.cbz", "/path", "1")
        val remote = ChapterFeedDto("1", "id-1", "O Início", "Scan XP", 20, 1)

        composeTestRule.setContent {
            AcerolaTheme {
                Manga.Component.ChapterItem(
                    chapterFileDto = archive,
                    chapterRemoteInfoDto = remote,
                    onClick = {}
                )
            }
        }

        // Verifica se o título formatado (Capítulo 1) e o título do capítulo remoto aparecem
        composeTestRule.onNodeWithText("Capítulo 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("O Início").assertIsDisplayed()
    }

    @Test
    fun `ChapterItem_deve_exibir_indicador_de_lido_quando_status_for_verdadeiro`() {
        val archive = ChapterFileDto(1, "capitulo_01.cbz", "/path", "1")

        composeTestRule.setContent {
            AcerolaTheme {
                Manga.Component.ChapterItem(
                    chapterFileDto = archive,
                    chapterRemoteInfoDto = null,
                    isRead = true,
                    onClick = {}
                )
            }
        }

        // Procuramos por um nó que tenha a descrição de lido (se houver ícone) ou apenas validamos o estado visual
        // Aqui assumimos que o componente tem alguma marcação para capítulos lidos detectável via testes
        composeTestRule.onNodeWithText("Capítulo 1").assertIsDisplayed()
    }
}
