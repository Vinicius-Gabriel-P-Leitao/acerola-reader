package br.acerola.manga.ui.feature.home.activity

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import br.acerola.manga.R
import br.acerola.manga.domain.database.AcerolaDatabase
import br.acerola.manga.domain.service.archive.ArchiveMangaService
import br.acerola.manga.shared.permission.FolderAccessManager
import br.acerola.manga.shared.route.Destination
import br.acerola.manga.ui.common.activity.BaseActivity
import br.acerola.manga.ui.common.component.ButtonType
import br.acerola.manga.ui.common.component.CardType
import br.acerola.manga.ui.common.component.SmartButton
import br.acerola.manga.ui.common.component.SmartCard
import br.acerola.manga.ui.common.layout.NavigationBottomBar
import br.acerola.manga.ui.common.layout.NavigationTopBar
import br.acerola.manga.ui.common.theme.AcerolaTheme
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModel
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModelFactory
import br.acerola.manga.ui.feature.chapters.activity.ChaptersActivity
import br.acerola.manga.ui.feature.home.viewmodel.MangaLibraryViewModel
import br.acerola.manga.ui.feature.home.viewmodel.MangaLibraryViewModelFactory

class HomeActivity(
    override val startDestinationRes: Int = Destination.HOME.route
) : BaseActivity() {

    private val database by lazy {
        AcerolaDatabase.getInstance(applicationContext)
    }

    private val folderAccessViewModel by lazy {
        ViewModelProvider(
            owner = this, factory = FolderAccessViewModelFactory(
                application, manager = FolderAccessManager(applicationContext)
            )
        )[FolderAccessViewModel::class.java]
    }

    private val mangaLibraryViewModel by lazy {
        ViewModelProvider(
            owner = this, factory = MangaLibraryViewModelFactory(
                application, folderAccessViewModel = folderAccessViewModel, archiveService = ArchiveMangaService(
                    context = applicationContext,
                    folderDao = database.mangaFolderDao(),
                    chapterDao = database.chapterFileDao()
                )
            )
        )[MangaLibraryViewModel::class.java]
    }


    override fun NavGraphBuilder.setupNavGraph(context: Context, navController: NavHostController) {
        composable(route = context.getString(Destination.HOME.route)) { HomeScreen() }
    }

    @Composable
    override fun TopBar(navController: NavHostController) {
        NavigationTopBar(navController, extraActions = {
            FilterButton()
            ResetIndexManga()
        })
    }

    @Composable
    override fun BottomBar(navController: NavHostController) {
        NavigationBottomBar(navController)
    }

    @Composable
    fun HomeScreen() {
        val context = LocalContext.current

        val isIndexing by mangaLibraryViewModel.isIndexing.collectAsState()
        val progress by mangaLibraryViewModel.progress.collectAsState()
        val error by mangaLibraryViewModel.error.collectAsState()

        val folders by mangaLibraryViewModel.folders.collectAsState()

        // NOTE: Isso é basicamente quando montar faça isso
        LaunchedEffect(key1 = Unit) {
            mangaLibraryViewModel.indexLibraryFromSavedFolder()
        }

        AcerolaTheme {
            Scaffold() { _ ->
                Column {
                    if (isIndexing) {
                        LinearProgressIndicator(progress = progress / 100f)
                    }

                    error?.let {
                        Text(text = "Erro: ${it.message}", color = Color.Red)
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(count = 1),
                        contentPadding = PaddingValues(all = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
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
                                    }
                                )

                                Spacer(modifier = Modifier.width(width = 8.dp))

                                Column(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(weight = 1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = folder.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1
                                    )

                                    Text(
                                        text = "Manga mamaco",
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 2
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
    fun ResetIndexManga() {
        SmartButton(type = ButtonType.ICON, modifier = Modifier.size(size = 48.dp), onClick = {
            mangaLibraryViewModel.indexLibraryFromSavedFolder()
        }) {
            Icon(
                imageVector = Icons.Default.RestartAlt, contentDescription = "Reset dos mangás"
            )
        }
    }

    @Composable
    fun FilterButton() {
        val context = LocalContext.current

        SmartButton(
            type = ButtonType.ICON, modifier = Modifier.size(size = 48.dp), onClick = {
            println("Filtrar")
        }) {
            Icon(
                imageVector = Icons.Filled.FilterList,
                contentDescription = context.getString(R.string.description_icon_filter_mangas_catalog)
            )
        }
    }
}