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
    fun `MangaGridItem_deve_exibir_o_título_do_quadrinho_corretamente_abaixo_da_capa`() {
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
                Main.Home.Component.ComicGridItem(comic = comic, onClick = {})
            }
        }

        // Valida se o título é renderizado corretamente
        composeTestRule.onNodeWithText("Título do Comic").assertIsDisplayed()
    }
}
