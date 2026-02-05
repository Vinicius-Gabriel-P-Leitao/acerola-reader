package br.acerola.manga.module.reader.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.acerola.manga.module.reader.component.HorizontalPagedReader
import br.acerola.manga.module.reader.component.VerticalPagedReader
import br.acerola.manga.module.reader.component.WebtoonReader
import br.acerola.manga.module.reader.state.ReadingMode

@Composable
fun ReaderContent(
    pageCount: Int,
    pagerState: PagerState,
    onUiToggle: () -> Unit,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    readingMode: ReadingMode,
    listState: LazyListState,
    pages: Map<Int, ByteArray>,
    onPageRequest: (Int) -> Unit,
    onZoomChange: (Boolean) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (readingMode) {
            ReadingMode.HORIZONTAL -> {
                HorizontalPagedReader(
                    pages = pages,
                    onUiToggle = onUiToggle,
                    pagerState = pagerState,
                    onPrevClick = onPrevClick,
                    onNextClick = onNextClick,
                    onZoomChange = onZoomChange,
                    onPageRequest = onPageRequest,
                )
            }

            ReadingMode.VERTICAL -> {
                VerticalPagedReader(
                    pages = pages,
                    onUiToggle = onUiToggle,
                    pagerState = pagerState,
                    onPrevClick = onPrevClick,
                    onNextClick = onNextClick,
                    onZoomChange = onZoomChange,
                    onPageRequest = onPageRequest,
                )
            }

            ReadingMode.WEBTOON -> {
                WebtoonReader(
                    pageCount = pageCount,
                    pages = pages,
                    listState = listState,
                    onPageRequest = onPageRequest,
                    onUiToggle = onUiToggle,
                    onZoomChange = onZoomChange
                )
            }
        }
    }
}
