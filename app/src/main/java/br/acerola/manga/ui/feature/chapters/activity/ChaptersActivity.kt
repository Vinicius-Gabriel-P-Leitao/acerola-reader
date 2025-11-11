package br.acerola.manga.ui.feature.chapters.activity

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import br.acerola.manga.domain.database.AcerolaDatabase
import br.acerola.manga.domain.service.library.archive.ArchiveMangaService
import br.acerola.manga.shared.permission.FolderAccessManager
import br.acerola.manga.shared.route.Destination
import br.acerola.manga.ui.common.activity.BaseActivity
import br.acerola.manga.ui.common.layout.NavigationTopBar
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModel
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModelFactory
import br.acerola.manga.ui.common.viewmodel.library.MangaLibraryViewModel
import br.acerola.manga.ui.common.viewmodel.library.MangaLibraryViewModelFactory
import br.acerola.manga.ui.feature.chapters.component.ChapterItem

class ChaptersActivity(
    override val startDestinationRes: Int = Destination.CHAPTERS.route
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
                application, folderAccessViewModel = folderAccessViewModel, libraryPort = ArchiveMangaService(
                    context = applicationContext,
                    folderDao = database.mangaFolderDao(),
                    chapterDao = database.chapterFileDao()
                )
            )
        )[MangaLibraryViewModel::class.java]
    }

    override fun NavGraphBuilder.setupNavGraph(context: Context, navController: NavHostController) {
        composable(route = context.getString(Destination.CHAPTERS.route)) { backStackEntry ->
            val folderId = intent?.getLongExtra("folderId", -1L) ?: -1L
            // NOTE: Pega o id do intent para fazer a busca dos capilutos
            if (folderId != -1L) mangaLibraryViewModel.selectFolder(folderId)

            Screen()
        }
    }

    @Composable
    override fun TopBar(navController: NavHostController) {
        NavigationTopBar(navController)
    }

    @Composable
    fun Screen() {
        val chapters by mangaLibraryViewModel.chapters.collectAsState()

        Column() {
            if (chapters.isEmpty()) {
                Text(text = "Nenhum capítulo encontrado")
            } else {
                chapters.forEach { chapter ->
                    ChapterItem (chapter = chapter, onClick = { /* abrir leitor */ })
                    Spacer(modifier = Modifier.height(height = 2.dp))
                }
            }
        }
    }
}