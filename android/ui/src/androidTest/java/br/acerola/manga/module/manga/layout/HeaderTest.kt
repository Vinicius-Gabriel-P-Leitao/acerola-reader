package br.acerola.manga.module.manga.layout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.manga.common.ux.theme.AcerolaTheme
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.module.manga.Manga
import org.junit.Rule
import org.junit.Test

class HeaderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `MangaHeader_deve_exibir_o_titulo_remoto_quando_disponivel`() {
        val manga = MangaDto(
            directory = MangaDirectoryDto(
                id = 1L,
                name = "Pasta",
                path = "",
                coverUri = null,
                bannerUri = null,
                lastModified = 0L,
                chapterTemplateFk = null
            ),
            remoteInfo = MangaMetadataDto(
                title = "Manga Fantástico",
                description = "Uma sinopse qualquer",
                status = "Lançando"
            )
        )

        composeTestRule.setContent {
            AcerolaTheme {
                Manga.Layout.Header(
                    manga = manga,
                    history = null,
                    onContinueClick = { _, _ -> }
                )
            }
        }

        // Verifica se o título remoto é exibido com prioridade
        composeTestRule.onNodeWithText("Manga Fantástico").assertIsDisplayed()
        
        // Verifica se a sinopse aparece
        composeTestRule.onNodeWithText("Uma sinopse qualquer").assertIsDisplayed()
    }
}
