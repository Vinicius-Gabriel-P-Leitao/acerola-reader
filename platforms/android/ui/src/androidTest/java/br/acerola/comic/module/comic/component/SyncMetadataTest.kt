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
        remoteInfo: ComicMetadataDto? = ComicMetadataDto(title = "Test Comic", description = "Desc", status = "Ongoing"),
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
    fun should_display_mangadex_anilist_and_comicinfo_sections_when_external_sync_enabled() {
        setContent(externalSyncEnabled = true)

        composeTestRule.onNodeWithText("MangaDex", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("AniList", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("ComicInfo", substring = true).assertIsDisplayed()
    }

    @Test
    fun should_not_display_mangadex_and_anilist_when_external_sync_disabled() {
        setContent(externalSyncEnabled = false)

        composeTestRule.onNodeWithText("MangaDex", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("AniList", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("ComicInfo", substring = true).assertIsDisplayed()
    }
}
