package br.acerola.comic.module.comic.component

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.comic.common.ux.theme.AcerolaTheme
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.module.comic.Comic
import org.junit.Rule
import org.junit.Test

class SyncMetadataTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `SyncMetadata_deve_exibir_as_seções_de_MangaDex_e_Arquivo_Local`() {
        val remoteInfo = ComicMetadataDto(
            title = "Manga Teste",
            description = "Desc",
            status = "Ongoing"
        )

        composeTestRule.setContent {
            AcerolaTheme {
                Comic.Component.SyncMetadata(
                    remoteInfo = remoteInfo,
                    externalSyncEnabled = true,
                    onSyncMangadexInfo = {},
                    onSyncMangadexChapters = {},
                    onSyncComicInfo = {},
                    onSyncComicInfoChapters = {},
                    onSyncAnilistInfo = {}
                )
            }
        }

        // Verifica se os itens aparecem
        composeTestRule.onNodeWithText("MangaDex", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("ComicInfo", substring = true).assertIsDisplayed()
    }
}
