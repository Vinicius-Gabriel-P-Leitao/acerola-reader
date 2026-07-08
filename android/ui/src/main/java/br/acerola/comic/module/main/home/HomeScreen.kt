package br.acerola.comic.module.main.home

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.acerola.comic.common.state.LocalSnackbarHostState
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.FabGroup
import br.acerola.comic.common.ux.component.FabGroupItem
import br.acerola.comic.common.ux.component.SnackbarVariant
import br.acerola.comic.common.ux.component.showSnackbar
import br.acerola.comic.common.ux.tokens.SizeTokens
import br.acerola.comic.common.ux.tokens.SpacingTokens
import br.acerola.comic.config.preference.types.HomeLayoutType
import br.acerola.comic.dto.ComicDto
import br.acerola.comic.module.comic.ComicActivity
import br.acerola.comic.module.main.Main
import br.acerola.comic.module.main.common.component.ComicActionsSheet
import br.acerola.comic.module.main.common.component.ComicListItem
import br.acerola.comic.module.main.home.component.ComicGridItem
import br.acerola.comic.module.main.home.component.HomeContinueBanner
import br.acerola.comic.module.main.home.component.HomeFilterSheet
import br.acerola.comic.module.main.home.component.HomeSearchBar
import br.acerola.comic.module.main.home.state.HomeAction
import br.acerola.comic.module.main.home.state.HomeUiState
import br.acerola.comic.module.reader.ReaderActivity
import br.acerola.comic.ui.R

