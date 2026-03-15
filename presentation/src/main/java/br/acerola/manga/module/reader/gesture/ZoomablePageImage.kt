package br.acerola.manga.module.reader.gesture

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import br.acerola.manga.config.preference.ReadingMode
import br.acerola.manga.module.reader.Reader
import br.acerola.manga.module.reader.state.TapArea

@Composable
fun Reader.Gesture.ZoomablePageImage(
    pageBitmap: Bitmap?,
    onAreaTap: (TapArea) -> Unit,
    onZoomStatusChange: (Boolean) -> Unit,
    orientation: ReadingMode = ReadingMode.VERTICAL,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val bitmap = remember(pageBitmap) {
        pageBitmap?.asImageBitmap()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        val w = size.width
                        val h = size.height
                        val x = tapOffset.x
                        val y = tapOffset.y

                        val area = when (orientation) {
                            ReadingMode.VERTICAL -> when {
                                y < h * 0.25f -> TapArea.TOP
                                y > h * 0.75f -> TapArea.BOTTOM
                                else -> TapArea.CENTER
                            }
                            ReadingMode.HORIZONTAL -> when {
                                x < w * 0.25f -> TapArea.LEFT
                                x > w * 0.75f -> TapArea.RIGHT
                                else -> TapArea.CENTER
                            }
                            ReadingMode.WEBTOON -> TapArea.CENTER
                        }
                        onAreaTap(area)
                    },
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                            onZoomStatusChange(false)
                        } else {
                            scale = 2.5f
                            onZoomStatusChange(true)
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()

                        if (zoom != 1f || scale > 1f) {
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            
                            val extraWidth = (newScale - 1) * containerSize.width
                            val extraHeight = (newScale - 1) * containerSize.height
                            val maxX = extraWidth / 2
                            val maxY = extraHeight / 2

                            val newOffset = if (newScale > 1f) {
                                Offset(
                                    x = (offset.x + pan.x * scale).coerceIn(-maxX, maxX),
                                    y = (offset.y + pan.y * scale).coerceIn(-maxY, maxY)
                                )
                            } else {
                                Offset.Zero
                            }

                            scale = newScale
                            offset = newOffset
                            onZoomStatusChange(scale > 1.05f)
                            event.changes.forEach { change ->
                                if (change.positionChanged()) {
                                    change.consume()
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
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
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(strokeCap = StrokeCap.Round)
            }
        }
    }
}
