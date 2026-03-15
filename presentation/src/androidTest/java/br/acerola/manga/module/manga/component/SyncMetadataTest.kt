package br.acerola.manga.module.manga.component

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.manga.common.ux.theme.AcerolaTheme
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.module.manga.Manga
import org.junit.Rule
import org.junit.Test

class SyncMetadataTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `SyncMetadata_deve_exibir_as_seções_de_MangaDex_e_Arquivo_Local`() {
        val remoteInfo = MangaRemoteInfoDto(
            mirrorId = "1",
            title = "Manga Teste",
            description = "Desc",
            status = "Ongoing"
        )

        composeTestRule.setContent {
            AcerolaTheme {
                Manga.Component.SyncMetadata(
                    remoteInfo = remoteInfo,
                    onSyncMangadexInfo = {},
                    onSyncMangadexChapters = {},
                    onSyncComicInfo = {},
                    onSyncComicInfoChapters = {}
                )
            }
        }

        // Verifica se os cabeçalhos de grupo aparecem
        composeTestRule.onNodeWithText("MangaDex", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Arquivo Local", substring = true).assertIsDisplayed()
    }
}
