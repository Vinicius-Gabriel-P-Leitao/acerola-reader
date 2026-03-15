package br.acerola.manga.module.home.component

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.manga.common.ux.theme.AcerolaTheme
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import org.junit.Rule
import org.junit.Test

class MangaGridItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `MangaGridItem_deve_exibir_o_título_do_mangá_corretamente_abaixo_da_capa`() {
        val manga = MangaDto(
            directory = MangaDirectoryDto(1, "Pasta Manga", "", null, null, 0, null),
            remoteInfo = MangaRemoteInfoDto(
                mirrorId = "1", title = "Título do Manga", description = "", status = ""
            )
        )

        composeTestRule.setContent {
            AcerolaTheme {
                MangaGridItem(manga = manga, onClick = {})
            }
        }

        // Valida se o título é renderizado corretamente
        composeTestRule.onNodeWithText("Título do Manga").assertIsDisplayed()
    }
}
