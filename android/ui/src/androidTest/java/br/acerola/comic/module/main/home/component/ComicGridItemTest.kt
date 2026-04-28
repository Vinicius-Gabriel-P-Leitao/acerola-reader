package br.acerola.comic.module.main.home.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.common.ux.theme.AcerolaTheme
import br.acerola.comic.dto.ComicDto
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.module.main.Main
import org.junit.Rule
import org.junit.Test

class ComicGridItemTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `MangaGridItem_deve_exibir_o_título_do_mangá_corretamente_abaixo_da_capa`() {
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
                Main.Home.Component.ComicGridItem(manga = manga, onClick = {})
            }
        }

        // Valida se o título é renderizado corretamente
        composeTestRule.onNodeWithText("Título do Manga").assertIsDisplayed()
    }
}