@Composable
fun Main.Home.Template.Screen(
    homeViewModel: HomeViewModel = hiltViewModel(),
    onNavigateToConfig: () -> Unit,
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
    val comics by homeViewModel.comics.collectAsStateWithLifecycle()
    val allCategories by homeViewModel.allCategories.collectAsStateWithLifecycle()
    val sortSettings by homeViewModel.sortSettings.collectAsStateWithLifecycle()
    val filterSettings by homeViewModel.filterSettings.collectAsStateWithLifecycle()
    
    val lastRead by remember(comics) {
        derivedStateOf {
            comics
                ?.filter { it.second != null }
                ?.maxByOrNull { it.second?.updatedAt ?: 0L }
        }
    }
    
    val uiState =
        HomeUiState(
            layout = layout,
            isIndexing = isIndexing,
            comics = comics,
            sortType = sortSettings.type,
            sortDirection = sortSettings.direction,
            filter = filterSettings,
        )

    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedMangaForActions by remember { mutableStateOf<ComicDto?>(null) }
    var isBannerExpanded by remember { mutableStateOf(true) }

    val onAction: (HomeAction) -> Unit = { action ->
        when (action) {
            is HomeAction.UpdateLayout -> homeViewModel.updateHomeLayout(action.layout)
            is HomeAction.ClickManga -> {
                val intent =
                    Intent(context, ComicActivity::class.java).apply {
                        putExtra(ComicActivity.ChapterExtra.COMIC, action.comic)
                    }
                context.startActivity(intent)
            }
            is HomeAction.ClickContinue -> {
                val intent =
                    Intent(context, ReaderActivity::class.java).apply {
                        putExtra(ReaderActivity.PageExtra.INITIAL_PAGE, action.history.lastPage)
                        putExtra(ReaderActivity.PageExtra.MANGA_ID, action.comic.directory.id)
                        putExtra(ReaderActivity.PageExtra.CHAPTER_ID, action.history.chapterArchiveId)
                        putExtra(ReaderActivity.PageExtra.CHAPTER_SORT, action.history.chapterSort)
                    }
                context.startActivity(intent)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val comicList = uiState.comics
        
        when {
            comicList == null -> Unit
            comicList.isEmpty() && !uiState.isIndexing -> EmptyState()
            else -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Main.Home.Component.HomeSearchBar(
                        onClick = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(1f)
                            .padding(
                                start = SpacingTokens.Small,
                                end = SpacingTokens.Small,
                                top = SpacingTokens.ExtraLarge
                            )
                    )
                    
                    val (lastComic, lastHistory, _) = lastRead ?: Triple(null, null, 0)
                    
                    val gridCells =
                        when (uiState.layout) {
                            HomeLayoutType.GRID -> GridCells.Adaptive(minSize = SizeTokens.ComicGridMinSize)
                            HomeLayoutType.LIST -> GridCells.Fixed(count = 1)
                        }

                    LazyVerticalGrid(
                        columns = gridCells,
                        verticalArrangement = Arrangement.spacedBy(space = SpacingTokens.Small),
                        horizontalArrangement = Arrangement.spacedBy(space = SpacingTokens.Small),
                        contentPadding = PaddingValues(
                            start = SpacingTokens.Small,
                            top = 120.dp,
                            end = SpacingTokens.Small,
                            bottom = 80.dp
                        ),
                    ) {
                        if (lastComic != null && lastHistory != null) {
                            item(
                                key = "continue_banner",
                                span = { GridItemSpan(maxLineSpan) }
                            ) {
                                Main.Home.Component.HomeContinueBanner(
                                    comic = lastComic,
                                    history = lastHistory,
                                    isExpanded = isBannerExpanded,
                                    onExpandedChange = { isBannerExpanded = it },
                                    onContinueClick = { onAction(HomeAction.ClickContinue(lastComic, lastHistory)) },
                                    onComicClick = { onAction(HomeAction.ClickManga(lastComic)) },
                                    modifier = Modifier.padding(bottom = SpacingTokens.Small)
                                )
                            }
                        }
                        
                        items(
                        items = comicList,
                        key = { (comic, _, _) -> "manga_${comic.directory.id}" },
                    ) { (comic, history, chapterCount) ->
                        when (uiState.layout) {
                            HomeLayoutType.GRID ->
                                Main.Home.Component.ComicGridItem(
                                    comic = comic,
                                    history = history,
                                    chapterCount = chapterCount,
                                    onShowActions = { selectedMangaForActions = comic },
                                    onClick = { onAction(HomeAction.ClickManga(comic)) },
                                )

                            HomeLayoutType.LIST ->
                                Main.Common.Component.ComicListItem(
                                    comic = comic,
                                    chapterCount = chapterCount,
                                    subtitle = comic.remoteInfo?.authors?.name,
                                    onClick = { onAction(HomeAction.ClickManga(comic)) },
                                    onPlayClick = history?.let { { onAction(HomeAction.ClickContinue(comic, it)) } },
                                    onShowActions = { selectedMangaForActions = comic },
                                )
                        }
                    }
                }
            }
        }
    }

        Acerola.Component.FabGroup(
            icon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(id = R.string.description_icon_home_floating_tool_hub),
                )
            },
            items =
                listOf(
                    FabGroupItem(
                        onClick = {
                            onAction(
                                HomeAction.UpdateLayout(
                                    layout =
                                        when (uiState.layout) {
                                            HomeLayoutType.LIST -> HomeLayoutType.GRID
                                            HomeLayoutType.GRID -> HomeLayoutType.LIST
                                        },
                                ),
                            )
                        },
                        icon = {
                            Icon(
                                imageVector =
                                    if (uiState.layout == HomeLayoutType.GRID) {
                                        Icons.AutoMirrored.Filled.ViewList
                                    } else {
                                        Icons.Default.GridView
                                    },
                                contentDescription = stringResource(id = R.string.description_icon_home_change_layout),
                            )
                        },
                    ),
                    FabGroupItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = stringResource(id = R.string.description_icon_home_filter),
                            )
                        },
                        onClick = { showFilterSheet = true },
                    ),
                ),
        )

        val activeManga = selectedMangaForActions
        if (activeManga != null) {
            Main.Common.Component.ComicActionsSheet(
                comic = activeManga,
                categories = allCategories,
                onHide = { homeViewModel.hideManga(activeManga.directory.id) },
                onDelete = { homeViewModel.deleteComic(activeManga.directory.id) },
                onBookmark = { categoryId -> homeViewModel.setMangaCategory(activeManga.directory.id, categoryId) },
                onDismiss = { selectedMangaForActions = null },
            )
        }

        if (showFilterSheet) {
            Main.Home.Component.HomeFilterSheet(
                sortSettings = sortSettings,
                filterSettings = filterSettings,
                categories = allCategories,
                onDismiss = { showFilterSheet = false },
                onSortChange = { homeViewModel.updateSortSettings(it) },
                onFilterChange = { homeViewModel.updateFilterSettings(it) },
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(id = R.string.description_text_home_empty_state),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}
