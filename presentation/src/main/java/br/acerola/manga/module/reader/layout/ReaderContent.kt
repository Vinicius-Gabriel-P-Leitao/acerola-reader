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
    readingMode: ReadingMode,
    pagerState: PagerState,
    pages: Map<Int, ByteArray>,
    listState: LazyListState,
    onPageRequest: (Int) -> Unit,
    onUiToggle: () -> Unit,
    onZoomChange: (Boolean) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (readingMode) {
            ReadingMode.PAGINATED -> {
                HorizontalPagedReader(
                    pages = pages,
                    pagerState = pagerState,
                    onPageRequest = onPageRequest,
                    onUiToggle = onUiToggle,
                    onZoomChange = onZoomChange
                )
            }

            ReadingMode.VERTICAL -> {
                VerticalPagedReader(
                    pageCount = pageCount,
                    pages = pages,
                    pagerState = pagerState,
                    onPageRequest = onPageRequest,
                    onUiToggle = onUiToggle,
                    onZoomChange = onZoomChange
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
