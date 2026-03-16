package br.acerola.manga.module.reader.layout

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.acerola.manga.config.preference.ReadingMode
import br.acerola.manga.module.reader.Reader
import br.acerola.manga.module.reader.component.HorizontalPagedReader
import br.acerola.manga.module.reader.component.VerticalPagedReader
import br.acerola.manga.module.reader.component.WebtoonReader

@Composable
fun Reader.Layout.PageContent(
    pageCount: Int,
    pagerState: PagerState,
    onUiToggle: () -> Unit,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    readingMode: ReadingMode,
    listState: LazyListState,
    pages: Map<Int, Bitmap>,
    onPageRequest: (Int) -> Unit,
    onZoomChange: (Boolean) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (readingMode) {
            ReadingMode.HORIZONTAL -> {
                Reader.Component.HorizontalPagedReader(
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
                Reader.Component.VerticalPagedReader(
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
                Reader.Component.WebtoonReader(
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
