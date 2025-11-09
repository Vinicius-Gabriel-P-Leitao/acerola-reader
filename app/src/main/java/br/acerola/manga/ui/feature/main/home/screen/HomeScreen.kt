package br.acerola.manga.ui.feature.main.home.screen

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.manga.R
import br.acerola.manga.shared.config.HomeLayoutType
import br.acerola.manga.ui.common.component.FloatingTool
import br.acerola.manga.ui.common.component.FloatingToolItem
import br.acerola.manga.ui.common.component.Modal
import br.acerola.manga.ui.common.layout.DockedSearch
import br.acerola.manga.ui.common.layout.TopCenterProgressIndicator
import br.acerola.manga.ui.common.theme.AcerolaTheme
import br.acerola.manga.ui.feature.chapters.activity.ChaptersActivity
import br.acerola.manga.ui.feature.main.home.component.MangaGridItem
import br.acerola.manga.ui.feature.main.home.component.MangaListItem
import br.acerola.manga.ui.feature.main.home.viewmodel.MangaLibraryViewModel

@Composable
fun HomeScreen(mangaLibraryViewModel: MangaLibraryViewModel) {
    val context = LocalContext.current

    val progress by mangaLibraryViewModel.progress.collectAsState()
    val error by mangaLibraryViewModel.error.collectAsState()

    val layout by mangaLibraryViewModel.selectedHomeLayout.collectAsState()
    val isIndexing by mangaLibraryViewModel.isIndexing.collectAsState()
    val folders by mangaLibraryViewModel.folders.collectAsState()

    var showSyncList by remember { mutableStateOf(value = false) }

    AcerolaTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {

                DockedSearch(
                    items = folders,
                    itemLabel = { it.name },
                    placeholder = stringResource(id = R.string.description_text_home_search_placeholder),
                    onItemSelected = { selected ->
                        Log.d("Search", "Selecionado: $selected")
                    }
                )

                if (folders.isEmpty() && !isIndexing) {
                    EmptyState(error)
                } else {
                    val gridCells = when (layout) {
                        HomeLayoutType.GRID -> GridCells.Adaptive(minSize = 120.dp)
                        HomeLayoutType.LIST -> GridCells.Fixed(count = 1)
                    }

                    LazyVerticalGrid(
                        columns = gridCells,
                        contentPadding = PaddingValues(all = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
                    ) {
                        items(items = folders) { folder ->
                            val onClick = {
                                val intent = Intent(context, ChaptersActivity::class.java).apply {
                                    putExtra("folderId", folder.id)
                                }
                                context.startActivity(intent)
                            }

                            when (layout) {
                                HomeLayoutType.GRID -> MangaGridItem(folder = folder, onClick = onClick)
                                HomeLayoutType.LIST -> MangaListItem(context, folder = folder, onClick = onClick)
                            }
                        }
                    }
                }
            }

            TopCenterProgressIndicator(
                isLoading = isIndexing,
                progress = if (progress >= 0) progress / 100f else null
            )

            FloatingTool(
                icon = { Icon(imageVector = Icons.Default.Edit, contentDescription = stringResource(id = R.string.description_icon_home_floating_tool_hub)) },
                items = listOf(
                    FloatingToolItem(
                        icon = { Icon(imageVector = Icons.Default.Sync, contentDescription = stringResource(id = R.string.description_text_home_sync_label)) },
                        label = stringResource(id = R.string.description_text_home_sync_label),
                        onClick = { showSyncList = true }
                    ),

                    FloatingToolItem(
                        label = if (layout == HomeLayoutType.GRID) stringResource(id = R.string.description_text_home_layout_list_label) else stringResource(id = R.string.description_text_home_layout_grid_label),
                        onClick = {
                            mangaLibraryViewModel.updateHomeLayout(
                                layout = when (layout) {
                                    HomeLayoutType.LIST -> HomeLayoutType.GRID
                                    HomeLayoutType.GRID -> HomeLayoutType.LIST
                                }
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = if (layout == HomeLayoutType.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                contentDescription = stringResource(id = R.string.description_icon_home_change_layout)
                            )
                        },
                    ),

                    FloatingToolItem(
                        icon = { Icon(imageVector = Icons.Default.FilterList, contentDescription = stringResource(id = R.string.description_icon_home_filter)) },
                        label = stringResource(id = R.string.description_text_home_filter_label),
                        onClick = { println("Filtrar") }
                    )
                )
            )

            Modal(
                show = showSyncList,
                title = stringResource(id = R.string.title_home_sync_modal),
                onDismiss = { showSyncList = false },
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(space = 16.dp, Alignment.CenterHorizontally),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                ) {
                    listOf(
                        Triple(
                            first = Icons.Default.SyncProblem,
                            second = stringResource(id = R.string.description_text_home_deep_sync),
                            third = { mangaLibraryViewModel.rescanLibrary() }
                        ),
                        Triple(
                            first = Icons.Default.Sync,
                            second = stringResource(id = R.string.description_text_home_quick_sync),
                            third = { mangaLibraryViewModel.syncLibrary() }
                        )
                    ).forEach { (icon, label, action) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(size = 88.dp)
                                    .shadow(
                                        elevation = 6.dp, shape = MaterialTheme.shapes.large
                                    )
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                                MaterialTheme.colorScheme.surface
                                            )
                                        ), shape = MaterialTheme.shapes.large
                                    )
                                    .clickable { action() }) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(size = 48.dp),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(error: Throwable?) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(id = R.string.description_text_home_empty_state),
                style = MaterialTheme.typography.headlineSmall
            )

            error?.let {
                Text(
                    text = it.message.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
