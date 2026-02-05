package br.acerola.manga.module.reader.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.acerola.manga.module.reader.gesture.ZoomablePageImage
import br.acerola.manga.module.reader.state.ReadingMode
import br.acerola.manga.module.reader.state.TapArea

@Composable
fun HorizontalPagedReader(
    pagerState: PagerState,
    onUiToggle: () -> Unit,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    pages: Map<Int, ByteArray>,
    onPageRequest: (Int) -> Unit,
    onZoomChange: (Boolean) -> Unit,
) {
    var isZoomed by remember { mutableStateOf(value = false) }

    HorizontalPager(
        state = pagerState,
        pageSpacing = 16.dp,
        beyondViewportPageCount = 1,
        userScrollEnabled = !isZoomed,
        modifier = Modifier.fillMaxSize(),
    ) { index ->
        LaunchedEffect(key1 = index) {
            onPageRequest(index)
        }

        ZoomablePageImage(
            pageBytes = pages[index],
            orientation = ReadingMode.HORIZONTAL,
            onAreaTap = { area ->
                when (area) {
                    TapArea.LEFT -> onPrevClick()
                    TapArea.RIGHT -> onNextClick()
                    TapArea.CENTER -> onUiToggle()
                    else -> {} // WARN: Ignora o resto já que não chega
                }
            },
            onZoomStatusChange = { zoomed ->
                isZoomed = zoomed
                onZoomChange(zoomed)
            })
    }
}
