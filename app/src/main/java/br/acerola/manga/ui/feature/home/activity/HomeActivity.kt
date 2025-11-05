package br.acerola.manga.ui.feature.home.activity

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import br.acerola.manga.R
import br.acerola.manga.domain.database.AcerolaDatabase
import br.acerola.manga.domain.permission.FolderAccessManager
import br.acerola.manga.domain.service.archive.ArchiveMangaService
import br.acerola.manga.shared.route.Destination
import br.acerola.manga.ui.common.activity.BaseActivity
import br.acerola.manga.ui.common.component.ButtonType
import br.acerola.manga.ui.common.component.SmartButton
import br.acerola.manga.ui.common.theme.AcerolaTheme
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModel
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModelFactory
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
    fun HomeScreen() {
        val isIndexing by mangaLibraryViewModel.isIndexing.collectAsState()
        val progress by mangaLibraryViewModel.progress.collectAsState()
        val error by mangaLibraryViewModel.error.collectAsState()

        val library by mangaLibraryViewModel.library.collectAsState()
        val folders by mangaLibraryViewModel.folders.collectAsState()
        val chapters by mangaLibraryViewModel.chapters.collectAsState()

        var selectedFolderId by remember { mutableStateOf<Long?>(value = null) }

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

                    Button(onClick = { mangaLibraryViewModel.indexLibraryFromSavedFolder() }) {
                        Text(text = "Reindexar biblioteca")
                    }

                    LazyColumn {
                        items(folders) { folder ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedFolderId = folder.id
                                        mangaLibraryViewModel.selectFolder(folderId = folder.id)
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = folder.name,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                if (selectedFolderId == folder.id) {
                                    chapters.forEach { ch ->
                                        Text(
                                            text = "• ${ch.chapter}",
                                            color = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.padding(start = 16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    override fun TopBar(navController: NavHostController, extraActions: @Composable RowScope.() -> Unit) {
        super.TopBar(navController) {
            FilterButton()
        }
    }

    @Composable
    fun FilterButton() {
        val context = LocalContext.current

        SmartButton(type = ButtonType.ICON, onClick = {
            println("Filtrar")
        }) {
            Icon(
                imageVector = Icons.Filled.FilterList,
                contentDescription = context.getString(R.string.description_icon_filter_mangas_catalog)
            )
        }
    }
}