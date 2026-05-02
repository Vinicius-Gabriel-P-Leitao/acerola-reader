package br.acerola.comic.module.comic

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.acerola.comic.common.state.LocalSnackbarHostState
import br.acerola.comic.common.ux.theme.AcerolaTheme
import br.acerola.comic.common.viewmodel.library.archive.ChapterArchiveViewModel
import br.acerola.comic.common.viewmodel.library.archive.ComicDirectoryViewModel
import br.acerola.comic.common.viewmodel.library.metadata.ChapterMetadataViewModel
import br.acerola.comic.common.viewmodel.library.metadata.ComicMetadataViewModel
import br.acerola.comic.config.preference.types.ChapterPageSizeType
import br.acerola.comic.config.preference.types.ChapterSortPreferenceData
import br.acerola.comic.config.preference.types.ChapterSortType
import br.acerola.comic.config.preference.types.SortDirection
import br.acerola.comic.config.preference.types.VolumeViewType
import br.acerola.comic.dto.ComicDto
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.error.UserMessage
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ComicScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val comicViewModel = mockk<ComicViewModel>(relaxed = true)
    private val chapterArchiveVM = mockk<ChapterArchiveViewModel>(relaxed = true)
    private val comicDirVM = mockk<ComicDirectoryViewModel>(relaxed = true)
    private val comicRemoteVM = mockk<ComicMetadataViewModel>(relaxed = true)
    private val chapterRemoteVM = mockk<ChapterMetadataViewModel>(relaxed = true)

    @Before
    fun setUp() {
        val emptyEvents = MutableSharedFlow<UserMessage>().asSharedFlow()

        every { comicViewModel.comic } returns MutableStateFlow(null)
        every { comicViewModel.chapters } returns MutableStateFlow(null)
        every { comicViewModel.chapterIsIndexing } returns MutableStateFlow(false)
        every { comicViewModel.chapterProgress } returns MutableStateFlow(-1)
        every { comicViewModel.comicIsIndexing } returns MutableStateFlow(false)
        every { comicViewModel.comicProgress } returns MutableStateFlow(-1)
        every { comicViewModel.history } returns MutableStateFlow(null)
        every { comicViewModel.readChapters } returns MutableStateFlow(emptyList<String>())
        every { comicViewModel.selectedChapterPerPage } returns MutableStateFlow(ChapterPageSizeType.SHORT)
        every { comicViewModel.uiEvents } returns emptyEvents
        every { comicViewModel.chapterSortSettings } returns
            MutableStateFlow(ChapterSortPreferenceData(ChapterSortType.NUMBER, SortDirection.ASCENDING))
        every { comicViewModel.volumeViewMode } returns MutableStateFlow(VolumeViewType.CHAPTER)
        every { comicViewModel.activeVolumeId } returns MutableStateFlow<Long?>(null)

        every { comicDirVM.uiEvents } returns emptyEvents
        every { chapterArchiveVM.uiEvents } returns emptyEvents

        every { comicRemoteVM.isIndexing } returns MutableStateFlow(false)
        every { comicRemoteVM.uiEvents } returns emptyEvents
        every { comicRemoteVM.allCategories } returns MutableStateFlow(emptyList())

        every { chapterRemoteVM.isIndexing } returns MutableStateFlow(false)
        every { chapterRemoteVM.uiEvents } returns emptyEvents
    }

    @Test
    fun `MangaScreen_deve_exibir_o_titulo_do_comic`() {
        val comic =
            ComicDto(
                directory = ComicDirectoryDto(1L, "Test", "path", null, null, 0L, null, false),
                remoteInfo =
                    ComicMetadataDto(
                        title = "Comic de Teste",
                        description = "Desc",
                        status = "Ongoing",
                    ),
            )

        composeTestRule.setContent {
            AcerolaTheme {
                CompositionLocalProvider(LocalSnackbarHostState provides SnackbarHostState()) {
                    ComicScreen(
                        comic = comic,
                        onBackClick = {},
                        comicViewModel = comicViewModel,
                        chapterArchiveViewModel = chapterArchiveVM,
                        comicDirectoryViewModel = comicDirVM,
                        comicMetadataViewModel = comicRemoteVM,
                        chapterMetadataViewModel = chapterRemoteVM,
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Comic de Teste").assertIsDisplayed()
    }
}
