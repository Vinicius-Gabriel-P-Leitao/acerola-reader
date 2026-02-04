package br.acerola.manga.module.reader.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import br.acerola.manga.module.reader.gesture.ZoomablePageImage

@Composable
fun VerticalPagedReader(
    pageCount: Int,
    pages: Map<Int, ByteArray>,
    pagerState: PagerState,
    onPageRequest: (Int) -> Unit,
    onUiToggle: () -> Unit,
    onZoomChange: (Boolean) -> Unit
) {
    var isZoomed by remember { mutableStateOf(false) }

    VerticalPager(
        state = pagerState,
        beyondViewportPageCount = 1,
        userScrollEnabled = !isZoomed,
        modifier = Modifier.fillMaxSize(),
    ) { index ->
        LaunchedEffect(key1 = index) {
            onPageRequest(index)
        }

        ZoomablePageImage(
            pageBytes = pages[index],
            onTap = onUiToggle,
            onZoomStatusChange = { zoomed ->
                isZoomed = zoomed
                onZoomChange(zoomed)
            }
        )
    }
}
