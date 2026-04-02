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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.component.FloatingTool
import br.acerola.manga.common.ux.component.FloatingToolItem
import br.acerola.manga.common.ux.component.SearchBar
import br.acerola.manga.common.ux.component.SnackbarVariant
import br.acerola.manga.common.ux.component.showSnackbar
import br.acerola.manga.common.ux.theme.local.LocalSnackbarHostState
import br.acerola.manga.config.preference.HomeLayoutType
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.dto.history.ReadingHistoryDto
import br.acerola.manga.module.main.Main
import br.acerola.manga.module.main.common.component.MangaActionsSheet
import br.acerola.manga.module.main.common.component.MangaListItem
import br.acerola.manga.module.main.home.component.HomeFilterSheet
import br.acerola.manga.module.main.home.component.MangaGridItem
import br.acerola.manga.module.main.home.state.HomeAction
import br.acerola.manga.module.main.home.state.HomeUiState
import br.acerola.manga.module.manga.MangaActivity
import br.acerola.manga.module.reader.ReaderActivity
import br.acerola.manga.ui.R

@Composable
fun Main.Home.Layout.Screen(
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current

    LaunchedEffect(Unit) {
        homeViewModel.uiEvents.collect { message ->
            snackbarHostState.showSnackbar(message.uiMessage.asString(context), SnackbarVariant.Error)
        }
    }

    val layout by homeViewModel.selectedHomeLayout.collectAsStateWithLifecycle()
    val isIndexing by homeViewModel.isIndexing.collectAsStateWithLifecycle()
    val mangas by homeViewModel.mangas.collectAsStateWithLifecycle()
    val allCategories by homeViewModel.allCategories.collectAsStateWithLifecycle()
    val sortSettings by homeViewModel.sortSettings.collectAsStateWithLifecycle()
    val filterSettings by homeViewModel.filterSettings.collectAsStateWithLifecycle()

    val uiState = HomeUiState(
        layout = layout,
        isIndexing = isIndexing,
        mangas = mangas,
        sortType = sortSettings.type,
        sortDirection = sortSettings.direction,
        filter = filterSettings
    )

    var selectedMangaForActions by remember { mutableStateOf<MangaDto?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }

    var query by rememberSaveable { mutableStateOf("") }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }

    val filteredMangas = remember(query, uiState.mangas) {
        if (query.isEmpty()) {
            uiState.mangas
        } else {
            uiState.mangas.filter { (manga, _, _) ->
                manga.directory.name.contains(query, ignoreCase = true) ||
                        manga.remoteInfo?.title?.contains(query, ignoreCase = true) == true
            }
        }
    }

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
        if (uiState.mangas.isEmpty() && !uiState.isIndexing) {
            EmptyState()
        } else {
            val gridCells = when (uiState.layout) {
                HomeLayoutType.GRID -> GridCells.Adaptive(minSize = 120.dp)
                HomeLayoutType.LIST -> GridCells.Fixed(count = 1)
            }

            LazyVerticalGrid(
                columns = gridCells,
                verticalArrangement = Arrangement.spacedBy(space = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                contentPadding = PaddingValues(start = 8.dp, top = 72.dp, end = 8.dp, bottom = 80.dp),
            ) {
                items(items = if (searchExpanded) filteredMangas else uiState.mangas) { (manga, history, chapterCount) ->
                    when (uiState.layout) {
                        HomeLayoutType.GRID -> Main.Home.Component.MangaGridItem(
                            manga = manga,
                            history = history,
                            chapterCount = chapterCount,
                            onShowActions = { selectedMangaForActions = manga },
                            onClick = { onAction(HomeAction.ClickManga(manga)) }
                        )

                        HomeLayoutType.LIST -> Main.Common.Component.MangaListItem(
                            manga = manga,
                            chapterCount = chapterCount,
                            subtitle = manga.remoteInfo?.authors?.name,
                            onClick = { onAction(HomeAction.ClickManga(manga)) },
                            onPlayClick = history?.let { { onAction(HomeAction.ClickContinue(manga, it)) } },
                            onShowActions = { selectedMangaForActions = manga },
                        )
                    }
                }
            }
        }

        Acerola.Component.SearchBar<Triple<MangaDto, ReadingHistoryDto?, Int>>(
            query = query,
            items = filteredMangas,
            expanded = searchExpanded,
            onQueryChange = { query = it },
            onSearch = { searchExpanded = false },
            onExpandedChange = { searchExpanded = it },
            contentPadding = PaddingValues(bottom = 16.dp),
            itemKey = { (manga, _, _) -> manga.directory.id },
            placeholder = stringResource(id = R.string.description_text_home_search_placeholder),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp),
            itemContent = { (manga, history, chapterCount) ->
                Main.Common.Component.MangaListItem(
                    manga = manga,
                    chapterCount = chapterCount,
                    onPlayClick = history?.let { { onAction(HomeAction.ClickContinue(manga, it)) } },
                    onClick = { onAction(HomeAction.ClickManga(manga)) }
                )
            })

        if (!searchExpanded) {
            Acerola.Component.FloatingTool(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(id = R.string.description_icon_home_floating_tool_hub)
                    )
                }, items = listOf(
                    FloatingToolItem(
                        onClick = {
                            onAction(
                                HomeAction.UpdateLayout(
                                    layout = when (uiState.layout) {
                                        HomeLayoutType.LIST -> HomeLayoutType.GRID
                                        HomeLayoutType.GRID -> HomeLayoutType.LIST
                                    }
                                )
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = if (uiState.layout == HomeLayoutType.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                contentDescription = stringResource(id = R.string.description_icon_home_change_layout)
                            )
                        },
                    ),

                    FloatingToolItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = stringResource(id = R.string.description_icon_home_filter)
                            )
                        }, onClick = { showFilterSheet = true })
                )
            )

        }

        val activeManga = selectedMangaForActions
        if (activeManga != null) {
            Main.Common.Component.MangaActionsSheet(
                manga = activeManga,
                categories = allCategories,
                onHide = { homeViewModel.hideManga(activeManga.directory.id) },
                onDelete = { homeViewModel.deleteManga(activeManga.directory.id) },
                onBookmark = { categoryId -> homeViewModel.setMangaCategory(activeManga.directory.id, categoryId) },
                onDismiss = { selectedMangaForActions = null },
            )
        }

        if (showFilterSheet) {
            Main.Home.Component.HomeFilterSheet(
                sortSettings = sortSettings,
                filterSettings = filterSettings,
                categories = allCategories,
                onSortChange = { homeViewModel.updateSortSettings(it) },
                onFilterChange = { homeViewModel.updateFilterSettings(it) },
                onDismiss = { showFilterSheet = false }
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
