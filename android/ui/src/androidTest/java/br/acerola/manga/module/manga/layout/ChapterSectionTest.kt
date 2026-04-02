package br.acerola.manga.module.manga.layout

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.manga.__fixtures__.MangaFixtures
import br.acerola.manga.dto.ChapterDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.module.manga.Manga
import org.junit.Rule
import org.junit.Test

class ChapterSectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `deve_exibir_lista_de_capitulos_e_footer_de_paginacao`() {
        val chapters = ChapterDto(
            archive = MangaFixtures.createChapterArchivePageDto(
                items = MangaFixtures.createChapterList(2)
            ),
            remoteInfo = ChapterRemoteInfoPageDto(emptyList(), 20, 0, 0)
        )

        composeTestRule.setContent {
            LazyColumn {
                Manga.Layout.ChapterSection(
                    scope = this,
                    chapters = chapters,
                    currentPage = 0,
                    totalPages = 5,
                    onChapterClick = { _, _ -> },
                    onToggleRead = {},
                    onPageChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Capítulo 1", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Capítulo 2", substring = true).assertIsDisplayed()
        // Footer de paginação deve estar visível (1 / 5)
        composeTestRule.onNodeWithText("1 / 5", substring = true).assertIsDisplayed()
    }
}
