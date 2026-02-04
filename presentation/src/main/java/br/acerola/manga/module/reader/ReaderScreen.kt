package br.acerola.manga.module.reader

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.module.reader.component.ReaderContent
import br.acerola.manga.module.reader.state.ReadingMode
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    chapter: ChapterFileDto?
) {
    val state by viewModel.state.collectAsState()
    val pagerState = rememberPagerState(pageCount = { state.pageCount })
    val listState = rememberLazyListState()

    LaunchedEffect(chapter) {
        chapter?.let { viewModel.openChapter(it) }
    }

    LaunchedEffect(pagerState, listState, state.readingMode) {
        snapshotFlow {
            if (state.readingMode == ReadingMode.WEBTOON) {
                listState.firstVisibleItemIndex
            } else {
                pagerState.currentPage
            }
        }.distinctUntilChanged().collectLatest { index ->
            viewModel.onCurrentPageChanged(index)
        }
    }

    LaunchedEffect(state.currentPage) {
        if (state.readingMode == ReadingMode.WEBTOON) {
            if (listState.firstVisibleItemIndex != state.currentPage) {
                listState.scrollToItem(state.currentPage)
            }
        } else {
            if (pagerState.currentPage != state.currentPage) {
                pagerState.animateScrollToPage(state.currentPage)
            }
        }
    }

    ReaderContent(
        readingMode = state.readingMode,
        pageCount = state.pageCount,
        pages = state.pages,
        pagerState = pagerState,
        listState = listState,
        onPageRequest = { index -> viewModel.onPageVisible(index) },
        onUiToggle = { viewModel.toggleUiVisibility() },
        onZoomChange = { /* Zoom state handled internally or via VM if needed to lock UI */ }
    )
}