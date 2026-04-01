package br.acerola.manga.module.download

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import br.acerola.manga.common.mapper.LanguageMapper
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.component.GlassButton
import br.acerola.manga.common.ux.component.Pagination
import br.acerola.manga.common.ux.component.SnackbarVariant
import br.acerola.manga.common.ux.component.showSnackbar
import br.acerola.manga.common.ux.layout.LanguageSelector
import br.acerola.manga.common.ux.layout.TopBar
import br.acerola.manga.common.ux.theme.local.LocalSnackbarHostState
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.module.download.component.ChapterDownloadItem
import br.acerola.manga.module.download.component.DownloadSelectionBar
import br.acerola.manga.module.download.state.DownloadAction
import br.acerola.manga.module.download.state.DownloadUiState
import br.acerola.manga.module.main.search.component.DownloadQueueComponent
import br.acerola.manga.ui.R
import coil.compose.AsyncImage

@Composable
fun Download.Layout.DownloadScreen(
    onBack: () -> Unit,
    manga: MangaMetadataDto,
    viewModel: DownloadViewModel = hiltViewModel(),
) {

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current

    LaunchedEffect(manga.sources?.mangadex?.mangadexId) {
        viewModel.init(manga)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { message ->
            snackbarHostState.showSnackbar(message.uiMessage.asString(context), SnackbarVariant.Error)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            bottomBar = {
                Download.Component.DownloadSelectionBar(
                    uiState = uiState,
                    onAction = viewModel::onAction
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                item(key = "header", contentType = "header") {
                    MangaDownloadHeader(manga = manga)
                }

                item(key = "active_download", contentType = "active_download") {
                    DownloadQueueComponent(
                        queue = listOfNotNull(uiState.activeDownload),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item(key = "chapters_bar", contentType = "bar") {
                    ChaptersSelectionBar(uiState = uiState, onAction = viewModel::onAction)
                }

                when {
                    uiState.isLoadingChapters -> {
                        item(key = "loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    uiState.chapters.isEmpty() -> {
                        item(key = "empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.label_search_no_results),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    else -> {
                        items(
                            items = uiState.chapters,
                            key = { it.id },
                            contentType = { "chapter" }
                        ) { chapter ->
                            Download.Component.ChapterDownloadItem(
                                chapter = chapter,
                                isSelected = chapter.id in uiState.selectedChapterIds,
                                isDownloading = chapter.id == uiState.activeDownload?.currentChapterId,
                                onClick = { viewModel.onAction(DownloadAction.ToggleChapter(chapter.id)) }
                            )
                        }

                        if (uiState.totalPages > 1) {
                            item(key = "pagination", contentType = "pagination") {
                                Acerola.Component.Pagination(
                                    currentPage = uiState.currentPage,
                                    totalPages = uiState.totalPages,
                                    onPageChange = { viewModel.onAction(DownloadAction.ChangePage(it)) }
                                )
                            }
                        }
                    }
                }

                item(key = "spacer") {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Acerola.Layout.TopBar(
            navigationIcon = {
                Acerola.Component.GlassButton(
                    onClick = onBack,
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.label_search_back_to_results),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
            }
        )
    }
}

@Composable
private fun MangaDownloadHeader(manga: MangaMetadataDto) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            AsyncImage(
                model = manga.cover?.url,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .blur(radius = 20.dp)
                    .align(Alignment.TopCenter),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                AsyncImage(
                    model = manga.cover?.url,
                    contentDescription = manga.title,
                    modifier = Modifier
                        .width(110.dp)
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(140.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = manga.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    val authorName = manga.authors?.name
                    if (!authorName.isNullOrBlank()) {
                        Text(
                            text = authorName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (manga.status.isNotBlank()) {
                        StatusBadge(status = manga.status)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChaptersSelectionBar(
    uiState: DownloadUiState,
    onAction: (DownloadAction) -> Unit,
) {
    Acerola.Layout.LanguageSelector(
        selectedLanguage = uiState.selectedLanguage,
        onLanguageSelected = { onAction(DownloadAction.SelectLanguage(it)) },
        trigger = { onClick ->
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.label_search_chapters, uiState.totalChapters),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(3.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }

                    TextButton(onClick = onClick) {
                        Text(
                            text = if (uiState.selectedLanguage != null) stringResource(id = LanguageMapper.getLabelRes(uiState.selectedLanguage)) else "",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    )
}



@Composable
private fun StatusBadge(status: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
