package br.acerola.comic.module.comic.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.common.ux.theme.AcerolaTheme
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.dto.metadata.comic.source.ComicInfoSourceDto
import br.acerola.comic.dto.metadata.comic.source.ComicSourcesDto
import br.acerola.comic.dto.metadata.comic.source.MangadexSourceDto
import br.acerola.comic.module.comic.Comic
import br.acerola.comic.pattern.MetadataSourcePattern
import org.junit.Rule
import org.junit.Test

class SyncMetadataTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        remoteInfo: ComicMetadataDto? = ComicMetadataDto(title = "Manga Teste", description = "Desc", status = "Ongoing"),
        externalSyncEnabled: Boolean = true,
        onSyncMangadexChapters: () -> Unit = {},
        onSyncComicInfoChapters: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            AcerolaTheme {
                Comic.Component.SyncMetadata(
                    remoteInfo = remoteInfo,
                    externalSyncEnabled = externalSyncEnabled,
                    onSyncMangadexInfo = {},
                    onSyncMangadexChapters = onSyncMangadexChapters,
                    onSyncComicInfo = {},
                    onSyncComicInfoChapters = onSyncComicInfoChapters,
                    onSyncAnilistInfo = {}
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

    @Test
    fun deve_exibir_item_aninhado_de_capitulos_mangadex_quando_ativo_e_com_fonte() {
        val remoteInfo = ComicMetadataDto(
            id = 1L,
            title = "Manga Teste",
            description = "Desc",
            status = "Ongoing",
            syncSource = MetadataSourcePattern.MANGADEX,
            sources = ComicSourcesDto(
                mangadex = MangadexSourceDto(mangadexId = "abc-123")
            )
        )

        setContent(remoteInfo = remoteInfo)

        composeTestRule.onNodeWithText("MangaDex", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Sincronizar capítulos", substring = true).assertIsDisplayed()
    }

    @Test
    fun nao_deve_exibir_item_aninhado_de_capitulos_mangadex_quando_nao_ativo() {
        val remoteInfo = ComicMetadataDto(
            id = 1L,
            title = "Manga Teste",
            description = "Desc",
            status = "Ongoing",
            syncSource = MetadataSourcePattern.COMIC_INFO,
            sources = ComicSourcesDto(
                mangadex = MangadexSourceDto(mangadexId = "abc-123")
            )
        )

        setContent(remoteInfo = remoteInfo)

        composeTestRule.onNodeWithText("MangaDex", substring = true).assertIsDisplayed()
    }

    @Test
    fun deve_exibir_item_aninhado_de_capitulos_comicinfo_quando_ativo_e_com_fonte() {
        val remoteInfo = ComicMetadataDto(
            title = "Manga Teste",
            description = "Desc",
            status = "Ongoing",
            syncSource = MetadataSourcePattern.COMIC_INFO,
            sources = ComicSourcesDto(
                comicInfo = ComicInfoSourceDto(localHash = "hash-abc")
            )
        )

        setContent(remoteInfo = remoteInfo, externalSyncEnabled = false)

        composeTestRule.onNodeWithText("ComicInfo", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Sincronizar capítulos", substring = true).assertIsDisplayed()
    }

    @Test
    fun nao_deve_exibir_item_aninhado_de_capitulos_comicinfo_quando_sem_fonte() {
        val remoteInfo = ComicMetadataDto(
            title = "Manga Teste",
            description = "Desc",
            status = "Ongoing",
            syncSource = MetadataSourcePattern.COMIC_INFO,
            sources = ComicSourcesDto(comicInfo = null)
        )

        setContent(remoteInfo = remoteInfo, externalSyncEnabled = false)

        composeTestRule.onNodeWithText("ComicInfo", substring = true).assertIsDisplayed()
    }
}
