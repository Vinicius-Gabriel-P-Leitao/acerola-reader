package br.acerola.manga.module.main.search.layout

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.component.Pagination
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.module.main.Main
import br.acerola.manga.module.main.search.component.ChapterDownloadItem
import br.acerola.manga.module.main.search.component.DownloadActionBar
import br.acerola.manga.module.main.search.state.SearchAction
import br.acerola.manga.module.main.search.state.SearchUiState
import br.acerola.manga.presentation.R
import coil.compose.AsyncImage

private val languageNames = mapOf(
    "pt-br" to "Português (BR)",
    "en" to "English",
    "es-la" to "Español (LA)",
    "es" to "Español",
    "fr" to "Français",
    "it" to "Italiano",
    "de" to "Deutsch",
    "ru" to "Русский",
    "ja" to "日本語",
    "ko" to "한국어",
    "zh" to "中文",
    "id" to "Indonesia"
)

@Composable
fun Main.Search.Layout.ChapterSelectionLayout(
    uiState: SearchUiState,
    onAction: (SearchAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val manga = uiState.selectedManga ?: return

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Main.Search.Component.DownloadActionBar(uiState = uiState, onAction = onAction)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            item(key = "header", contentType = "header") {
                MangaDownloadHeader(
                    manga = manga,
                    onBack = { onAction(SearchAction.BackToSearch) }
                )
            }

            item(key = "chapters_bar", contentType = "bar") {
                ChaptersSelectionBar(uiState = uiState, onAction = onAction)
            }

            when {
                uiState.isLoadingChapters -> {
                    item(key = "loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
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
                                .height(200.dp),
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
                        Main.Search.Component.ChapterDownloadItem(
                            chapter = chapter,
                            isSelected = chapter.id in uiState.selectedChapterIds,
                            onClick = { onAction(SearchAction.ToggleChapter(chapter.id)) }
                        )
                    }

                    if (uiState.totalPages > 1) {
                        item(key = "pagination", contentType = "pagination") {
                            Acerola.Component.Pagination(
                                currentPage = uiState.currentPage,
                                totalPages = uiState.totalPages,
                                onPageChange = { onAction(SearchAction.ChangePage(it)) }
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
}

@Composable
private fun MangaDownloadHeader(
    manga: MangaRemoteInfoDto,
    onBack: () -> Unit,
) {
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

            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.label_search_back_to_results),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

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
    uiState: SearchUiState,
    onAction: (SearchAction) -> Unit,
) {
    var languageMenuExpanded by remember { mutableStateOf(false) }
    val availableLanguages = languageNames.keys.toList()

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
                    text = stringResource(R.string.label_search_chapters) + " (${uiState.totalChapters})",
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    TextButton(onClick = { languageMenuExpanded = true }) {
                        Text(
                            text = languageNames[uiState.selectedLanguage] ?: uiState.selectedLanguage,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    DropdownMenu(
                        expanded = languageMenuExpanded,
                        onDismissRequest = { languageMenuExpanded = false }
                    ) {
                        availableLanguages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(languageNames[lang] ?: lang) },
                                onClick = {
                                    languageMenuExpanded = false
                                    onAction(SearchAction.SelectLanguage(lang))
                                }
                            )
                        }
                    }
                }

                TextButton(onClick = { onAction(SearchAction.SelectAll) }) {
                    Text(
                        text = stringResource(R.string.label_search_select_all),
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                TextButton(onClick = { onAction(SearchAction.DeselectAll) }) {
                    Text(
                        text = stringResource(R.string.label_search_deselect_all),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        HorizontalDivider()
    }
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
