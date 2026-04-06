package br.acerola.comic.module.reader.layout
import br.acerola.comic.ui.R

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.acerola.comic.config.preference.ReadingMode
import br.acerola.comic.module.reader.Reader
import br.acerola.comic.module.reader.component.HorizontalPagedReader
import br.acerola.comic.module.reader.component.VerticalPagedReader
import br.acerola.comic.module.reader.component.WebtoonReader

@Composable
fun Reader.Layout.PageContent(
    pageCount: Int,
    pagerState: PagerState,
    onUiToggle: () -> Unit,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    readingMode: ReadingMode,
    listState: LazyListState,
    mangaId: Long,
    chapterId: Long,
    onPageRequest: (Int) -> Unit,
    onZoomChange: (Boolean) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (readingMode) {
            ReadingMode.HORIZONTAL -> {
                Reader.Component.HorizontalPagedReader(
                    pageCount = pageCount,
                    mangaId = mangaId,
                    chapterId = chapterId,
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
                    pageCount = pageCount,
                    mangaId = mangaId,
                    chapterId = chapterId,
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
                    mangaId = mangaId,
                    chapterId = chapterId,
                    listState = listState,
                    onPageRequest = onPageRequest,
                    onUiToggle = onUiToggle,
                    onZoomChange = onZoomChange
                )
            }
        }
    }
}
