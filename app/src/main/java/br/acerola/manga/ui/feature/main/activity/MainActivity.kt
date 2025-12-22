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
import br.acerola.manga.ui.common.activity.BaseActivity
import br.acerola.manga.ui.common.layout.NavigationBottomBar
import br.acerola.manga.ui.common.route.Destination
import br.acerola.manga.ui.common.viewmodel.archive.file.FilePreferencesViewModel
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModel
import br.acerola.manga.ui.common.viewmodel.library.archive.MangaFolderViewModel
import br.acerola.manga.ui.common.viewmodel.library.metadata.MangaMetadataViewModel
import br.acerola.manga.ui.feature.main.config.screen.ConfigScreen
import br.acerola.manga.ui.feature.main.history.screen.HistoryScreen
import br.acerola.manga.ui.feature.main.home.screen.HomeScreen
import br.acerola.manga.ui.feature.main.home.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity(
    override val startDestinationRes: Int = Destination.HOME.route
) : BaseActivity() {
    private val filePreferencesViewModel: FilePreferencesViewModel by viewModels()
    private val folderAccessViewModel: FolderAccessViewModel by viewModels()
    private val mangaFolderViewModel: MangaFolderViewModel by viewModels()
    private val mangaDexViewModel: MangaMetadataViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()


    override fun NavGraphBuilder.setupNavGraph(context: Context, navController: NavHostController) {
        defaultComposable(context, Destination.HOME) {
            HomeScreen(
                mangaFolderViewModel, homeViewModel
            )
        }
        defaultComposable(context, Destination.HISTORY) {
            HistoryScreen()
        }
        defaultComposable(context, Destination.CONFIG) {
            ConfigScreen(
                filePreferencesViewModel,
                folderAccessViewModel,
                mangaFolderViewModel,
                mangaDexViewModel
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