package br.acerola.manga.module.reader.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import br.acerola.manga.config.preference.ReadingMode
import br.acerola.manga.module.reader.Reader
import br.acerola.manga.module.reader.gesture.ZoomablePageImage
import br.acerola.manga.module.reader.state.TapArea

@Composable
fun Reader.Component.VerticalPagedReader(
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
    VerticalPager(
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
            orientation = ReadingMode.VERTICAL,
            onZoomStatusChange = onZoomChange,
            onAreaTap = { area ->
                when (area) {
                    TapArea.TOP -> onPrevClick()
                    TapArea.BOTTOM -> onNextClick()
                    TapArea.CENTER -> onUiToggle()
                    else -> {}
                }
            }
        )
    }
}
