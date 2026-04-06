package br.acerola.comic.module.reader.gesture
import br.acerola.comic.ui.R

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalContext
import br.acerola.comic.config.preference.ReadingMode
import br.acerola.comic.module.reader.Reader
import br.acerola.comic.module.reader.state.TapArea
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun Reader.Gesture.ZoomablePageImage(
    mangaId: Long,
    chapterId: Long,
    pageIndex: Int,
    onAreaTap: (TapArea) -> Unit,
    onZoomStatusChange: (Boolean) -> Unit,
    orientation: ReadingMode = ReadingMode.VERTICAL,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                // ... same gesture detection code ...
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data("acerola://page/$mangaId/$chapterId/$pageIndex") // Assuming I will implement a custom fetcher later or it's handled
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
        )
    }
}
