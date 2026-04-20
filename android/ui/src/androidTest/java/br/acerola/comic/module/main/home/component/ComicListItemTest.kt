package br.acerola.comic.module.main.home.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.common.ux.theme.AcerolaTheme
import br.acerola.comic.dto.ComicDto
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.module.main.Main
import br.acerola.comic.module.main.common.component.ComicListItem
import org.junit.Rule
import org.junit.Test

class ComicListItemTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `MangaListItem_deve_exibir_o_título_do_mangá_corretamente`() {
        val manga =
            ComicDto(
                directory =
                    ComicDirectoryDto(
                        id = 1L,
                        name = "Pasta Manga",
                        path = "",
                        coverUri = null,
                        bannerUri = null,
                        lastModified = 0L,
                        chapterTemplateFk = null,
                    ),
                remoteInfo =
                    ComicMetadataDto(
                        title = "Título do Manga",
                        description = "",
                        status = "",
                    ),
            )

        composeTestRule.setContent {
            AcerolaTheme {
                Main.Common.Component.ComicListItem(manga = manga, onClick = {})
            }
        }

        // Valida se o título é renderizado corretamente
        composeTestRule.onNodeWithText("Título do Manga").assertIsDisplayed()
    }
}
