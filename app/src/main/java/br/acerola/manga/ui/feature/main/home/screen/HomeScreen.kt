package br.acerola.manga.ui.feature.main.home.screen

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.manga.R
import br.acerola.manga.shared.config.HomeLayoutType
import br.acerola.manga.ui.common.component.FloatingTool
import br.acerola.manga.ui.common.component.FloatingToolItem
import br.acerola.manga.ui.common.layout.ProgressIndicator
import br.acerola.manga.ui.common.layout.SearchBar
import br.acerola.manga.ui.common.viewmodel.library.archive.MangaFolderViewModel
import br.acerola.manga.ui.feature.chapters.activity.ChaptersActivity
import br.acerola.manga.ui.feature.main.home.component.MangaGridItem
import br.acerola.manga.ui.feature.main.home.component.MangaListItem

@Composable
fun HomeScreen(mangaFolderViewModel: MangaFolderViewModel) {
    val context = LocalContext.current

    val progress by mangaFolderViewModel.progress.collectAsState()
    val error by mangaFolderViewModel.error.collectAsState()

    val layout by mangaFolderViewModel.selectedHomeLayout.collectAsState()
    val isIndexing by mangaFolderViewModel.isIndexing.collectAsState()
    val folders by mangaFolderViewModel.folders.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchBar(
                items = folders,
                itemKey = { it.id },
                searchKey = { it.name },
                placeholder = stringResource(id = R.string.description_text_home_search_placeholder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 6.dp),
                itemContent = { folder ->
                    MangaListItem(
                        folder = folder, onClick = {
                            val intent = Intent(context, ChaptersActivity::class.java).apply {
                                putExtra("folder", folder)
                            }
                            context.startActivity(intent)
                        })
                })

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
                                putExtra("folder", folder)
                            }
                            context.startActivity(intent)
                        }

                        when (layout) {
                            HomeLayoutType.GRID -> MangaGridItem(folder = folder, onClick = onClick)
                            HomeLayoutType.LIST -> MangaListItem(folder = folder, onClick = onClick)
                        }
                    }
                }
            }
        }

        FloatingTool(
            icon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(id = R.string.description_icon_home_floating_tool_hub)
                )
            }, items = listOf(
                FloatingToolItem(
                    label = if (layout == HomeLayoutType.GRID) stringResource(id = R.string.description_text_home_layout_list_label) else stringResource(
                        id = R.string.description_text_home_layout_grid_label
                    ),
                    onClick = {
                        mangaFolderViewModel.updateHomeLayout(
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

                // TODO: Criar filtragem por categoria.
                FloatingToolItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = stringResource(id = R.string.description_icon_home_filter)
                        )
                    },
                    label = stringResource(id = R.string.description_text_home_filter_label),
                    onClick = { println("Filtrar") })
            )
        )

        Box(
            contentAlignment = Alignment.BottomStart,
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 8.dp),
        ) {
            ProgressIndicator(
                isLoading = isIndexing,
                progress = if (progress >= 0) progress / 100f else null,
            )
        }
    }
}

@Composable
private fun EmptyState(error: Throwable?) {
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
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
