package br.acerola.manga.module.manga.layout

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.manga.common.theme.AcerolaTheme
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.manga.AuthorDto
import br.acerola.manga.dto.metadata.manga.GenreDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.local.database.entity.metadata.MetadataSource
import org.junit.Rule
import org.junit.Test

class MangaHeaderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `MangaHeader_deve_exibir_metadados_badges_e_gêneros_do_mangá_corretamente`() {
        val manga = MangaDto(
            directory = MangaDirectoryDto(
                id = 1,
                name = "Pasta Local",
                path = "",
                coverUri = null,
                bannerUri = null,
                lastModified = 0,
                chapterTemplate = null,
                hasComicInfo = false
            ),
            remoteInfo = MangaRemoteInfoDto(
                mirrorId = "1",
                title = "Manga de Teste",
                description = "Uma descrição longa",
                status = "Lançando",
                authors = AuthorDto("a1", "Autor X", "author"),
                genre = listOf(GenreDto("g1", "Ação"), GenreDto("g2", "Drama")),
                metadataSource = MetadataSource.MANGADEX
            )
        )

        composeTestRule.setContent {
            AcerolaTheme {
                MangaHeader(
                    manga = manga,
                    history = null,
                    onContinueClick = { _, _ -> }
                )
            }
        }

        // Valida Título e Autor
        composeTestRule.onNodeWithText("Manga de Teste").assertIsDisplayed()
        composeTestRule.onNodeWithText("Autor X").assertIsDisplayed()

        // Valida Badges de Status e Fonte
        composeTestRule.onNodeWithText("Lançando").assertIsDisplayed()
        composeTestRule.onNodeWithText("MANGADEX").assertIsDisplayed()

        // Valida Chips de Gênero
        composeTestRule.onNodeWithText("Ação").assertIsDisplayed()
        composeTestRule.onNodeWithText("Drama").assertIsDisplayed()

        // Valida Título de Sinopse
        composeTestRule.onNodeWithText("Sinopse", substring = true).assertIsDisplayed()
    }
}
