package br.acerola.manga.ui.feature.main.home.screen

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.acerola.manga.R
import br.acerola.manga.domain.database.AcerolaDatabase
import br.acerola.manga.domain.service.archive.ArchiveMangaService
import br.acerola.manga.shared.permission.FolderAccessManager
import br.acerola.manga.ui.common.component.CardType
import br.acerola.manga.ui.common.component.SmartCard
import br.acerola.manga.ui.common.theme.AcerolaTheme
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModel
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModelFactory
import br.acerola.manga.ui.feature.chapters.activity.ChaptersActivity
import br.acerola.manga.ui.feature.main.home.component.FilterButton
import br.acerola.manga.ui.feature.main.home.component.ResetIndexManga
import br.acerola.manga.ui.feature.main.home.viewmodel.MangaLibraryViewModel
import br.acerola.manga.ui.feature.main.home.viewmodel.MangaLibraryViewModelFactory

@Composable
fun HomeScreen(onSetTopBarActions: (@Composable RowScope.() -> Unit) -> Unit) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val application = activity.application

    val folderAccessViewModel: FolderAccessViewModel = viewModel(
        factory = FolderAccessViewModelFactory(
            application = application, manager = FolderAccessManager(context)
        )
    )

    val database = remember { AcerolaDatabase.getInstance(context) }
    val archiveService = remember {
        ArchiveMangaService(
            context = context, folderDao = database.mangaFolderDao(), chapterDao = database.chapterFileDao()
        )
    }

    val mangaLibraryViewModel: MangaLibraryViewModel = viewModel(
        factory = MangaLibraryViewModelFactory(
            application, archiveService, folderAccessViewModel
        )
    )

    val isIndexing by mangaLibraryViewModel.isIndexing.collectAsState()
    val folders by mangaLibraryViewModel.folders.collectAsState()

    val progress by mangaLibraryViewModel.progress.collectAsState()
    val error by mangaLibraryViewModel.error.collectAsState()


    LaunchedEffect(key1 = Unit) {
        mangaLibraryViewModel.indexLibraryFromSavedFolder()
    }

    DisposableEffect(key1 = mangaLibraryViewModel) {
        onSetTopBarActions {
            ResetIndexManga(mangaLibraryViewModel = mangaLibraryViewModel)
            FilterButton()
        }

        onDispose {
            onSetTopBarActions {}
        }
    }

    AcerolaTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isIndexing) {
                LinearProgressIndicator(progress = { progress / 100f })
            }

            error?.let {
                Text(text = "Erro: ${it.message}", color = Color.Red)
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(count = 1),
                contentPadding = PaddingValues(all = 8.dp),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                items(items = folders) { folder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(height = 120.dp)
                            .padding(all = 4.dp)
                    ) {
                        SmartCard(
                            type = CardType.IMAGE,
                            image = painterResource(id = R.drawable.ic_launcher_background),
                            modifier = Modifier
                                .width(width = 80.dp)
                                .fillMaxHeight(),
                            onClick = {
                                val intent = Intent(context, ChaptersActivity::class.java).apply {
                                    putExtra("folderId", folder.id)
                                }
                                context.startActivity(intent)
                            })

                        Spacer(modifier = Modifier.width(width = 8.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(weight = 1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = folder.name, style = MaterialTheme.typography.titleMedium, maxLines = 1
                            )
                            Text(
                                text = "Manga mamaco", style = MaterialTheme.typography.bodyMedium, maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}
