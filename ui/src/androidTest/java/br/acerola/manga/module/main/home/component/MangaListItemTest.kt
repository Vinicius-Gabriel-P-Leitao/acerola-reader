package br.acerola.manga.module.main.home.component

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.manga.common.ux.theme.AcerolaTheme
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.module.main.Main
import br.acerola.manga.module.main.common.component.MangaListItem
import org.junit.Rule
import org.junit.Test

class MangaListItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `MangaListItem_deve_exibir_o_título_do_mangá_corretamente`() {
        val manga = MangaDto(
            directory = MangaDirectoryDto(
                id = 1L,
                name = "Pasta Manga",
                path = "",
                coverUri = null,
                bannerUri = null,
                lastModified = 0L,
                chapterTemplateFk = null
            ),
            remoteInfo = MangaMetadataDto(
                title = "Título do Manga", description = "", status = ""
            )
        )

        composeTestRule.setContent {
            AcerolaTheme {
                Main.Common.Component.MangaListItem(manga = manga, onClick = {})
            }
        }

        // Valida se o título é renderizado corretamente
        composeTestRule.onNodeWithText("Título do Manga").assertIsDisplayed()
    }
}
