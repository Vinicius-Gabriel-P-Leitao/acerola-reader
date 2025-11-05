package br.acerola.manga.ui.feature.chapters.activity

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import br.acerola.manga.domain.database.AcerolaDatabase
import br.acerola.manga.domain.permission.FolderAccessManager
import br.acerola.manga.domain.service.archive.ArchiveMangaService
import br.acerola.manga.shared.route.Destination
import br.acerola.manga.ui.common.activity.BaseActivity
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModel
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModelFactory
import br.acerola.manga.ui.feature.home.viewmodel.MangaLibraryViewModel
import br.acerola.manga.ui.feature.home.viewmodel.MangaLibraryViewModelFactory

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
                application, folderAccessViewModel = folderAccessViewModel, archiveService = ArchiveMangaService(
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
            if (folderId != -1L) mangaLibraryViewModel.selectFolder(folderId)

            ChaptersScreen(folderId = folderId)
        }
    }

    @Composable
    fun ChaptersScreen(folderId: Long) {
        val chapters by mangaLibraryViewModel.chapters.collectAsState()

        Column() {
            if (chapters.isEmpty()) {
                Text("Nenhum capítulo encontrado")
            } else {
                chapters.forEach { ch ->
                    Text("• ${ch.chapter}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}