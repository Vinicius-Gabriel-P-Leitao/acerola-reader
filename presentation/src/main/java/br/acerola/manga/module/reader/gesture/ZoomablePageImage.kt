package br.acerola.manga.module.reader.gesture

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import br.acerola.manga.config.preference.ReadingMode
import br.acerola.manga.module.reader.state.TapArea

@Composable
fun ZoomablePageImage(
    pageBitmap: Bitmap?,
    onAreaTap: (TapArea) -> Unit,
    onZoomStatusChange: (Boolean) -> Unit,
    orientation: ReadingMode = ReadingMode.VERTICAL,
) {
    var scale by remember { mutableFloatStateOf(value = 1f) }
    var offset by remember { mutableStateOf(value = Offset.Zero) }

    val bitmap = remember(key1 = pageBitmap) {
        pageBitmap?.asImageBitmap()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(key1 = Unit) {
                detectTapGestures(onTap = { offset ->
                    val w = size.width
                    val h = size.height
                    val x = offset.x
                    val y = offset.y

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

                        // WARN: Só para satisfazer o when, sem já que o webtoon usa um consumer proprio
                        ReadingMode.WEBTOON -> TapArea.CENTER
                    }

                    onAreaTap(area)
                }, onDoubleTap = {
                    if (scale > 1f) {
                        scale = 1f
                        offset = Offset.Zero
                        onZoomStatusChange(false)
                    } else {
                        scale = 2f
                        onZoomStatusChange(true)
                    }
                })
            }
            .pointerInput(key1 = Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)

                    if (newScale > 1f) {
                        val maxTranslateX = (newScale - 1) * 1000
                        val maxTranslateY = (newScale - 1) * 1000

                        offset = Offset(
                            x = (offset.x + pan.x).coerceIn(-maxTranslateX, maxTranslateX),
                            y = (offset.y + pan.y).coerceIn(-maxTranslateY, maxTranslateY)
                        )
                    } else {
                        offset = Offset.Zero
                    }

                    scale = newScale
                    onZoomStatusChange(scale > 1.05f)
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
