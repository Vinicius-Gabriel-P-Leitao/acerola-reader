package br.acerola.manga.module.reader.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun WebtoonReader(
    pageCount: Int,
    onUiToggle: () -> Unit,
    listState: LazyListState,
    pages: Map<Int, Bitmap>,
    onPageRequest: (Int) -> Unit,
    onZoomChange: (Boolean) -> Unit
) {
    var scale by remember { mutableFloatStateOf(value = 1f) }
    var offset by remember { mutableStateOf(value = Offset.Zero) }

    // NOTE: Reporta o estado do zoom ao elemento pai (por exemplo, para ocultar a interface do usuário)
    LaunchedEffect(key1 = scale) {
        onZoomChange(scale > 1.0f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // NOTE: Gestos personalizados.
            .pointerInput(key1 = Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val isZoomed = scale > 1f
                        val event = awaitPointerEvent()
                        val isMultiTouch = event.changes.size > 1

                        // Se o dispositivo estiver multitoque OU já tiver aplicado zoom
                        if (isMultiTouch || isZoomed) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()

                            if (zoom != 1f || pan != Offset.Zero) {
                                val newScale = (scale * zoom).coerceIn(1f, 3f)

                                val maxTranslateX = (newScale - 1) * size.width / 2
                                val maxTranslateY = (newScale - 1) * size.height / 2

                                val newOffset = if (newScale > 1f) {
                                    Offset(
                                        x = (offset.x + pan.x * newScale).coerceIn(-maxTranslateX, maxTranslateX),
                                        y = (offset.y + pan.y * newScale).coerceIn(-maxTranslateY, maxTranslateY)
                                    )
                                } else {
                                    Offset.Zero
                                }

                                scale = newScale
                                offset = newOffset

                                // Consumir eventos para impedir que LazyColumn os veja
                                event.changes.forEach {
                                    if (it.positionChanged()) it.consume()
                                }
                            }
                        }
                        // Else: Deixe passar para LazyColumn (Toque único, Escala=1)

                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        LazyColumn(
            state = listState,
            // NOTE:  Desativar a rolagem interna da lista quando o zoom estiver aplicado (o deslocamento é  controlado manualmente).
            userScrollEnabled = scale == 1f,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        ) {
            items(pageCount) { index ->
                LaunchedEffect(key1 = index) {
                    onPageRequest(index)
                }

                // NOTE: Imagem da Webtoon: Apenas uma imagem, sem zoom individual.
                val pageBitmap = pages[index]
                val bitmap = remember(key1 = pageBitmap) {
                    pageBitmap?.asImageBitmap()
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            // NOTE: Toque duas vezes na imagem para zoom e zoom-out
                            .pointerInput(key1 = Unit) {
                                detectTapGestures(
                                    onTap = {
                                        onUiToggle()
                                    },
                                    onDoubleTap = {
                                        if (scale > 1f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                        } else {
                                            scale = 2f
                                        }
                                    }
                                )
                            },
                        contentScale = ContentScale.FillWidth
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(height = 300.dp),
                    ) {
                        CircularProgressIndicator(strokeCap = StrokeCap.Round)
                    }
                }
            }
        }
    }
}
