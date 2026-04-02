package br.acerola.manga.module.manga

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.manga.common.ux.theme.AcerolaTheme
import br.acerola.manga.common.ux.theme.local.LocalSnackbarHostState
import br.acerola.manga.common.viewmodel.library.archive.ChapterArchiveViewModel
import br.acerola.manga.common.viewmodel.library.archive.MangaDirectoryViewModel
import br.acerola.manga.common.viewmodel.library.metadata.ChapterMetadataViewModel
import br.acerola.manga.common.viewmodel.library.metadata.MangaMetadataViewModel
import br.acerola.manga.config.preference.ChapterPageSizeType
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.error.UserMessage
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

import br.acerola.manga.config.preference.ChapterSortPreferenceData
import br.acerola.manga.config.preference.ChapterSortType
import br.acerola.manga.config.preference.SortDirection

class MangaScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mangaViewModel = mockk<MangaViewModel>(relaxed = true)
    private val chapterArchiveVM = mockk<ChapterArchiveViewModel>(relaxed = true)
    private val mangaDirVM = mockk<MangaDirectoryViewModel>(relaxed = true)
    private val mangaRemoteVM = mockk<MangaMetadataViewModel>(relaxed = true)
    private val chapterRemoteVM = mockk<ChapterMetadataViewModel>(relaxed = true)

    @Before
    fun setUp() {
        val emptyEvents = MutableSharedFlow<UserMessage>().asSharedFlow()
        
        every { mangaViewModel.manga } returns MutableStateFlow(null)
        every { mangaViewModel.chapters } returns MutableStateFlow(null)
        every { mangaViewModel.chapterIsIndexing } returns MutableStateFlow(false)
        every { mangaViewModel.chapterProgress } returns MutableStateFlow(-1)
        every { mangaViewModel.mangaIsIndexing } returns MutableStateFlow(false)
        every { mangaViewModel.mangaProgress } returns MutableStateFlow(-1)
        every { mangaViewModel.history } returns MutableStateFlow(null)
        every { mangaViewModel.readChapters } returns MutableStateFlow(emptyList<Long>())
        every { mangaViewModel.selectedChapterPerPage } returns MutableStateFlow(ChapterPageSizeType.SHORT)
        every { mangaViewModel.uiEvents } returns emptyEvents
        every { mangaViewModel.chapterSortSettings } returns MutableStateFlow(ChapterSortPreferenceData(ChapterSortType.NUMBER, SortDirection.ASCENDING))

        every { mangaDirVM.uiEvents } returns emptyEvents
        every { chapterArchiveVM.uiEvents } returns emptyEvents
        
        every { mangaRemoteVM.isIndexing } returns MutableStateFlow(false)
        every { mangaRemoteVM.uiEvents } returns emptyEvents
        every { mangaRemoteVM.allCategories } returns MutableStateFlow(emptyList())
        
        every { chapterRemoteVM.isIndexing } returns MutableStateFlow(false)
        every { chapterRemoteVM.uiEvents } returns emptyEvents
    }

    @Test
    fun `MangaScreen_deve_exibir_o_titulo_do_manga`() {
        val manga = MangaDto(
            directory = MangaDirectoryDto(1L, "Test", "path", null, null, 0L, null, false),
            remoteInfo = MangaMetadataDto(
                title = "Manga de Teste", description = "Desc", status = "Ongoing"
            )
        )

        composeTestRule.setContent {
            AcerolaTheme {
                CompositionLocalProvider(LocalSnackbarHostState provides SnackbarHostState()) {
                    MangaScreen(
                        manga = manga,
                        onBackClick = {},
                        mangaViewModel = mangaViewModel,
                        chapterArchiveViewModel = chapterArchiveVM,
                        mangaDirectoryViewModel = mangaDirVM,
                        mangaMetadataViewModel = mangaRemoteVM,
                        chapterMetadataViewModel = chapterRemoteVM
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Manga de Teste").assertIsDisplayed()
    }
}
