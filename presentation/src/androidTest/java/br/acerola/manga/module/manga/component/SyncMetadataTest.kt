package br.acerola.manga.module.manga.component

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import br.acerola.manga.common.theme.AcerolaTheme
import br.acerola.manga.common.viewmodel.library.metadata.ChapterRemoteInfoViewModel
import br.acerola.manga.common.viewmodel.library.metadata.MangaRemoteInfoViewModel
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.local.database.entity.metadata.MetadataSource
import br.acerola.manga.presentation.R
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class SyncMetadataTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val mangaRemoteInfoVM = mockk<MangaRemoteInfoViewModel>(relaxed = true)
    private val chapterRemoteInfoVM = mockk<ChapterRemoteInfoViewModel>(relaxed = true)

    @Test
    fun `SyncMetadata_deve_renderizar_as_seções_de_MangaDex_e_Arquivo_Local_corretamente`() {
        val directory = MangaDirectoryDto(
            id = 1, name = "Test", path = "", coverUri = null, bannerUri = null, 
            lastModified = 0, chapterTemplate = null, hasComicInfo = false
        )
        val remoteInfo = MangaRemoteInfoDto(
            mirrorId = "1", title = "Test", description = "", status = "ongoing", 
            metadataSource = MetadataSource.MANGADEX, id = 10L
        )

        composeTestRule.setContent {
            AcerolaTheme {
                SyncMetadata(
                    directory = directory,
                    remoteInfo = remoteInfo,
                    mangaRemoteInfoViewModel = mangaRemoteInfoVM,
                    chapterRemoteInfoViewModel = chapterRemoteInfoVM
                )
            }
        }

        // Valida cabeçalhos de seções
        composeTestRule.onNodeWithText(context.getString(R.string.label_mangadex_group)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.label_local_file_group)).assertIsDisplayed()

        // Valida ações principais
        composeTestRule.onNodeWithText(context.getString(R.string.title_sync_mangadex_remote_info)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.title_sync_comic_info)).assertIsDisplayed()
    }
}
