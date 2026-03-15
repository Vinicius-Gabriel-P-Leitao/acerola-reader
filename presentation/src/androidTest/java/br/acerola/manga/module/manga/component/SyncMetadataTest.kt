package br.acerola.manga.module.manga.component

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.manga.common.ux.theme.AcerolaTheme
import br.acerola.manga.common.viewmodel.library.metadata.ChapterRemoteInfoViewModel
import br.acerola.manga.common.viewmodel.library.metadata.MangaRemoteInfoViewModel
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.module.manga.Manga
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class SyncMetadataTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mangaRemoteInfoViewModel = mockk<MangaRemoteInfoViewModel>(relaxed = true)
    private val chapterRemoteInfoViewModel = mockk<ChapterRemoteInfoViewModel>(relaxed = true)

    @Test
    fun `SyncMetadata_deve_exibir_as_seções_de_MangaDex_e_Arquivo_Local`() {
        val directory = MangaDirectoryDto(1, "Teste", "", null, null, 0, null)
        val remoteInfo = MangaRemoteInfoDto("1", "Manga Teste", "", "")

        composeTestRule.setContent {
            AcerolaTheme {
                Manga.Component.SyncMetadata(
                    directory = directory,
                    remoteInfo = remoteInfo,
                    mangaRemoteInfoViewModel = mangaRemoteInfoViewModel,
                    chapterRemoteInfoViewModel = chapterRemoteInfoViewModel
                )
            }
        }

        // Verifica se os cabeçalhos de grupo aparecem
        composeTestRule.onNodeWithText("MANGADEX", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("ARQUIVO LOCAL", substring = true).assertIsDisplayed()
    }
}
