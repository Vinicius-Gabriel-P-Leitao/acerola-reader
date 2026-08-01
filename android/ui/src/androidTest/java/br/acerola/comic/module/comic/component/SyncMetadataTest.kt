package br.acerola.comic.module.comic.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.common.ux.theme.AcerolaTheme
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.module.comic.Comic
import org.junit.Rule
import org.junit.Test

class SyncMetadataTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        remoteInfo: ComicMetadataDto? = ComicMetadataDto(title = "Comic Teste", description = "Desc", status = "Ongoing"),
        externalSyncEnabled: Boolean = true,
    ) {
        composeTestRule.setContent {
            AcerolaTheme {
                Comic.Component.SyncMetadata(
                    remoteInfo = remoteInfo,
                    externalSyncEnabled = externalSyncEnabled,
                    onSyncMangadexInfo = {},
                    onSyncComicInfo = {},
                    onSyncAnilistInfo = {},
                )
            }
        }
    }

    @Test
    fun deve_exibir_secoes_de_mangadex_anilist_e_comicinfo_quando_sync_externo_ativado() {
        setContent(externalSyncEnabled = true)

        composeTestRule.onNodeWithText("MangaDex", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("AniList", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("ComicInfo", substring = true).assertIsDisplayed()
    }

    @Test
    fun nao_deve_exibir_mangadex_e_anilist_quando_sync_externo_desativado() {
        setContent(externalSyncEnabled = false)

        composeTestRule.onNodeWithText("MangaDex", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("AniList", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("ComicInfo", substring = true).assertIsDisplayed()
    }
}
