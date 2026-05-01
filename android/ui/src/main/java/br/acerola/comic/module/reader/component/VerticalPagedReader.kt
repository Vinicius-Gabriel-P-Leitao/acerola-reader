package br.acerola.comic.module.reader.component
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import br.acerola.comic.config.preference.types.ReadingMode
import br.acerola.comic.module.reader.Reader
import br.acerola.comic.module.reader.gesture.ZoomablePageImage
import br.acerola.comic.module.reader.state.TapArea

@Composable
fun Reader.Component.VerticalPagedReader(
    comicId: Long,
    chapterId: Long?,
    pagerState: PagerState,
    onUiToggle: () -> Unit,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onZoomChange: (Boolean) -> Unit,
) {
    var isZoomed by remember { mutableStateOf(false) }

    VerticalPager(
        key = { it },
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = !isZoomed,
    ) { index ->
        Reader.Gesture.ZoomablePageImage(
            comicId = comicId,
            chapterId = chapterId,
            pageIndex = index,
            orientation = ReadingMode.VERTICAL,
            onZoomStatusChange = { zoomed ->
                onZoomChange(zoomed)
            },
            onAreaTap = { area ->
                when (area) {
                    TapArea.TOP -> onPrevClick()
                    TapArea.BOTTOM -> onNextClick()
                    TapArea.CENTER -> onUiToggle()
                    else -> {}
                }
            },
        )
    }
}
