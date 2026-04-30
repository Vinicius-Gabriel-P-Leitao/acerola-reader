package br.acerola.comic.module.comic.layout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.common.ux.theme.AcerolaTheme
import br.acerola.comic.dto.ComicDto
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.module.comic.Comic
import org.junit.Rule
import org.junit.Test

class HeaderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `MangaHeader_deve_exibir_o_titulo_remoto_quando_disponivel`() {
        val comic =
            ComicDto(
                directory =
                    ComicDirectoryDto(
                        id = 1L,
                        name = "Pasta",
                        path = "",
                        coverUri = null,
                        bannerUri = null,
                        lastModified = 0L,
                        chapterTemplateFk = null,
                    ),
                remoteInfo =
                    ComicMetadataDto(
                        title = "Comic Fantástico",
                        description = "Uma sinopse qualquer",
                        status = "Lançando",
                    ),
            )

        composeTestRule.setContent {
            AcerolaTheme {
                Comic.Layout.Header(
                    comic = comic,
                    history = null,
                    onContinueClick = { _, _ -> },
                )
            }
        }

        // Verifica se o título remoto é exibido com prioridade
        composeTestRule.onNodeWithText("Comic Fantástico").assertIsDisplayed()

        // Verifica se a sinopse aparece
        composeTestRule.onNodeWithText("Uma sinopse qualquer").assertIsDisplayed()
    }
}
