package br.acerola.manga.module.reader.component

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import br.acerola.manga.config.preference.ReadingMode
import br.acerola.manga.module.reader.Reader
import br.acerola.manga.module.reader.gesture.ZoomablePageImage
import br.acerola.manga.module.reader.state.TapArea

@Composable
fun Reader.Component.HorizontalPagedReader(
    pages: Map<Int, Bitmap>,
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
            pageBitmap = pages[index],
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
