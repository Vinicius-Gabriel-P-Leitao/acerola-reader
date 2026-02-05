package br.acerola.manga.module.reader

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.module.reader.layout.ReaderContent
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

    LaunchedEffect(key1 = pagerState, key2 = listState, key3 = state.readingMode) {
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
        onPageRequest = { index -> viewModel.onPageVisible(index) },
        onPrevClick = { viewModel.onSliderChanged(index = state.currentPage - 1) },
        onNextClick = { viewModel.onSliderChanged(index = state.currentPage + 1) },
        onZoomChange = {
            /* NOTE: O estado do zoom é gerenciado internamente ou via máquina virtual, se necessário, para bloquear a interface do usuário */
        }
    )
}