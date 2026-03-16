package br.acerola.manga.module.main.home

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.component.FloatingTool
import br.acerola.manga.common.ux.component.FloatingToolItem
import br.acerola.manga.common.ux.component.SearchBar
import br.acerola.manga.common.ux.layout.ProgressIndicator
import br.acerola.manga.common.ux.theme.local.LocalSnackbarHostState
import br.acerola.manga.config.preference.HomeLayoutType
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.dto.history.ReadingHistoryDto
import br.acerola.manga.module.main.Main
import br.acerola.manga.module.main.common.component.MangaListItem
import br.acerola.manga.module.main.home.component.MangaGridItem
import br.acerola.manga.module.main.home.state.HomeAction
import br.acerola.manga.module.main.home.state.HomeUiState
import br.acerola.manga.module.manga.MangaActivity
import br.acerola.manga.module.reader.ReaderActivity
import br.acerola.manga.presentation.R

@Composable
fun Main.Home.Layout.Screen(
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current

    LaunchedEffect(Unit) {
        homeViewModel.uiEvents.collect { message ->
            snackbarHostState.showSnackbar(message.uiMessage.asString(context))
        }
    }

    val layout by homeViewModel.selectedHomeLayout.collectAsState()
    val isIndexing by homeViewModel.isIndexing.collectAsState()
    val progress by homeViewModel.progress.collectAsState()
    val mangas by homeViewModel.mangas.collectAsState()

    val uiState = HomeUiState(
        layout = layout,
        isIndexing = isIndexing,
        indexingProgress = if (progress >= 0) progress / 100f else null,
        mangas = mangas
    )

    val onAction: (HomeAction) -> Unit = { action ->
        when (action) {
            is HomeAction.UpdateLayout -> homeViewModel.updateHomeLayout(action.layout)
            is HomeAction.ClickManga -> {
                val intent = Intent(context, MangaActivity::class.java).apply {
                    putExtra(MangaActivity.ChapterExtra.MANGA, action.manga)
                }
                context.startActivity(intent)
            }
            is HomeAction.ClickContinue -> {
                val intent = Intent(context, ReaderActivity::class.java).apply {
                    putExtra(ReaderActivity.PageExtra.INITIAL_PAGE, action.history.lastPage)
                    putExtra(ReaderActivity.PageExtra.MANGA_ID, action.manga.directory.id)
                    putExtra(ReaderActivity.PageExtra.CHAPTER_ID, action.history.chapterArchiveId)
                }
                context.startActivity(intent)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Acerola.Component.SearchBar<Pair<MangaDto, ReadingHistoryDto?>>(
                items = uiState.mangas,
                placeholder = stringResource(id = R.string.description_text_home_search_placeholder),
                itemKey = { (manga, _) -> manga.directory.id },
                searchKey = { (manga, _) -> manga.directory.name },
                modifier = Modifier.padding(all = 6.dp),
                itemContent = { (manga, history) ->
                    Main.Common.Component.MangaListItem(
                        manga = manga,
                        onPlayClick = history?.let {
                            { onAction(HomeAction.ClickContinue(manga, it)) }
                        },
                        onClick = { onAction(HomeAction.ClickManga(manga)) }
                    )
                })

            if (uiState.mangas.isEmpty() && !uiState.isIndexing) {
                EmptyState()
            } else {
                val gridCells = when (uiState.layout) {
                    HomeLayoutType.GRID -> GridCells.Adaptive(minSize = 120.dp)
                    HomeLayoutType.LIST -> GridCells.Fixed(count = 1)
                }

                LazyVerticalGrid(
                    columns = gridCells,
                    contentPadding = PaddingValues(all = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(space = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
                ) {
                    items(items = uiState.mangas) { (manga, history) ->
                        when (uiState.layout) {
                            HomeLayoutType.GRID -> Main.Home.Component.MangaGridItem(
                                manga = manga,
                                onClick = { onAction(HomeAction.ClickManga(manga)) }
                            )
                            HomeLayoutType.LIST -> Main.Common.Component.MangaListItem(
                                manga = manga,
                                onPlayClick = history?.let { h ->
                                    { onAction(HomeAction.ClickContinue(manga, h)) }
                                },
                                onClick = { onAction(HomeAction.ClickManga(manga)) }
                            )
                        }
                    }
                }
            }
        }

        Acerola.Component.FloatingTool(
            icon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(id = R.string.description_icon_home_floating_tool_hub)
                )
            }, items = listOf(
                FloatingToolItem(
                    onClick = {
                        onAction(HomeAction.UpdateLayout(
                            layout = when (uiState.layout) {
                                HomeLayoutType.LIST -> HomeLayoutType.GRID
                                HomeLayoutType.GRID -> HomeLayoutType.LIST
                            }
                        ))
                    },
                    icon = {
                        Icon(
                            imageVector = if (uiState.layout == HomeLayoutType.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            contentDescription = stringResource(id = R.string.description_icon_home_change_layout)
                        )
                    },
                ),

                // TODO: Criar filtragem por categoria.
                FloatingToolItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = stringResource(id = R.string.description_icon_home_filter)
                        )
                    }, onClick = { println("Filtrar") })
            )
        )

        Box(
            contentAlignment = Alignment.BottomStart,
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 8.dp),
        ) {
            Acerola.Layout.ProgressIndicator(
                isLoading = uiState.isIndexing,
                progress = uiState.indexingProgress,
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(id = R.string.description_text_home_empty_state),
                style = MaterialTheme.typography.headlineSmall
            )

        }
    }
}
