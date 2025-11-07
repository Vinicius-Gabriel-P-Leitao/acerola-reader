package br.acerola.manga.ui.feature.main.home.screen

import android.content.Intent
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.acerola.manga.ui.common.component.FloatingTool
import br.acerola.manga.ui.common.component.FloatingToolItem
import br.acerola.manga.ui.common.component.Modal
import br.acerola.manga.ui.common.layout.TopCenterProgressIndicator
import br.acerola.manga.ui.common.theme.AcerolaTheme
import br.acerola.manga.ui.feature.chapters.activity.ChaptersActivity
import br.acerola.manga.ui.feature.main.home.component.MangaGridItem
import br.acerola.manga.ui.feature.main.home.component.MangaListItem
import br.acerola.manga.ui.feature.main.home.viewmodel.MangaLibraryViewModel

@Composable
fun HomeScreen(mangaLibraryViewModel: MangaLibraryViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val isIndexing by mangaLibraryViewModel.isIndexing.collectAsState()
    val folders by mangaLibraryViewModel.folders.collectAsState()

    val progress by mangaLibraryViewModel.progress.collectAsState()
    val error by mangaLibraryViewModel.error.collectAsState()

    var isGridView by remember { mutableStateOf(false) }
    var showSyncList by remember { mutableStateOf(value = false) }

    LaunchedEffect(key1 = Unit) {
        mangaLibraryViewModel.indexLibraryFromSavedFolder()
    }

    DisposableEffect(key1 = lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                mangaLibraryViewModel.quickIndexLibraryFromSavedFolder()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AcerolaTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                error?.let {
                    Text(text = "Erro: ${it.message}", color = Color.Red)
                }

                if (folders.isEmpty() && !isIndexing) {
                    EmptyState()
                } else {
                    val gridCells = if (isGridView) GridCells.Adaptive(120.dp) else GridCells.Fixed(1)

                    LazyVerticalGrid(
                        columns = gridCells,
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items = folders) { folder ->
                            val onClick = {
                                val intent = Intent(context, ChaptersActivity::class.java).apply {
                                    putExtra("folderId", folder.id)
                                }
                                context.startActivity(intent)
                            }

                            // TODO: Salvar prenferencia no dataStore
                            if (isGridView) {
                                MangaGridItem(folder = folder, onClick = onClick)
                            } else {
                                MangaListItem(folder = folder, onClick = onClick)
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
                icon = { Icon(imageVector = Icons.Default.Edit, contentDescription = "Abrir hub") },
                items = listOf(
                    FloatingToolItem(
                        icon = { Icon(imageVector = Icons.Default.Sync, contentDescription = "Sync") },
                        label = "Sync",
                        onClick = { showSyncList = true }
                    ),

                    // TODO: Usar o dataStore para salvar essa preferencia
                    FloatingToolItem(
                        label = if (isGridView) "Lista" else "Grade",
                        onClick = { isGridView = !isGridView },
                        icon = {
                            Icon(
                                imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                contentDescription = "Mudar Visualização"
                            )
                        },
                    ),

                    FloatingToolItem(
                        icon = { Icon(imageVector = Icons.Default.FilterList, contentDescription = "Filtro") },
                        label = "Filter",
                        onClick = { println("Filtrar") }
                    )
                )
            )

            Modal(
                show = showSyncList,
                title = "Sincronizar mangás",
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
                            second = "Sincronização demorada",
                            third = { mangaLibraryViewModel.indexLibraryFromSavedFolder() }
                        ),
                        Triple(
                            first = Icons.Default.Sync,
                            second = "Sincronização rápida",
                            third = { mangaLibraryViewModel.quickIndexLibraryFromSavedFolder() }
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
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Nenhum mangá na biblioteca",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Adicione uma pasta com seus mangás nas configurações.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
