package br.acerola.manga.module.reader.component

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import br.acerola.manga.module.reader.state.ReadingMode

@Composable
fun MangaReaderScaffold(
    title: String,
    chapterSort: String,
    isUiVisible: Boolean,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    bottomControls: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = Color.Black,
        contentColor = Color.White,
        topBar = {
            ReaderTopBar(
                title = title,
                subtitle = "Ordem: $chapterSort",
                isVisible = isUiVisible,
                onBackClick = onBackClick,
                onSettingsClick = onSettingsClick
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isUiVisible,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                bottomControls()
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            content(paddingValues)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopBar(
    title: String,
    subtitle: String,
    isVisible: Boolean,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        CenterAlignedTopAppBar(
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar"
                    )
                }
            },
            actions = {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Configurações"
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
fun ReaderContent(
    readingMode: ReadingMode,
    pageCount: Int,
    pages: Map<Int, ByteArray>,
    pagerState: PagerState,
    listState: LazyListState,
    onPageRequest: (Int) -> Unit,
    onUiToggle: () -> Unit,
    onZoomChange: (Boolean) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (readingMode) {
            ReadingMode.PAGINATED -> {
                HorizontalPagedReader(
                    pageCount = pageCount,
                    pages = pages,
                    pagerState = pagerState,
                    onPageRequest = onPageRequest,
                    onUiToggle = onUiToggle,
                    onZoomChange = onZoomChange
                )
            }

            ReadingMode.VERTICAL -> {
                VerticalPagedReader(
                    pageCount = pageCount,
                    pages = pages,
                    pagerState = pagerState,
                    onPageRequest = onPageRequest,
                    onUiToggle = onUiToggle,
                    onZoomChange = onZoomChange
                )
            }

            ReadingMode.WEBTOON -> {
                WebtoonReader(
                    pageCount = pageCount,
                    pages = pages,
                    listState = listState,
                    onPageRequest = onPageRequest,
                    onUiToggle = onUiToggle,
                    onZoomChange = onZoomChange
                )
            }
        }
    }
}

@Composable
fun HorizontalPagedReader(
    pageCount: Int,
    pages: Map<Int, ByteArray>,
    pagerState: PagerState,
    onPageRequest: (Int) -> Unit,
    onUiToggle: () -> Unit,
    onZoomChange: (Boolean) -> Unit
) {
    var isZoomed by remember { mutableStateOf(false) }

    HorizontalPager(
        state = pagerState,
        userScrollEnabled = !isZoomed,
        modifier = Modifier.fillMaxSize(),
        pageSpacing = 16.dp,
        beyondViewportPageCount = 1
    ) { index ->
        LaunchedEffect(index) {
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
        userScrollEnabled = !isZoomed,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1
    ) { index ->
        LaunchedEffect(index) {
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

@Composable
fun WebtoonReader(
    pageCount: Int,
    pages: Map<Int, ByteArray>,
    listState: LazyListState,
    onPageRequest: (Int) -> Unit,
    onUiToggle: () -> Unit,
    onZoomChange: (Boolean) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Report zoom state to parent (e.g., to hide UI)
    LaunchedEffect(scale) {
        onZoomChange(scale > 1.0f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Custom Gesture Detector to handle Conflict between Scroll and Zoom
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val isMultiTouch = event.changes.size > 1
                        val isZoomed = scale > 1f

                        // If multi-touch OR already zoomed, we take over control
                        if (isMultiTouch || isZoomed) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()

                            if (zoom != 1f || pan != Offset.Zero) {
                                val newScale = (scale * zoom).coerceIn(1f, 3f)

                                // Limit Translation based on scale
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

                                // Consume events to prevent LazyColumn from seeing them
                                event.changes.forEach {
                                    if (it.positionChanged()) it.consume()
                                }
                            }
                        }
                        // Else: Let it pass through to LazyColumn (Single touch, Scale=1)

                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        LazyColumn(
            state = listState,
            // Disable List internal scrolling if we are zoomed in (we handle panning manually)
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
                LaunchedEffect(index) {
                    onPageRequest(index)
                }

                // Webtoon Item: Just an Image, no individual Zoom
                val pageBytes = pages[index]
                val bitmap = remember(pageBytes) {
                    pageBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            // Handle Double Tap to Reset/Zoom
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { onUiToggle() },
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp), // Placeholder height
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(strokeCap = StrokeCap.Round)
                    }
                }
            }
        }
    }
}

@Composable
fun ReaderBottomControls(
    currentPage: Int,
    pageCount: Int,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    enableNavigation: Boolean = true
) {
    Surface(
        modifier = Modifier
            .padding(16.dp)
            .navigationBarsPadding(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Página ${currentPage + 1} / $pageCount",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.weight(1f))

            if (enableNavigation) {
                FilledTonalIconButton(
                    onClick = onPrevClick,
                    enabled = currentPage > 0
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Página Anterior"
                    )
                }

                Button(
                    onClick = onNextClick,
                    enabled = currentPage < pageCount - 1
                ) {
                    Text("Próximo")
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ZoomablePageImage(
    pageBytes: ByteArray?,
    onTap: () -> Unit,
    onZoomStatusChange: (Boolean) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val bitmap = remember(pageBytes) {
        pageBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                            onZoomStatusChange(false)
                        } else {
                            scale = 2f
                            onZoomStatusChange(true)
                        }
                    }
                )
            }
            .pointerInput(Unit) {
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
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentScale = ContentScale.Fit
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