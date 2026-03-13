package br.acerola.manga.module.reader

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import br.acerola.manga.config.preference.ReadingMode
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.module.reader.layout.ReaderContent
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    chapter: ChapterFileDto?,
    chapterId: Long = -1L,
    initialPage: Int,
    mangaId: Long,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(chapter, chapterId, mangaId) {
        if (chapter != null) {
            viewModel.openChapter(mangaId, chapter, initialPage)
        } else if (chapterId != -1L) {
            viewModel.loadAndOpenChapter(mangaId, chapterId, initialPage)
        }
    }

    if (state.isLoading || state.pageCount == 0) {
        br.acerola.manga.common.layout.ProgressIndicator(isLoading = true)
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
            val activeChapterId = chapter?.id ?: chapterId
            if (activeChapterId != -1L) {
                viewModel.onCurrentPageChanged(mangaId, activeChapterId, index)
            }
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

    ReaderContent(
        pages = state.pages,
        listState = listState,
        pagerState = pagerState,
        pageCount = state.pageCount,
        readingMode = state.readingMode,
        onUiToggle = { viewModel.toggleUiVisibility() },
        onPageRequest = { index ->
            val activeChapterId = chapter?.id ?: chapterId
            if (activeChapterId != -1L) {
                viewModel.onPageVisible(mangaId, activeChapterId, index)
            }
        },
        onPrevClick = { viewModel.onSliderChanged(index = state.currentPage - 1) },
        onNextClick = { viewModel.onSliderChanged(index = state.currentPage + 1) },
        onZoomChange = {
            /* NOTE: O estado do zoom é gerenciado internamente ou via máquina virtual, se necessário, para bloquear a interface do usuário */
        }
    )
}