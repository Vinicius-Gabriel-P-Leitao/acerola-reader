package br.acerola.comic

import android.content.Context
import android.os.Bundle
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
import br.acerola.comic.common.activity.BaseActivity
import br.acerola.comic.common.navigation.Destination
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.BottomBar
import br.acerola.comic.common.ux.component.SideBar
import br.acerola.comic.module.main.Main
import br.acerola.comic.module.main.config.Screen
import br.acerola.comic.module.main.history.Screen
import br.acerola.comic.module.main.home.Screen
import br.acerola.comic.module.main.pattern.FilePatternScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity(
    override val startDestinationRes: Int = Destination.HOME.route,
) : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0)
        }
    }

    override fun NavGraphBuilder.setupNavGraph(
        context: Context,
        navController: NavHostController,
    ) {
        defaultComposable(context, Destination.HOME) {
            Main.Home.Template.Screen(
                onNavigateToConfig = {
                    navController.navigate(context.getString(Destination.CONFIG.route))
                },
            )
        }
        defaultComposable(context, Destination.HISTORY) {
            Main.History.Template.Screen()
        }
        defaultComposable(context, Destination.CONFIG) {
            Main.Config.Template.Screen(
                onNavigateToTemplates = {
                    navController.navigate(context.getString(Destination.PATTERN.route))
                },
            )
        }
        defaultComposable(context, Destination.PATTERN) {
            Main.Pattern.Template.FilePatternScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }

    @Composable
    override fun TopBar(navController: NavHostController) {
    }

    @Composable
    override fun BottomBar(navController: NavHostController) {
        Acerola.Component.BottomBar(navController)
    }

    @Composable
    override fun SideBar(navController: NavHostController) {
        Acerola.Component.SideBar(navController)
    }

    private fun NavGraphBuilder.defaultComposable(
        context: Context,
        destination: Destination,
        content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit,
    ) {
        composable(
            route = context.getString(destination.route),
            enterTransition = {
                scaleIn(
                    initialScale = 0.8f,
                    animationSpec = tween(durationMillis = 300),
                ) + fadeIn(animationSpec = tween(durationMillis = 300))
            },
            exitTransition = {
                scaleOut(
                    targetScale = 0.8f,
                    animationSpec = tween(durationMillis = 300),
                ) + fadeOut(animationSpec = tween(durationMillis = 300))
            },
            popEnterTransition = {
                scaleIn(
                    initialScale = 1.2f,
                    animationSpec = tween(durationMillis = 300),
                ) + fadeIn(animationSpec = tween(durationMillis = 300))
            },
            popExitTransition = {
                scaleOut(
                    targetScale = 1.2f,
                    animationSpec = tween(durationMillis = 300),
                ) + fadeOut(animationSpec = tween(durationMillis = 300))
            },
            content = content,
        )
    }
}
