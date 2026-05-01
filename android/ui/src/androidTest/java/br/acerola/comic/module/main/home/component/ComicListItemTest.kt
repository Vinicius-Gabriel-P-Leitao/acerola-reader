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
    fun `MangaListItem_deve_exibir_o_título_do_quadrinho_corretamente`() {
        val comic =
            ComicDto(
                directory =
                    ComicDirectoryDto(
                        id = 1L,
                        name = "Pasta Comic",
                        path = "",
                        coverUri = null,
                        bannerUri = null,
                        lastModified = 0L,
                        archiveTemplateFk = null,
                    ),
                remoteInfo =
                    ComicMetadataDto(
                        title = "Título do Comic",
                        description = "",
                        status = "",
                    ),
            )

        composeTestRule.setContent {
            AcerolaTheme {
                Main.Common.Component.ComicListItem(comic = comic, onClick = {})
            }
        }

        // Valida se o título é renderizado corretamente
        composeTestRule.onNodeWithText("Título do Comic").assertIsDisplayed()
    }
}
