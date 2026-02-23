package br.acerola.manga

import android.content.Context
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import br.acerola.manga.common.activity.BaseActivity
import br.acerola.manga.common.layout.LocalSnackbarHostState
import br.acerola.manga.common.layout.NavigationBottomBar
import br.acerola.manga.common.navigation.Destination
import br.acerola.manga.common.viewmodel.archive.FilePreferencesViewModel
import br.acerola.manga.common.viewmodel.archive.FileSystemAccessViewModel
import br.acerola.manga.common.viewmodel.library.archive.MangaDirectoryViewModel
import br.acerola.manga.common.viewmodel.library.metadata.MangaRemoteInfoViewModel
import br.acerola.manga.module.config.ConfigScreen
import br.acerola.manga.module.history.HistoryScreen
import br.acerola.manga.module.home.HomeScreen
import br.acerola.manga.module.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity(
    override val startDestinationRes: Int = Destination.HOME.route
) : BaseActivity() {
    private val fileSystemAccessViewModel: FileSystemAccessViewModel by viewModels()
    private val filePreferencesViewModel: FilePreferencesViewModel by viewModels()
    private val mangaDirectoryViewModel: MangaDirectoryViewModel by viewModels()
    private val mangaDexViewModel: MangaRemoteInfoViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val metadataSettingsViewModel: br.acerola.manga.common.viewmodel.metadata.MetadataSettingsViewModel by viewModels()

    override fun NavGraphBuilder.setupNavGraph(context: Context, navController: NavHostController) {
        defaultComposable(context, Destination.HOME) {
            HomeScreen(
                homeViewModel
            )
        }
        defaultComposable(context, Destination.HISTORY) {
            HistoryScreen()
        }
        defaultComposable(context, Destination.CONFIG) {
            ConfigScreen(
                fileSystemAccessViewModel = fileSystemAccessViewModel,
                filePreferencesViewModel = filePreferencesViewModel,
                mangaDirectoryViewModel = mangaDirectoryViewModel,
                mangaDexViewModel = mangaDexViewModel,
                metadataSettingsViewModel = metadataSettingsViewModel
            )
        }
    }

    @Composable
    override fun TopBar(navController: NavHostController) {
    }

    @Composable
    override fun BottomBar(navController: NavHostController) {
        // TODO: Verificar por que o uiEvents tá sendo carregado aqui
        val snackbarHostState = LocalSnackbarHostState.current
        val context = LocalContext.current

        LaunchedEffect(key1 = Unit) {
            mangaDirectoryViewModel.uiEvents.collect { message ->
                snackbarHostState.showSnackbar(message.uiMessage.asString(context))
            }
        }

        LaunchedEffect(key1 = Unit) {
            mangaDexViewModel.uiEvents.collect { message ->
                snackbarHostState.showSnackbar(message.uiMessage.asString(context))
            }
        }

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
            },
            exitTransition = {
                scaleOut(
                    targetScale = 0.8f, animationSpec = tween(durationMillis = 300)
                ) + fadeOut(animationSpec = tween(durationMillis = 300))
            },
            popEnterTransition = {
                scaleIn(
                    initialScale = 1.2f, animationSpec = tween(durationMillis = 300)
                ) + fadeIn(animationSpec = tween(durationMillis = 300))
            },
            popExitTransition = {
                scaleOut(
                    targetScale = 1.2f, animationSpec = tween(durationMillis = 300)
                ) + fadeOut(animationSpec = tween(durationMillis = 300))
            },
            content = content
        )
    }
}