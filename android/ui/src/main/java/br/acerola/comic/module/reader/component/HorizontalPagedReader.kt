package br.acerola.comic.module.reader.component
import br.acerola.comic.ui.R

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import br.acerola.comic.config.preference.ReadingMode
import br.acerola.comic.module.reader.Reader
import br.acerola.comic.module.reader.gesture.ZoomablePageImage
import br.acerola.comic.module.reader.state.TapArea

@Composable
fun Reader.Component.HorizontalPagedReader(
    pageCount: Int,
    mangaId: Long,
    chapterId: Long,
    pagerState: PagerState,
    onUiToggle: () -> Unit,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onPageRequest: (Int) -> Unit,
    onZoomChange: (Boolean) -> Unit,
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        key = { it }
    ) { index ->
        LaunchedEffect(index) {
            onPageRequest(index)
        }

        Reader.Gesture.ZoomablePageImage(
            mangaId = mangaId,
            chapterId = chapterId,
            pageIndex = index,
            orientation = ReadingMode.HORIZONTAL,
            onZoomStatusChange = onZoomChange,
            onAreaTap = { area ->
                when (area) {
                    TapArea.LEFT -> onPrevClick()
                    TapArea.RIGHT -> onNextClick()
                    TapArea.CENTER -> onUiToggle()
                    else -> {}
                }
            }
        )
    }
}
