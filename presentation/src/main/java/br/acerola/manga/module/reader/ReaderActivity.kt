package br.acerola.manga.module.reader

import android.content.Context
import android.os.Build
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import br.acerola.manga.common.activity.BaseActivity
import br.acerola.manga.common.navigation.Destination
import br.acerola.manga.config.preference.ReadingMode
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.module.reader.layout.ReaderBottomControls
import br.acerola.manga.module.reader.layout.ReaderSettingsSheet
import br.acerola.manga.module.reader.layout.ReaderTopBar
import br.acerola.manga.presentation.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReaderActivity(
    override val startDestinationRes: Int = Destination.READER.route
) : BaseActivity() {
    override val applyScaffoldPadding: Boolean = false
    private val viewModel: ReaderViewModel by viewModels()

    object PageExtra {
        const val PAGE = "PAGE"
        const val MANGA_ID = "MANGA_ID"
        const val CHAPTER_ID = "CHAPTER_ID"
        const val INITIAL_PAGE = "INITIAL_PAGE"
    }

    val page: ChapterFileDto? by lazy {
        val safeIntent = intent ?: return@lazy null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            safeIntent.extras?.getParcelable(PageExtra.PAGE, ChapterFileDto::class.java)
        } else {
            @Suppress("DEPRECATION") safeIntent.extras?.getParcelable(PageExtra.PAGE)
        }
    }

    override fun NavGraphBuilder.setupNavGraph(context: Context, navController: NavHostController) {
        composable(route = context.getString(Destination.READER.route)) {
            val state by viewModel.state.collectAsState()
            val mangaId = intent?.getLongExtra(PageExtra.MANGA_ID, -1L) ?: -1L
            val chapterId = intent?.getLongExtra(PageExtra.CHAPTER_ID, -1L) ?: -1L
            val initialPage = intent?.getIntExtra(PageExtra.INITIAL_PAGE, 0) ?: 0
            
            ReaderScreen(
                chapter = state.currentChapter ?: page,
                chapterId = chapterId,
                mangaId = mangaId,
                viewModel = viewModel,
                initialPage = initialPage
            )
        }
    }

    @Composable
    override fun BottomBar(navController: NavHostController) {
        val state by viewModel.state.collectAsState()
        val mangaId = intent?.getLongExtra(PageExtra.MANGA_ID, -1L) ?: -1L

        AnimatedVisibility(
            visible = state.isUiVisible,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }) {
            ReaderBottomControls(
                pageCount = state.pageCount,
                currentPage = state.currentPage,
                enableNavigation = state.readingMode != ReadingMode.WEBTOON,
                isChapterRead = state.isChapterRead,
                hasNextChapter = state.nextChapterId != null,
                hasPreviousChapter = state.previousChapterId != null,
                isLoading = state.isLoading,
                onPrevClick = { viewModel.onSliderChanged(index = state.currentPage - 1) },
                onNextClick = { viewModel.onSliderChanged(index = state.currentPage + 1) },
                onNextChapterClick = { viewModel.loadNextChapter(mangaId) },
                onPreviousChapterClick = { viewModel.loadPreviousChapter(mangaId) }
            )
        }
    }

    @Composable
    override fun TopBar(navController: NavHostController) {
        val state by viewModel.state.collectAsState()
        var showSettings by remember { mutableStateOf(value = false) }

        val activeChapter = state.currentChapter ?: page

        Box {
            ReaderTopBar(
                title = activeChapter?.name ?: stringResource(id = R.string.label_reader_activity),
                subtitle = stringResource(id = R.string.label_reader_chapter_order, activeChapter?.chapterSort ?: "-"),
                isVisible = state.isUiVisible,
                onBackClick = { finish() },
                onSettingsClick = { showSettings = true })

            if (showSettings) {
                ReaderSettingsSheet(
                    onDismissRequest = { showSettings = false },
                    currentMode = state.readingMode,
                    onModeSelected = { mode ->
                        viewModel.updateReadingMode(mode = mode)
                        showSettings = false
                    }
                )
            }
        }
    }
}
