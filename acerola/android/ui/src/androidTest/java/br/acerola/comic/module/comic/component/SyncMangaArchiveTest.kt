package br.acerola.comic.module.comic.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.acerola.comic.module.comic.Comic
import org.junit.Rule
import org.junit.Test

class SyncMangaArchiveTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun should_display_manga_archive_sync_options() {
        composeTestRule.setContent {
            Comic.Component.SyncMangaArchive(
                onSyncChapters = {},
                onRescanCover = {},
                onExtractFirstPageAsCover = {},
            )
        }

        composeTestRule.onNodeWithText("Sincronizar capítulos", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Sincronizar cover e banner", substring = true).assertIsDisplayed()
    }

    @Test
    fun should_call_onSyncChapters_when_clicking_option() {
        var clicked = false
        composeTestRule.setContent {
            Comic.Component.SyncMangaArchive(
                onSyncChapters = { clicked = true },
                onRescanCover = {},
                onExtractFirstPageAsCover = {},
            )
        }

        composeTestRule.onNodeWithText("Sincronizar capítulos", substring = true).performClick()
        assert(clicked)
    }
}
