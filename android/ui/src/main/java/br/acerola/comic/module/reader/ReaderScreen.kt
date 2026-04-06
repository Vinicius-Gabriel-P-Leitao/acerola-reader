package br.acerola.comic.module.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.layout.ProgressIndicator
import br.acerola.comic.common.ux.component.SnackbarVariant
import br.acerola.comic.common.ux.component.showSnackbar
import br.acerola.comic.common.ux.theme.local.LocalSnackbarHostState
import br.acerola.comic.config.preference.ReadingMode
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.module.reader.layout.BottomControls
import br.acerola.comic.module.reader.layout.PageContent
import br.acerola.comic.module.reader.layout.SettingsSheet
import br.acerola.comic.module.reader.layout.TopBar
import br.acerola.comic.module.reader.state.ReaderAction
import br.acerola.comic.ui.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun ReaderScreen(
    chapter: ChapterFileDto?,
    chapterId: Long = -1L,
    initialPage: Int,
    mangaId: Long,
    onBackClick: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    val state by viewModel.state.collectAsState()
    val snackbarHostState = LocalSnackbarHostState.current

    LaunchedEffect(chapter, chapterId, mangaId) {
        if (chapter != null) {
            viewModel.openChapter(mangaId, chapter, initialPage)
        } else if (chapterId != -1L) {
            viewModel.loadAndOpenChapter(mangaId, chapterId, initialPage)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { message ->
            snackbarHostState.showSnackbar(message.uiMessage.asString(context), SnackbarVariant.Error)
        }
    }

    val onAction: (ReaderAction) -> Unit = { action ->
        when (action) {
            ReaderAction.NavigateBack -> onBackClick()
            ReaderAction.ToggleUi -> viewModel.toggleUiVisibility()
            is ReaderAction.UpdateReadingMode -> viewModel.updateReadingMode(action.mode)
            is ReaderAction.ChangePage -> viewModel.onSliderChanged(action.index)
            ReaderAction.LoadNextChapter -> viewModel.loadNextChapter(mangaId)
            ReaderAction.LoadPreviousChapter -> viewModel.loadPreviousChapter(mangaId)
            is ReaderAction.PageVisible -> viewModel.onPageVisible(mangaId, state.currentChapter?.id ?: chapterId, action.index)
            is ReaderAction.CurrentPageChanged -> viewModel.onCurrentPageChanged(mangaId, state.currentChapter?.id ?: chapterId, action.index)
        }
    }

    if (state.isLoading || state.pageCount == 0) {
        Acerola.Layout.ProgressIndicator(isLoading = true)
        return
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, (state.pageCount - 1).coerceAtLeast(0)),
        pageCount = { state.pageCount }
    )
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialPage.coerceIn(0, (state.pageCount - 1).coerceAtLeast(0))
    )

    LaunchedEffect(pagerState, listState, state.readingMode, mangaId, chapter, chapterId) {
        snapshotFlow {
            if (state.readingMode == ReadingMode.WEBTOON) {
                listState.firstVisibleItemIndex
            } else {
                pagerState.currentPage
            }
        }.distinctUntilChanged().collectLatest { index ->
            onAction(ReaderAction.CurrentPageChanged(index))
        }
    }

    LaunchedEffect(key1 = state.currentPage) {
        if (state.readingMode == ReadingMode.WEBTOON) {
            if (listState.firstVisibleItemIndex != state.currentPage) {
                listState.scrollToItem(index = state.currentPage)
            }
        } else {
            if (pagerState.currentPage != state.currentPage) {
                pagerState.animateScrollToPage(state.currentPage)
            }
        }
    }

    var showSettings by remember { mutableStateOf(value = false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Reader.Layout.PageContent(
            mangaId = mangaId,
            chapterId = state.currentChapter?.id ?: chapterId,
            listState = listState,
            pagerState = pagerState,
            pageCount = state.pageCount,
            readingMode = state.readingMode,
            onUiToggle = { onAction(ReaderAction.ToggleUi) },
            onPageRequest = { index -> onAction(ReaderAction.PageVisible(index)) },
            onPrevClick = { onAction(ReaderAction.ChangePage(state.currentPage - 1)) },
            onNextClick = { onAction(ReaderAction.ChangePage(state.currentPage + 1)) },
            onZoomChange = {}
        )

        // UI Overlay
        val activeChapter = state.currentChapter ?: chapter
        
        Reader.Layout.TopBar(
            title = activeChapter?.name ?: stringResource(id = R.string.label_reader_activity),
            subtitle = stringResource(id = R.string.label_reader_chapter_order, activeChapter?.chapterSort ?: "-"),
            isVisible = state.isUiVisible,
            onBackClick = { onAction(ReaderAction.NavigateBack) },
            onSettingsClick = { showSettings = true }
        )

        AnimatedVisibility(
            visible = state.isUiVisible,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.fillMaxSize()
        ) {
            Box(contentAlignment = Alignment.BottomCenter) {
                Reader.Layout.BottomControls(
                    isLoading = state.isLoading,
                    pageCount = state.pageCount,
                    currentPage = state.currentPage,
                    isChapterRead = state.isChapterRead,
                    hasNextChapter = state.nextChapterId != null,
                    hasPreviousChapter = state.previousChapterId != null,
                    enableNavigation = state.readingMode != ReadingMode.WEBTOON,
                    onNextChapterClick = { onAction(ReaderAction.LoadNextChapter) },
                    onPrevClick = { onAction(ReaderAction.ChangePage(state.currentPage - 1)) },
                    onNextClick = { onAction(ReaderAction.ChangePage(state.currentPage + 1)) },
                    onPreviousChapterClick = { onAction(ReaderAction.LoadPreviousChapter) }
                )
            }
        }

        if (showSettings) {
            Reader.Layout.SettingsSheet(
                onDismissRequest = { showSettings = false },
                currentMode = state.readingMode,
                onModeSelected = { mode ->
                    onAction(ReaderAction.UpdateReadingMode(mode))
                    showSettings = false
                }
            )
        }
    }
}
