package br.acerola.manga.ui.feature.main.activity

import android.content.Context
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import br.acerola.manga.domain.database.dao.database.AcerolaDatabase
import br.acerola.manga.domain.service.library.chapter.FileChapterOperation
import br.acerola.manga.domain.service.library.manga.FolderMangaOperation
import br.acerola.manga.domain.service.library.manga.MangaDexMangaOperation
import br.acerola.manga.domain.service.library.sync.ArchiveSyncService
import br.acerola.manga.domain.service.library.sync.MangaDexSyncService
import br.acerola.manga.shared.permission.FolderAccessManager
import br.acerola.manga.ui.common.route.Destination
import br.acerola.manga.ui.common.activity.BaseActivity
import br.acerola.manga.ui.common.layout.NavigationBottomBar
import br.acerola.manga.ui.common.viewmodel.archive.file.FilePreferencesViewModel
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModel
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModelFactory
import br.acerola.manga.ui.common.viewmodel.library.archive.MangaFolderViewModel
import br.acerola.manga.ui.common.viewmodel.library.archive.MangaFolderViewModelFactory
import br.acerola.manga.ui.common.viewmodel.library.metadata.MangaMetadataViewModel
import br.acerola.manga.ui.common.viewmodel.library.metadata.MangaMetadataViewModelFactory
import br.acerola.manga.ui.feature.main.config.screen.ConfigScreen
import br.acerola.manga.ui.feature.main.history.screen.HistoryScreen
import br.acerola.manga.ui.feature.main.home.viewmodel.HomeViewModel
import br.acerola.manga.ui.feature.main.home.viewmodel.HomeViewModelFactory
import br.acerola.manga.ui.feature.main.home.screen.HomeScreen

class MainActivity(
    override val startDestinationRes: Int = Destination.HOME.route
) : BaseActivity() {
    private val filePreferencesViewModel: FilePreferencesViewModel by viewModels()

    private val database by lazy { AcerolaDatabase.getInstance(context = this) }

    private val folderAccessViewModel: FolderAccessViewModel by viewModels {
        FolderAccessViewModelFactory(application, manager = FolderAccessManager(context = this))
    }

    private val mangaFolderViewModel: MangaFolderViewModel by viewModels {
        MangaFolderViewModelFactory(
            application,
            folderAccessViewModel = folderAccessViewModel,
            libraryPort = ArchiveSyncService(
                context = this, folderDao = database.mangaFolderDao(), chapterDao = database.chapterFileDao(),
            ), mangaOperations = FolderMangaOperation(
                context = this, folderDao = database.mangaFolderDao(), chapterDao = database.chapterFileDao()
            ), chapterOperations = FileChapterOperation(
                chapterDao = database.chapterFileDao()
            )
        )
    }

    private val mangaDexViewModel: MangaMetadataViewModel by viewModels {
        MangaMetadataViewModelFactory(
            application,
            libraryPort = MangaDexSyncService(
                folderDao = database.mangaFolderDao(),
                mangaDao = database.mangaMetadataDao(),
            ),
            mangaOperations = MangaDexMangaOperation(
                folderDao = database.mangaFolderDao(),
                mangaDao = database.mangaMetadataDao(),
            )
        )
    }

    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(
            application,
            mangaFolderViewModel = mangaFolderViewModel,
            mangaMetadataViewModel = mangaDexViewModel
        )
    }

    override fun NavGraphBuilder.setupNavGraph(context: Context, navController: NavHostController) {
        defaultComposable(context, Destination.HOME) {
            HomeScreen(
                mangaFolderViewModel,
                homeViewModel
            )
        }
        defaultComposable(context, Destination.HISTORY) {
            HistoryScreen()
        }
        defaultComposable(context, Destination.CONFIG) {
            ConfigScreen(
                folderAccessViewModel, filePreferencesViewModel, mangaFolderViewModel, mangaDexViewModel
            )
        }
    }

    @Composable
    override fun TopBar(navController: NavHostController) {
    }

    @Composable
    override fun BottomBar(navController: NavHostController) {
        NavigationBottomBar(navController)
    }

    private fun NavGraphBuilder.defaultComposable(
        context: Context,
        destination: Destination,
        content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit
    ) {
        composable(
            route = context.getString(destination.route), enterTransition = {
                scaleIn(
                    initialScale = 0.8f, animationSpec = tween(durationMillis = 300)
                ) + fadeIn(animationSpec = tween(durationMillis = 300))
            }, exitTransition = {
                scaleOut(
                    targetScale = 0.8f, animationSpec = tween(durationMillis = 300)
                ) + fadeOut(animationSpec = tween(durationMillis = 300))
            }, popEnterTransition = {
                scaleIn(
                    initialScale = 1.2f, animationSpec = tween(durationMillis = 300)
                ) + fadeIn(animationSpec = tween(durationMillis = 300))
            }, popExitTransition = {
                scaleOut(
                    targetScale = 1.2f, animationSpec = tween(durationMillis = 300)
                ) + fadeOut(animationSpec = tween(durationMillis = 300))
            }, content = content
        )
    }
}