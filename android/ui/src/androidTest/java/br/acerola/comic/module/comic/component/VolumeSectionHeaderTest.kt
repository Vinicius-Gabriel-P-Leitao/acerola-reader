package br.acerola.comic.module.comic.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import br.acerola.comic.common.ux.theme.AcerolaTheme
import br.acerola.comic.dto.archive.VolumeArchiveDto
import br.acerola.comic.dto.archive.VolumeChapterGroupDto
import br.acerola.comic.module.comic.Comic
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VolumeSectionHeaderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun group(
        id: Long = 1L,
        name: String = "Volume 1",
        volumeSort: String = "0001",
        isSpecial: Boolean = false,
        coverUri: String? = null,
        totalChapters: Int = 8,
        loadedCount: Int = 8,
    ) = VolumeChapterGroupDto(
        volume = VolumeArchiveDto(id = id, name = name, volumeSort = volumeSort, isSpecial = isSpecial, coverUri = coverUri),
        items = emptyList(),
        totalChapters = totalChapters,
        loadedCount = loadedCount,
        hasMore = false,
    )

    @Test
    fun `should_display_title_and_summary_of_loaded_chapters`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Comic.Component.VolumeSectionHeader(
                    group = group(name = "Volume 1", loadedCount = 5, totalChapters = 8),
                    expanded = false,
                    onToggleExpanded = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Volume 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("5 de 8", substring = true).assertIsDisplayed()
    }

    @Test
    fun `should_derive_volume_number_from_volumeSort`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Comic.Component.VolumeSectionHeader(
                    group = group(name = "Volume 3", volumeSort = "0003"),
                    expanded = false,
                    onToggleExpanded = {},
                )
            }
        }

        composeTestRule.onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    fun `should_display_special_badge_even_when_collapsed`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Comic.Component.VolumeSectionHeader(
                    group = group(name = "Extra - Artbook", volumeSort = "0000", isSpecial = true),
                    expanded = false,
                    onToggleExpanded = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Especial").assertIsDisplayed()
    }

    @Test
    fun `should_not_display_special_badge_on_normal_volume`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Comic.Component.VolumeSectionHeader(
                    group = group(isSpecial = false),
                    expanded = false,
                    onToggleExpanded = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Especial").assertDoesNotExist()
    }

    @Test
    fun `click_on_header_should_call_onToggleExpanded`() {
        var toggled = false

        composeTestRule.setContent {
            AcerolaTheme {
                Comic.Component.VolumeSectionHeader(
                    group = group(name = "Volume 1"),
                    expanded = false,
                    onToggleExpanded = { toggled = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Volume 1").performClick()

        assertTrue(toggled)
    }

    @Test
    fun `cover_mode_should_display_title_and_summary_of_chapters_even_without_cover`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Comic.Component.CoverVolumeSectionHeader(
                    group = group(name = "Volume 1", coverUri = null, totalChapters = 4, loadedCount = 4),
                    expanded = false,
                    onToggleExpanded = {},
                    onExtractCover = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Volume 1").assertIsDisplayed()
    }

    @Test
    fun `cover_mode_long_press_should_call_onExtractCover`() {
        var extracted = false

        composeTestRule.setContent {
            AcerolaTheme {
                Comic.Component.CoverVolumeSectionHeader(
                    group = group(name = "Volume 1"),
                    expanded = false,
                    onToggleExpanded = {},
                    onExtractCover = { extracted = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Volume 1").performTouchInput { longClick() }

        assertTrue(extracted)
    }
}
