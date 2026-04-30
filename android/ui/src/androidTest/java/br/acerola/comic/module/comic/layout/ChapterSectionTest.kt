package br.acerola.comic.module.comic.layout

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.dto.ChapterDto
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.archive.VolumeDto
import br.acerola.comic.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.comic.fixtures.ComicFixtures
import br.acerola.comic.module.comic.Comic
import org.junit.Rule
import org.junit.Test

class ChapterSectionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `deve_exibir_lista_de_capitulos_e_footer_de_paginacao`() {
        val chapters =
            ChapterDto(
                archive =
                    ComicFixtures.createChapterArchivePageDto(
                        items = ComicFixtures.createChapterList(2),
                    ),
                remoteInfo = ChapterRemoteInfoPageDto(emptyList(), 20, 0, 0),
            )

        composeTestRule.setContent {
            LazyColumn {
                Comic.Layout.chapterSection(
                    scope = this,
                    chapters = chapters,
                    currentPage = 0,
                    totalPages = 5,
                    onChapterClick = { _, _ -> },
                    onToggleRead = {},
                    onPageChange = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Cap", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("1 / 5", substring = true).assertIsDisplayed()
    }

    @Test
    fun `deve_exibir_headers_por_volume_quando_houver_transicao_de_volume`() {
        val volume1 = VolumeDto(id = 10L, name = "Vol. 1", volumeSort = "1", isSpecial = false)
        val volume2 = VolumeDto(id = 20L, name = "Vol. 2", volumeSort = "2", isSpecial = true)
        val chapters =
            ChapterDto(
                archive =
                    ComicFixtures
                        .createChapterArchivePageDto(
                            items =
                                listOf(
                                    ChapterFileDto(id = 1L, name = "Cap. 1", path = "", chapterSort = "1", volumeId = 10L),
                                    ChapterFileDto(id = 2L, name = "Cap. 2", path = "", chapterSort = "2", volumeId = 10L),
                                    ChapterFileDto(id = 3L, name = "Cap. 3", path = "", chapterSort = "3", volumeId = 20L),
                                ),
                        ).copy(volumes = listOf(volume1, volume2)),
                remoteInfo = ChapterRemoteInfoPageDto(emptyList(), 20, 0, 0),
                showVolumeHeaders = true,
            )

        composeTestRule.setContent {
            LazyColumn {
                Comic.Layout.chapterSection(
                    scope = this,
                    chapters = chapters,
                    currentPage = 0,
                    totalPages = 1,
                    onChapterClick = { _, _ -> },
                    onToggleRead = {},
                    onPageChange = {},
                    showVolumeHeaders = true,
                )
            }
        }

        composeTestRule.onNodeWithText("Vol. 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vol. 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 cap", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("1 cap", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Especial").assertIsDisplayed()
    }

    @Test
    fun `nao_deve_exibir_header_quando_showVolumeHeaders_for_falso`() {
        val volume1 = VolumeDto(id = 10L, name = "Vol. 1", volumeSort = "1", isSpecial = false)
        val chapters =
            ChapterDto(
                archive =
                    ComicFixtures
                        .createChapterArchivePageDto(
                            items =
                                listOf(
                                    ChapterFileDto(id = 1L, name = "Cap. 1", path = "", chapterSort = "1", volumeId = 10L),
                                    ChapterFileDto(id = 2L, name = "Cap. 2", path = "", chapterSort = "2", volumeId = 10L),
                                ),
                        ).copy(volumes = listOf(volume1)),
                remoteInfo = ChapterRemoteInfoPageDto(emptyList(), 20, 0, 0),
                showVolumeHeaders = false,
            )

        composeTestRule.setContent {
            LazyColumn {
                Comic.Layout.chapterSection(
                    scope = this,
                    chapters = chapters,
                    currentPage = 0,
                    totalPages = 1,
                    onChapterClick = { _, _ -> },
                    onToggleRead = {},
                    onPageChange = {},
                    showVolumeHeaders = false,
                )
            }
        }

        composeTestRule.onNodeWithText("Vol. 1").assertDoesNotExist()
    }
}
